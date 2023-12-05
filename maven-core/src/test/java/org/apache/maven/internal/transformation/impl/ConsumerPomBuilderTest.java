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
package org.apache.maven.internal.transformation.impl;

import javax.inject.Inject;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.internal.transformation.AbstractRepositoryTestCase;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerPomBuilderTest extends AbstractRepositoryTestCase {

    @Inject
    ConsumerPomBuilder builder;

    @Test
    void testTrivialConsumer() throws Exception {
        MavenProject project;
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");
        try (InputStream inputStream = Files.newInputStream(file)) {
            org.apache.maven.model.Model model =
                    new org.apache.maven.model.Model(new MavenStaxReader().read(inputStream));
            project = new MavenProject(model);
            project.setRootDirectory(Paths.get("src/test/resources/consumer/trivial"));
            project.setOriginalModel(model);
            project.setRemoteArtifactRepositories(Collections.singletonList(new MavenArtifactRepository(
                    "central", "http://repo.maven.apache.org/", new DefaultRepositoryLayout(), null, null)));
        }
        Model model = builder.build(session, project, file);

        assertNotNull(model);
    }

    @Test
    void testSimpleConsumer() throws Exception {
        MavenProject project;
        Path file = Paths.get("src/test/resources/consumer/simple/simple-parent/simple-weather/pom.xml");
        ((DefaultRepositorySystemSession) session).setUserProperty("changelist", "MNG6957");
        try (InputStream inputStream = Files.newInputStream(file)) {
            org.apache.maven.model.Model model =
                    new org.apache.maven.model.Model(new MavenStaxReader().read(inputStream));
            project = new MavenProject(model);
            project.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
            project.setRemoteArtifactRepositories(Collections.singletonList(new MavenArtifactRepository(
                    "central", "http://repo.maven.apache.org/", new DefaultRepositoryLayout(), null, null)));
            project.setOriginalModel(model);
        }
        Model model = builder.build(session, project, file);

        assertNotNull(model);
        assertTrue(model.getProfiles().isEmpty());
    }
}
