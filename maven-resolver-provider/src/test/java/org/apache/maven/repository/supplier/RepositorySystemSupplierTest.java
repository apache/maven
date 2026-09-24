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
package org.apache.maven.repository.supplier;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.eclipse.aether.version.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RepositorySystemSupplierTest {
    @Test
    void smoke() throws Exception {
        try (RepositorySystem system = new RepositorySystemSupplier().get();
                CloseableSession session = new SessionBuilderSupplier(system, Maven3ScopeManagerConfiguration.INSTANCE)
                        .get()
                        .withLocalRepositoryBaseDirectories(new File("target/local-repo").toPath())
                        .build()) {
            Artifact artifact = new DefaultArtifact("org.apache.maven.resolver:maven-resolver-util:[0,)");
            VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(artifact);
            rangeRequest.setRepositories(Collections.singletonList(
                    new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                            .build()));
            VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);
            List<Version> versions = rangeResult.getVersions();

            // As of 2023-11-14, Maven Central has 36 versions of this artifact (and it will just grow)
            Assertions.assertTrue(versions.size() >= 36);
            System.out.println("Available " + versions.size() + " versions: " + versions);
        }
    }

    @Test
    void smokeV2Feature() throws Exception {
        try (RepositorySystem system = new RepositorySystemSupplier().get();
                CloseableSession session = new SessionBuilderSupplier(system, Maven3ScopeManagerConfiguration.INSTANCE)
                        .get()
                        .withLocalRepositoryBaseDirectories(new File("target/local-repo").toPath())
                        .build()) {
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setResolutionScope(session.getScopeManager()
                    .getResolutionScope(Maven3ScopeManagerConfiguration.RS_TEST_RUNTIME)
                    .orElseThrow(AssertionError::new));
            collectRequest.setRoot(
                    new Dependency(new DefaultArtifact("org.apache.maven:maven-resolver-provider:3.6.1"), ""));
            collectRequest.setRepositories(Collections.singletonList(
                    new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                            .build()));

            CollectResult collectResult = system.collectDependencies(session, collectRequest);

            collectResult.getRoot().accept(new DependencyGraphDumper(System.out::println));
        }
    }

    @Test
    void closed() {
        RepositorySystemSupplier systemSupplier = new RepositorySystemSupplier();
        systemSupplier.get().close(); // get an instance and immediately shut it down, this closes supplier as well
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::get);
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::getRepositorySystem);
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::getChecksumProcessor);
    }

    @Test
    void memorizing() {
        RepositorySystemSupplier systemSupplier = new RepositorySystemSupplier();
        PathProcessor pathProcessor = systemSupplier.getPathProcessor();
        Deployer deployer = systemSupplier.getDeployer();
        try (RepositorySystem repositorySystem = systemSupplier.get()) {
            Assertions.assertSame(systemSupplier.get(), repositorySystem);
            Assertions.assertSame(systemSupplier.getPathProcessor(), pathProcessor);
            Assertions.assertSame(systemSupplier.getDeployer(), deployer);
        }
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::get);
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::getRepositorySystem);
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::getDeployer);
        Assertions.assertThrows(IllegalStateException.class, systemSupplier::getPathProcessor);
    }
}
