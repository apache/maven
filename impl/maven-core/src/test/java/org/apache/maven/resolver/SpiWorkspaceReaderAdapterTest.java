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
package org.apache.maven.resolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.model.Model;
import org.apache.maven.impl.resolver.MavenWorkspaceReader;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiWorkspaceReaderAdapterTest {

    @Test
    void testImplementsMavenWorkspaceReader() {
        org.apache.maven.api.spi.WorkspaceReader spiReader = new org.apache.maven.api.spi.WorkspaceReader() {
            @Override
            public Optional<Path> findArtifact(Artifact artifact) {
                return Optional.empty();
            }

            @Override
            public List<String> findVersions(Artifact artifact) {
                return Collections.emptyList();
            }
        };

        SpiWorkspaceReaderAdapter adapter = new SpiWorkspaceReaderAdapter(spiReader);
        assertTrue(adapter instanceof MavenWorkspaceReader);
    }

    @Test
    void testDefaultFindModelReturnsEmpty() {
        org.apache.maven.api.spi.WorkspaceReader defaultReader = new org.apache.maven.api.spi.WorkspaceReader() {
            @Override
            public Optional<Path> findArtifact(Artifact artifact) {
                return Optional.empty();
            }

            @Override
            public List<String> findVersions(Artifact artifact) {
                return Collections.emptyList();
            }
        };

        SpiWorkspaceReaderAdapter adapter = new SpiWorkspaceReaderAdapter(defaultReader);
        org.eclipse.aether.artifact.Artifact resolverArtifact =
                new DefaultArtifact("org.apache.maven.test", "test-artifact", "jar", "1.0.0");

        assertNull(adapter.findModel(resolverArtifact));
    }

    @Test
    void testFindModelDelegatesAndReturnsSuppliedModel() {
        Model expectedModel = Model.newBuilder()
                .groupId("org.apache.maven.test")
                .artifactId("synthetic-artifact")
                .version("2.0.0")
                .packaging("jar")
                .build();

        org.apache.maven.api.spi.WorkspaceReader spiReader = new org.apache.maven.api.spi.WorkspaceReader() {
            @Override
            public Optional<Path> findArtifact(Artifact artifact) {
                return Optional.empty();
            }

            @Override
            public List<String> findVersions(Artifact artifact) {
                return List.of("2.0.0");
            }

            @Override
            public Optional<Model> findModel(Artifact artifact) {
                if ("org.apache.maven.test".equals(artifact.getGroupId())
                        && "synthetic-artifact".equals(artifact.getArtifactId())) {
                    return Optional.of(expectedModel);
                }
                return Optional.empty();
            }
        };

        SpiWorkspaceReaderAdapter adapter = new SpiWorkspaceReaderAdapter(spiReader);
        org.eclipse.aether.artifact.Artifact resolverArtifact =
                new DefaultArtifact("org.apache.maven.test", "synthetic-artifact", "pom", "2.0.0");

        Model actualModel = adapter.findModel(resolverArtifact);
        assertNotNull(actualModel);
        assertEquals("org.apache.maven.test", actualModel.getGroupId());
        assertEquals("synthetic-artifact", actualModel.getArtifactId());
        assertEquals("2.0.0", actualModel.getVersion());
    }

    @Test
    void testFindArtifactAndVersionsDelegation() {
        Path artifactPath = Paths.get("target/test.jar");
        org.apache.maven.api.spi.WorkspaceReader spiReader = new org.apache.maven.api.spi.WorkspaceReader() {
            @Override
            public Optional<Path> findArtifact(Artifact artifact) {
                return Optional.of(artifactPath);
            }

            @Override
            public List<String> findVersions(Artifact artifact) {
                return List.of("1.0.0", "1.1.0");
            }
        };

        SpiWorkspaceReaderAdapter adapter = new SpiWorkspaceReaderAdapter(spiReader);
        org.eclipse.aether.artifact.Artifact resolverArtifact =
                new DefaultArtifact("org.apache.maven.test", "test-artifact", "jar", "1.0.0");

        assertEquals(artifactPath.toFile(), adapter.findArtifact(resolverArtifact));
        assertEquals(List.of("1.0.0", "1.1.0"), adapter.findVersions(resolverArtifact));
    }
}
