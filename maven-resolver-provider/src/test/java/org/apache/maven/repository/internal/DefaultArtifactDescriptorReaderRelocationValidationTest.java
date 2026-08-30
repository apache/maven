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
package org.apache.maven.repository.internal;

import javax.inject.Inject;

import java.io.File;

import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a relocation read from a resolved artifact descriptor's model is validated before its
 * components are applied to the artifact being resolved, exercising the real
 * {@link ArtifactDescriptorReader#readArtifactDescriptor(RepositorySystemSession, ArtifactDescriptorRequest)} code
 * path against a fixture repository on disk. Each test method gets its own {@code @TempDir} local repository so
 * runs do not share resolution state with the rest of this module's tests.
 */
@PlexusTest
public class DefaultArtifactDescriptorReaderRelocationValidationTest {

    @Inject
    private RepositorySystem system;

    @Inject
    private ArtifactDescriptorReader reader;

    private RepositorySystemSession session;

    @BeforeEach
    void setUp(@TempDir File localRepoDir) {
        DefaultRepositorySystemSession newSession = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoDir);
        newSession.setLocalRepositoryManager(system.newLocalRepositoryManager(newSession, localRepo));
        session = newSession;
    }

    private static RemoteRepository testRepository() throws Exception {
        return new RemoteRepository.Builder(
                        "repo",
                        "default",
                        getTestFile("target/test-classes/repo").toURI().toURL().toString())
                .build();
    }

    @Test
    void testRelocationWithInvalidArtifactIdIsRejected() throws Exception {
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.addRepository(testRepository());
        request.setArtifact(new DefaultArtifact("ut.simple", "dep-invalid-relocation", "pom", "1.0"));

        ArtifactDescriptorException exception = assertThrows(
                ArtifactDescriptorException.class, () -> reader.readArtifactDescriptor(session, request));
        assertTrue(exception.getMessage().contains("artifactId"));
    }
}
