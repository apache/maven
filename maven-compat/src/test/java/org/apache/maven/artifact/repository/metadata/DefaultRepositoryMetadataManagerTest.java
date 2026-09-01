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
package org.apache.maven.artifact.repository.metadata;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link DefaultRepositoryMetadataManager} rejects repository metadata carrying version tokens that
 * are not valid coordinate components, on the legacy read path used when metadata is loaded for merging.
 */
public class DefaultRepositoryMetadataManagerTest {

    private final DefaultRepositoryMetadataManager manager = new DefaultRepositoryMetadataManager();

    @Test
    void testMetadataWithInvalidVersionTokenIsRejected() {
        File metadataFile = testFile("metadata-invalid-token/maven-metadata.xml");

        RepositoryMetadataReadException exception =
                assertThrows(RepositoryMetadataReadException.class, () -> manager.readMetadata(metadataFile));

        assertTrue(exception.getMessage().contains("invalid version token"), exception.getMessage());
    }

    @Test
    void testMetadataWithInvalidSnapshotTimestampIsRejected() {
        File metadataFile = testFile("metadata-invalid-timestamp/maven-metadata.xml");

        RepositoryMetadataReadException exception =
                assertThrows(RepositoryMetadataReadException.class, () -> manager.readMetadata(metadataFile));

        assertTrue(exception.getMessage().contains("invalid version token"), exception.getMessage());
    }

    private static File testFile(String resource) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        assertNotNull(url, "test resource not found: " + resource);
        return new File(url.getFile());
    }
}
