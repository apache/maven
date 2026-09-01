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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ensures that a repository key which may originate from a downloaded POM's {@code <repositories>} section
 * cannot select a local metadata file name outside the intended one.
 */
class AbstractRepositoryMetadataTest {

    private static ArtifactRepository repository(String id) {
        ArtifactRepository repo = mock(ArtifactRepository.class);
        when(repo.getKey()).thenReturn(id);
        return repo;
    }

    private static RepositoryMetadata createMetadata() {
        Artifact artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn("org.test");
        when(artifact.getArtifactId()).thenReturn("test-artifact");
        when(artifact.getVersion()).thenReturn("1.0");
        return new ArtifactRepositoryMetadata(artifact);
    }

    @Test
    void repositoryKeyWithColonIsRejected() {
        RepositoryMetadata metadata = createMetadata();
        ArtifactRepository repo = repository("central:1.0");

        assertThrows(IllegalArgumentException.class, () -> metadata.getLocalFilename(repo));
    }

    @Test
    void repositoryKeyWithDotDotSegmentIsRejected() {
        RepositoryMetadata metadata = createMetadata();
        ArtifactRepository repo = repository("x/../../../../../../settings");

        assertThrows(IllegalArgumentException.class, () -> metadata.getLocalFilename(repo));
    }

    @Test
    void repositoryKeyWithBackslashIsRejected() {
        RepositoryMetadata metadata = createMetadata();
        ArtifactRepository repo = repository("x\\..\\..\\settings");

        assertThrows(IllegalArgumentException.class, () -> metadata.getLocalFilename(repo));
    }

    @Test
    void wellFormedRepositoryKeyProducesExpectedFilename() {
        RepositoryMetadata metadata = createMetadata();
        ArtifactRepository repo = repository("central");

        assertEquals("maven-metadata-central.xml", metadata.getLocalFilename(repo));
    }
}
