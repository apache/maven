/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.cli.props;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.utils.StringInterpolator;
import org.apache.maven.utils.StringInterpolatorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class MavenPropertiesCompatibilityTest {
    @TempDir
    Path directory;

    @Test
    void preservesConstructorsAndSavedLayout() throws Exception {
        Path file = directory.resolve("settings.properties");
        String text = "# Configuration\nname=Sahil\nmessage=Hello ${name}\n";
        Files.writeString(file, text);

        MavenProperties properties = new MavenProperties(file);
        assertEquals("Hello Sahil", properties.getProperty("message"));
        properties.save();
        assertEquals(text, Files.readString(file));
        assertEquals("Hello Sahil", new MavenProperties(file, key -> null).getProperty("message"));

        MavenProperties unresolved = new MavenProperties(file, false);
        unresolved.load(file);
        assertEquals("Hello ${name}", unresolved.getProperty("message"));

        MavenProperties callback = new MavenProperties(file, key -> "external");
        assertEquals("Sahil", callback.getProperty("name"));
    }

    @Test
    void preservesNestedReaderWriterAndLayoutTypes() throws Exception {
        StringWriter output = new StringWriter();
        try (MavenProperties.PropertiesWriter writer = new MavenProperties.PropertiesWriter(output, false)) {
            writer.writeProperty("name", "Sahil");
        }
        assertEquals("name = Sahil" + System.lineSeparator(), output.toString());

        try (MavenProperties.PropertiesReader reader =
                new MavenProperties.PropertiesReader(new StringReader(output.toString()), false)) {
            assertTrue(reader.nextProperty());
            assertEquals("name", reader.getPropertyName());
            assertEquals("Sahil", reader.getPropertyValue());
        }

        MavenProperties.Layout layout = new MavenProperties.Layout(List.of("# comment"), List.of("name=Sahil"));
        assertEquals(List.of("# comment"), layout.getCommentLines());
        assertEquals(List.of("name=Sahil"), layout.getValueLines());
    }

    @Test
    void preservesTheLegacyTypedUpdateOverride() {
        class TrackingProperties extends MavenProperties {
            boolean called;

            @Override
            public boolean update(MavenProperties properties) {
                called = true;
                return super.update(properties);
            }
        }

        TrackingProperties properties = new TrackingProperties();
        Map<String, String> values = Map.of("name", "Sahil");
        assertTrue(properties.update(values));
        assertTrue(properties.called);
        assertEquals("Sahil", properties.getProperty("name"));
    }

    @Test
    void preservesMavenExceptionsForRecursiveReferences() throws Exception {
        MavenProperties properties = new MavenProperties(false);
        properties.put("name", "${name}");
        assertRecursiveFailure(properties::substitute);
        assertRecursiveFailure(() -> MavenPropertiesLoader.substitute(properties, null));
        assertRecursiveFailure(() -> MavenPropertiesLoader.substVars("${name}", "name", null, null));

        Path file = directory.resolve("recursive.properties");
        Files.writeString(file, "name=${name}\n");
        assertRecursiveFailure(() -> new MavenProperties(file));
        assertRecursiveFailure(
                () -> MavenPropertiesLoader.loadProperties(new java.util.Properties(), file, null, false));
    }

    private void assertRecursiveFailure(Executable action) {
        InterpolatorException error = assertThrows(InterpolatorException.class, action);
        StringInterpolatorException cause = assertInstanceOf(StringInterpolatorException.class, error.getCause());
        assertEquals("recursive variable reference: name", error.getMessage());
        assertEquals(error.getMessage(), cause.getMessage());
        assertTrue(java.util.Arrays.stream(cause.getStackTrace())
                .anyMatch(frame -> StringInterpolator.class.getName().equals(frame.getClassName())));
    }

    @Test
    void retainsTheOriginalUtilityExceptionAndItsCause() throws Exception {
        StringInterpolatorException failure = new StringInterpolatorException("utility callback failed");
        failure.initCause(new IllegalStateException("original failure"));
        java.util.function.UnaryOperator<String> callback = key -> {
            throw failure;
        };
        MavenProperties properties = new MavenProperties(false);
        properties.put("message", "${external}");
        assertSame(
                failure,
                assertThrows(InterpolatorException.class, () -> properties.substitute(callback))
                        .getCause());
        assertSame(
                failure,
                assertThrows(InterpolatorException.class, () -> MavenPropertiesLoader.substitute(properties, callback))
                        .getCause());
        assertSame(
                failure,
                assertThrows(
                                InterpolatorException.class,
                                () -> MavenPropertiesLoader.substVars("${external}", "message", null, callback))
                        .getCause());
        Path file = directory.resolve("callback.properties");
        Files.writeString(file, "message=${external}\n");
        assertSame(
                failure,
                assertThrows(InterpolatorException.class, () -> new MavenProperties(file, callback))
                        .getCause());
        assertSame(
                failure,
                assertThrows(
                                InterpolatorException.class,
                                () -> MavenPropertiesLoader.loadProperties(
                                        new java.util.Properties(), file, callback, false))
                        .getCause());
    }

    @Test
    void preservesExceptionsRaisedByCallbacks() {
        MavenProperties properties = new MavenProperties(false);
        properties.put("message", "${external}");
        IllegalArgumentException failure = new IllegalArgumentException("callback failed");
        assertSame(
                failure,
                assertThrows(
                        IllegalArgumentException.class,
                        () -> properties.substitute(key -> {
                            throw failure;
                        })));
    }
}
