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
package org.apache.maven.model;

import java.io.StringWriter;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code Model}.
 *
 */
class ModelTest {

    @Test
    void testHashCodeNullSafe() {
        new Model().hashCode();
    }

    @Test
    void testBuild() {
        Model model = new Model();
        Build build = new Build();
        build.setOutputDirectory("myOutputDirectory");
        model.setBuild(build);
        Build build2 = model.getBuild();
        assertNotNull(build2);
        assertEquals("myOutputDirectory", build2.getOutputDirectory());
        model.setBuild(null);
        assertNull(model.getBuild());
    }

    @Test
    void testEqualsNullSafe() {
        assertNotEquals(null, new Model());

        new Model().equals(new Model());
    }

    @Test
    void testEqualsIdentity() {
        Model thing = new Model();
        assertEquals(thing, thing);
    }

    @Test
    void testToStringNullSafe() {
        assertNotNull(new Model().toString());
    }

    @Test
    void testWritePreservesModelVersionNamespace() throws Exception {
        Model model = new Model();
        model.setModelVersion("4.1.0");
        model.setGroupId("g");
        model.setArtifactId("a");
        model.setVersion("1");

        StringWriter output = new StringWriter();
        new MavenXpp3Writer().write(output, model);

        String xml = output.toString();
        assertTrue(xml.contains("xmlns=\"http://maven.apache.org/POM/4.1.0\""));
        assertTrue(
                xml.contains(
                        "xsi:schemaLocation=\"http://maven.apache.org/POM/4.1.0 https://maven.apache.org/xsd/maven-4.1.0.xsd\""));
    }

    @Test
    void testPropertiesClear() {
        // Test for issue #11552: NullPointerException when clearing properties
        Model model = new Model();
        model.addProperty("key1", "value1");
        model.addProperty("key2", "value2");
        assertEquals(2, model.getProperties().size());

        // This should not throw NullPointerException
        model.getProperties().clear();
        assertEquals(0, model.getProperties().size());
    }
}
