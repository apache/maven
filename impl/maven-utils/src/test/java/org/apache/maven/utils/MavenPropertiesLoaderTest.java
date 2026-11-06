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
package org.apache.maven.utils;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenPropertiesLoaderTest {
    @TempDir
    Path directory;

    @Test
    void loadsNestedAndQuotedIncludesRelativeToTheIncludingFile() throws Exception {
        Path subdirectory = Files.createDirectory(directory.resolve("subdirectory"));
        Files.writeString(directory.resolve("main.properties"), "${includes}=\"subdirectory/extra file.properties\"\n");
        Files.writeString(
                subdirectory.resolve("extra file.properties"), "${includes}=nested.properties\nurl=${host}/api\n");
        Files.writeString(subdirectory.resolve("nested.properties"), "host=example.org\n");

        Properties properties = new Properties();
        MavenPropertiesLoader.loadProperties(properties, directory.resolve("main.properties"), null, false);

        assertEquals("example.org/api", properties.getProperty("url"));
        assertFalse(properties.containsKey(MavenPropertiesLoader.INCLUDES_PROPERTY));
    }

    @Test
    void distinguishesOptionalAndMandatoryIncludes() throws Exception {
        Path file = directory.resolve("main.properties");
        Files.writeString(file, "${includes}=?missing.properties\nname=value\n");
        Properties properties = new Properties();
        MavenPropertiesLoader.loadProperties(properties, file, null, false);
        assertEquals("value", properties.getProperty("name"));

        Files.writeString(file, "${includes}=missing.properties\n");
        assertThrows(
                NoSuchFileException.class,
                () -> MavenPropertiesLoader.loadProperties(new Properties(), file, null, false));
    }

    @Test
    void preservesExistingPropertiesAndAppliesExplicitOverrides() throws Exception {
        Path file = directory.resolve("main.properties");
        Files.writeString(file, "name=from-file\nmaven.override.port=9090\n");
        Properties properties = new Properties();
        properties.setProperty("name", "existing");
        properties.setProperty("port", "8080");

        MavenPropertiesLoader.loadProperties(properties, file, properties::getProperty, false);

        assertEquals("existing", properties.getProperty("name"));
        assertEquals("9090", properties.getProperty("port"));
        assertFalse(properties.containsKey("maven.override.port"));
    }

    @Test
    void resolvesCallbacksAndPreservesUnresolvedValues() throws Exception {
        Path file = directory.resolve("main.properties");
        Files.writeString(file, "message=Hello ${name}\nunknown=${missing}\n");
        Properties properties = new Properties();

        MavenPropertiesLoader.loadProperties(properties, file, Map.of("name", "Sahil")::get, false);

        assertEquals("Hello Sahil", properties.getProperty("message"));
        assertEquals("${missing}", properties.getProperty("unknown"));
    }

    @Test
    void escapesExistingValuesWhenRequested() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("literal", "${name}");
        properties.setProperty("name", "expanded");

        MavenPropertiesLoader.loadProperties(properties, directory.resolve("missing.properties"), null, true);

        assertEquals("${name}", properties.getProperty("literal"));
    }

    @Test
    void rejectsRecursiveReferences() throws Exception {
        Path file = directory.resolve("main.properties");
        Files.writeString(file, "first=${second}\nsecond=${first}\n");
        assertThrows(
                StringInterpolatorException.class,
                () -> MavenPropertiesLoader.loadProperties(new Properties(), file, null, false));
    }
}
