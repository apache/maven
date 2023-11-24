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
package org.apache.maven.internal.transformation;

import javax.inject.Inject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerPomBuilderTest extends AbstractRepositoryTestCase {

    @Inject
    ConsumerPomBuilder builder;

    @Test
    void testTrivialConsumer() {
        MavenProject project = new MavenProject();
        project.setRootDirectory(Paths.get("src/test/resources/consumer/trivial"));
        project.setRemoteArtifactRepositories(Collections.singletonList(new MavenArtifactRepository(
                "central", "http://repo.maven.apache.org/", new DefaultRepositoryLayout(), null, null)));
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");
        Model model = builder.build(session, project, file);

        assertNotNull(model);
    }

    @Test
    void testSimpleConsumer() {
        MavenProject project = new MavenProject();
        project.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
        project.setRemoteArtifactRepositories(Collections.singletonList(new MavenArtifactRepository(
                "central", "http://repo.maven.apache.org/", new DefaultRepositoryLayout(), null, null)));
        Path file = Paths.get("src/test/resources/consumer/simple/simple-parent/simple-weather/pom.xml");
        ((DefaultRepositorySystemSession) session).setUserProperty("changelist", "MNG6957");
        Model model = builder.build(session, project, file);

        assertNotNull(model);
        assertTrue(model.getProfiles().isEmpty());
    }
}
