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
import java.util.Collections;
import java.util.List;

import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PlexusTest
public class ModelBuilderTest {

    @Inject
    ProjectBuilder projectBuilder;

    @Inject
    MavenRepositorySystem repositorySystem;

    @Inject
    RepositorySystemSessionFactory repositorySessionFactory;

    @Test
    void testModelBuilder() throws Exception {
        MavenExecutionRequest mavenRequest = new DefaultMavenExecutionRequest();
        mavenRequest.setLocalRepository(repositorySystem.createLocalRepository(new File("target/test-repo/")));

        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        request.setRepositorySession(repositorySessionFactory
                .newRepositorySessionBuilder(mavenRequest)
                .build());
        List<ProjectBuildingResult> results = projectBuilder.build(
                Collections.singletonList(new File("src/test/resources/projects/tree/pom.xml")), true, request);

        assertEquals(3, results.size());
    }
}
