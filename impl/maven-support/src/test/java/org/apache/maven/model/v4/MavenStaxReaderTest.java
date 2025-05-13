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

import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenStaxReaderTest {

    @Test
    void namespaceReporting() throws Exception {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    @Test
    void emptyNamespaceReporting() throws Exception {
        String xml = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n" + "</project>";

        Model model = fromXml(xml);
        assertEquals("", model.getNamespaceUri());
    }

    @Test
    void namespaceConsistency() throws XMLStreamException {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    @Test
    void namespaceInconsistencyThrows() {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build xmlns=\"http://maven.apache.org/POM/4.1.0\">\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        XMLStreamException ex = assertThrows(XMLStreamException.class, () -> fromXml(xml));
        assertTrue(ex.getMessage().contains("Unexpected namespace for element 'build'"));
        assertTrue(ex.getMessage().contains("found 'http://maven.apache.org/POM/4.1.0'"));
        assertTrue(ex.getMessage().contains("expected 'http://maven.apache.org/POM/4.0.0'"));
    }

    @Test
    void emptyNamespaceConsistency() throws XMLStreamException {
        String xml = "<project>\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertEquals("", model.getNamespaceUri());
    }

    @Test
    void emptyNamespaceInconsistencyThrows() {
        String xml = "<project>\n"
                + "  <build xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        XMLStreamException ex = assertThrows(XMLStreamException.class, () -> fromXml(xml));
        assertTrue(ex.getMessage().contains("Unexpected namespace for element 'build'"));
        assertTrue(ex.getMessage().contains("found 'http://maven.apache.org/POM/4.0.0'"));
        assertTrue(ex.getMessage().contains("expected ''"));
    }

    @Test
    void pluginConfigurationAllowsOtherNamespaces() throws XMLStreamException {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "        <configuration>\n"
                + "          <customConfig xmlns:custom=\"http://custom.namespace.org\">\n"
                + "            <custom:element>value</custom:element>\n"
                + "          </customConfig>\n"
                + "        </configuration>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertNotNull(model);
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    private Model fromXml(String xml) throws XMLStreamException {
        MavenStaxReader reader = new MavenStaxReader();
        return reader.read(new StringReader(xml), true, null);
    }
}
