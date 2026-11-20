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
package org.apache.maven.impl.standalone;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.maven.di.Injector;
import org.apache.maven.di.impl.Types;
import org.apache.maven.impl.ExtensibleEnumRegistries;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiTest {

    @Test
    @SuppressWarnings("deprecation")
    void discoversOriginIndependentLocalRepositories(@TempDir final Path directory) throws Exception {
        final var injector = Injector.create();
        injector.bindImplicit(RepositorySystemSupplier.class);
        injector.discover(ApiRunner.class.getClassLoader());
        final var system = injector.getInstance(RepositorySystem.class);
        try {
            final var session = new DefaultRepositorySystemSession();
            final var manager = system.newLocalRepositoryManager(session, new LocalRepository(directory));
            final var artifact = new DefaultArtifact("test:cached:jar:1");
            final var origin = new RemoteRepository.Builder("old", "default", "https://old.invalid").build();
            final var path = manager.getAbsolutePathForRemoteArtifact(artifact, origin, "");
            Files.createDirectories(path.getParent());
            Files.writeString(path, "cached");
            manager.add(session, new LocalArtifactRegistration(artifact, origin, List.of("")));
            assertTrue(manager.find(session, new LocalArtifactRequest(artifact, List.of(), ""))
                    .isAvailable());
            session.setLocalRepositoryManager(manager);
            session.setOffline(true);
            assertEquals(
                    path,
                    system.resolveArtifact(session, new ArtifactRequest(artifact, List.of(), ""))
                            .getArtifact()
                            .getPath());
            final var workspacePath = Files.writeString(directory.resolve("workspace.jar"), "workspace");
            session.setWorkspaceReader(new WorkspaceReader() {
                @Override
                public WorkspaceRepository getRepository() {
                    return new WorkspaceRepository("reactor");
                }

                @Override
                public File findArtifact(final Artifact requested) {
                    return workspacePath.toFile();
                }

                @Override
                public List<String> findVersions(final Artifact requested) {
                    return List.of("1");
                }
            });
            assertEquals(
                    workspacePath,
                    system.resolveArtifact(session, new ArtifactRequest(artifact, List.of(), ""))
                            .getArtifact()
                            .getPath(),
                    "Workspace artifacts must win over an available cached release");
        } finally {
            system.shutdown();
        }
    }

    @Test
    void testGenerics() {
        Set<Type> types = Types.getAllSuperTypes(ExtensibleEnumRegistries.DefaultPathScopeRegistry.class);
        assertNotNull(types);

        Injector injector = Injector.create();
        injector.bindImplicit(ExtensibleEnumRegistries.DefaultPathScopeRegistry.class);
    }
}
