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
package org.apache.maven.model.v4;

import javax.xml.stream.XMLStreamException;

import java.io.StringReader;
import java.util.Locale;

import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenStaxReaderNamespaceTest {

    private static final InputSource NO_INPUT_SOURCE = null;

    private static final String POM_41 = ""
            + "<project xmlns=\"http://maven.apache.org/POM/4.1.0\" "
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "  <modelVersion>4.1.0</modelVersion>"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "  <version>1.0</version>"
            + "</project>";

    private static final String POM_40 = ""
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "  <modelVersion>4.0.0</modelVersion>"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "  <version>1.0</version>"
            + "</project>";

    private static final String POM_NO_NS = ""
            + "<project>"
            + "  <modelVersion>4.0.0</modelVersion>"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "  <version>1.0</version>"
            + "</project>";

    private static final String POM_BAD_NS = ""
            + "<project xmlns=\"http://example.com/not/pom\">"
            + "  <modelVersion>4.1.0</modelVersion>"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "  <version>1.0</version>"
            + "</project>";

    @Test
    void acceptsPom41() throws Exception {
        MavenStaxReader reader = new MavenStaxReader();
        Model model = reader.read(new StringReader(POM_41), /*strict*/ true, NO_INPUT_SOURCE);
        assertNotNull(model);
        assertEquals("com.acme", model.getGroupId());
        assertEquals("demo", model.getArtifactId());
        assertEquals("1.0", model.getVersion());
    }

    @Test
    void acceptsPom40() throws Exception {
        MavenStaxReader reader = new MavenStaxReader();
        Model model = reader.read(new StringReader(POM_40), true, NO_INPUT_SOURCE);
        assertNotNull(model);
        assertEquals("com.acme", model.getGroupId());
        assertEquals("demo", model.getArtifactId());
        assertEquals("1.0", model.getVersion());
    }

    @Test
    void acceptsPomWithoutNamespace() throws Exception {
        MavenStaxReader reader = new MavenStaxReader();
        Model model = reader.read(new StringReader(POM_NO_NS), true, NO_INPUT_SOURCE);
        assertNotNull(model);
        assertEquals("com.acme", model.getGroupId());
        assertEquals("demo", model.getArtifactId());
        assertEquals("1.0", model.getVersion());
    }

    @Test
    void rejectsUnexpectedPomNamespace() {
        MavenStaxReader reader = new MavenStaxReader();
        XMLStreamException ex = assertThrows(
                XMLStreamException.class, () -> reader.read(new StringReader(POM_BAD_NS), true, NO_INPUT_SOURCE));
        // sanity check: message mentions unrecognized namespace
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("unrecognized pom namespace"));
    }
}
