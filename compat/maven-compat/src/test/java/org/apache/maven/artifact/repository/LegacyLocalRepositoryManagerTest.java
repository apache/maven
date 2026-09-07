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
package org.apache.maven.artifact.repository;

import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyLocalRepositoryManagerTest {

    private static LegacyLocalRepositoryManager.ArtifactMetadataAdapter newAdapter() {
        Metadata metadata =
                new DefaultMetadata("g", "a", "1.0", "maven-metadata.xml", Metadata.Nature.RELEASE_OR_SNAPSHOT);
        return new LegacyLocalRepositoryManager.ArtifactMetadataAdapter(metadata);
    }

    private static ArtifactRepository repositoryWithId(String id) {
        return new DefaultArtifactRepository(id, "http://example.invalid/repo", new DefaultRepositoryLayout());
    }

    @Test
    void getLocalFilenameKeepsWellFormedRepositoryKeyUnchanged() {
        String filename = newAdapter().getLocalFilename(repositoryWithId("central"));

        assertEquals("maven-metadata-central.xml", filename);
    }

    @Test
    void getLocalFilenameRejectsRepositoryKeyContainingPathSeparator() {
        assertThrows(
                IllegalArgumentException.class, () -> newAdapter().getLocalFilename(repositoryWithId("repo/evil")));
    }

    @Test
    void getLocalFilenameRejectsRepositoryKeyThatIsAParentDirectoryReference() {
        assertThrows(IllegalArgumentException.class, () -> newAdapter().getLocalFilename(repositoryWithId("..")));
    }
}
