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

import org.apache.maven.api.metadata.Metadata;
import org.apache.maven.metadata.v4.MetadataStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataStaxReaderNamespaceTest {

    private static final String META_110 = ""
            + "<metadata xmlns=\"http://maven.apache.org/METADATA/1.1.0\">"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "  <versioning>"
            + "    <latest>1.0</latest>"
            + "    <release>1.0</release>"
            + "  </versioning>"
            + "</metadata>";

    private static final String META_NO_NS = ""
            + "<metadata>"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "  <versioning>"
            + "    <latest>1.0</latest>"
            + "  </versioning>"
            + "</metadata>";

    private static final String META_BAD_NS = ""
            + "<metadata xmlns=\"http://example.com/not/metadata\">"
            + "  <groupId>com.acme</groupId>"
            + "  <artifactId>demo</artifactId>"
            + "</metadata>";

    @Test
    void acceptsMetadata110() throws Exception {
        MetadataStaxReader reader = new MetadataStaxReader();
        Metadata metadata = reader.read(new StringReader(META_110), /*strict*/ true);
        assertNotNull(metadata);
        assertEquals("com.acme", metadata.getGroupId());
        assertEquals("demo", metadata.getArtifactId());
        assertNotNull(metadata.getVersioning());
        assertEquals("1.0", metadata.getVersioning().getLatest());
    }

    @Test
    void acceptsMetadataWithoutNamespace() throws Exception {
        MetadataStaxReader reader = new MetadataStaxReader();
        Metadata metadata = reader.read(new StringReader(META_NO_NS), true);
        assertNotNull(metadata);
        assertEquals("com.acme", metadata.getGroupId());
        assertEquals("demo", metadata.getArtifactId());
    }

    @Test
    void rejectsUnexpectedMetadataNamespace() {
        MetadataStaxReader reader = new MetadataStaxReader();
        XMLStreamException ex =
                assertThrows(XMLStreamException.class, () -> reader.read(new StringReader(META_BAD_NS), true));
        assertTrue(ex.getMessage().toLowerCase().contains("unrecognized metadata namespace"));
    }
}
