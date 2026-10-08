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
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyLocalRepositoryManagerTest {

    private static LegacyLocalRepositoryManager.ArtifactMetadataAdapter newAdapter() {
        Metadata metadata =
                new DefaultMetadata("g", "a", "1.0", "maven-metadata.xml", Metadata.Nature.RELEASE_OR_SNAPSHOT);
        return new LegacyLocalRepositoryManager.ArtifactMetadataAdapter(metadata);
    }

    private static ArtifactRepository repositoryWithId(String id) {
        ArtifactRepository repo = mock(ArtifactRepository.class);
        when(repo.getKey()).thenReturn(id);
        return repo;
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

    @Test
    void overlayUsesLegacyManagerForNonLocalDefaultLayoutRepository() {
        ArtifactRepository repository = mock(ArtifactRepository.class);
        when(repository.getBasedir()).thenReturn("target/central-staging");
        when(repository.getId()).thenReturn("central-staging");
        when(repository.getLayout()).thenReturn(new DefaultRepositoryLayout());

        RepositorySystem system = mock(RepositorySystem.class);
        LocalRepositoryManager resolverManager = mock(LocalRepositoryManager.class);
        when(system.newLocalRepositoryManager(any(RepositorySystemSession.class), any(LocalRepository.class)))
                .thenReturn(resolverManager);
        RepositorySystemSession session = new DefaultRepositorySystemSession();

        RepositorySystemSession overlaid = LegacyLocalRepositoryManager.overlay(repository, session, system);

        assertInstanceOf(LegacyLocalRepositoryManager.class, overlaid.getLocalRepositoryManager());
        verify(system, never())
                .newLocalRepositoryManager(any(RepositorySystemSession.class), any(LocalRepository.class));
    }

    @Test
    void overlayUsesResolverManagerForLocalDefaultLayoutRepository() {
        ArtifactRepository repository = mock(ArtifactRepository.class);
        when(repository.getBasedir()).thenReturn("target/local-repository");
        when(repository.getId()).thenReturn("local");
        when(repository.getLayout()).thenReturn(new DefaultRepositoryLayout());

        RepositorySystem system = mock(RepositorySystem.class);
        LocalRepositoryManager resolverManager = mock(LocalRepositoryManager.class);
        when(system.newLocalRepositoryManager(any(RepositorySystemSession.class), any(LocalRepository.class)))
                .thenReturn(resolverManager);
        RepositorySystemSession session = new DefaultRepositorySystemSession();

        RepositorySystemSession overlaid = LegacyLocalRepositoryManager.overlay(repository, session, system);

        assertSame(resolverManager, overlaid.getLocalRepositoryManager());
        verify(system).newLocalRepositoryManager(any(RepositorySystemSession.class), any(LocalRepository.class));
    }
}
