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
package org.apache.maven.model;

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.SimpleLookup;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.DefaultRepositoryFactory;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.cache.DefaultRequestCacheFactory;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryKeyFunctionFactory;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PlexusTest
public class ModelBuilderTest {

    @Inject
    ProjectBuilder projectBuilder;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    RepositorySystemSessionFactory repositorySessionFactory;

    @Inject
    RepositorySystem repositorySystem;

    @Test
    void testModelBuilder() throws Exception {
        MavenExecutionRequest mavenRequest = new DefaultMavenExecutionRequest();
        mavenRequest.setLocalRepository(mavenRepositorySystem.createLocalRepository(new File("target/test-repo/")));
        mavenRequest.setRootDirectory(Paths.get("src/test/resources/projects/tree"));

        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        RepositorySystemSession.CloseableSession rsession = repositorySessionFactory
                .newRepositorySessionBuilder(mavenRequest)
                .build();
        request.setRepositorySession(rsession);
        MavenSession msession = new MavenSession(rsession, mavenRequest, new DefaultMavenExecutionResult());
        InternalSession session = new DefaultSession(
                msession,
                repositorySystem,
                null,
                mavenRepositorySystem,
                new SimpleLookup(List.of(
                        new DefaultRequestCacheFactory(),
                        new DefaultRepositoryFactory(new DefaultRemoteRepositoryManager(
                                new DefaultUpdatePolicyAnalyzer(),
                                new DefaultChecksumPolicyProvider(),
                                new DefaultRepositoryKeyFunctionFactory())))),
                null);
        InternalSession.associate(rsession, session);

        List<ProjectBuildingResult> results = projectBuilder.build(
                Collections.singletonList(new File("src/test/resources/projects/tree/pom.xml")), true, request);

        assertEquals(3, results.size());
    }
}
