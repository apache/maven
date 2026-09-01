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
package org.apache.maven.artifact.repository.metadata.io;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultMetadataReaderTest {

    private final DefaultMetadataReader reader = new DefaultMetadataReader();

    private File resource(String name) throws URISyntaxException {
        return new File(getClass().getResource(name).toURI());
    }

    @Test
    public void testWellFormedMetadataParsesUnchanged() throws Exception {
        Metadata metadata = reader.read(resource("well-formed-metadata.xml"), Collections.emptyMap());

        assertEquals("org.apache.maven.its", metadata.getGroupId());
        assertEquals("sample", metadata.getArtifactId());
        assertEquals("1.1", metadata.getVersioning().getRelease());
        assertEquals("1.1", metadata.getVersioning().getLatest());
        assertEquals("maven-sample-plugin", metadata.getPlugins().get(0).getArtifactId());
    }

    @Test
    public void testVersionContainingColonIsRejected() throws Exception {
        File input = resource("invalid-version-token.xml");

        IOException e = assertThrows(IOException.class, () -> reader.read(input, Collections.emptyMap()));
        assertTrue(e.getMessage().contains("1.0:evil"));
    }

    @Test
    public void testPluginArtifactIdContainingSlashIsRejected() throws Exception {
        File input = resource("invalid-plugin-artifactid.xml");

        IOException e = assertThrows(IOException.class, () -> reader.read(input, Collections.emptyMap()));
        assertTrue(e.getMessage().contains("maven/sample-plugin"));
    }
}
