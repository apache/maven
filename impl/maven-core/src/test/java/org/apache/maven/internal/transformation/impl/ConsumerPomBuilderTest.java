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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.di.Injector;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.transformation.AbstractRepositoryTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerPomBuilderTest extends AbstractRepositoryTestCase {

    @Inject
    ConsumerPomBuilder builder;

    @Inject
    ModelBuilder modelBuilder;

    @BeforeEach
    void setupTransformerContext() throws Exception {
        // We need bind the model resolver explicitly to avoid going to maven central
        getContainer().lookup(Injector.class).bindImplicit(MyModelResolver.class);
        InternalSession iSession = InternalSession.from(session);
        // set up the model resolver
        iSession.getData().set(SessionData.key(ModelResolver.class), new MyModelResolver());
    }

    @Test
    void testTrivialConsumer() throws Exception {
        InternalMavenSession.from(InternalSession.from(session))
                .getMavenSession()
                .getRequest()
                .setRootDirectory(Paths.get("src/test/resources/consumer/trivial"));

        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");

        ModelBuilder.ModelBuilderSession mbs = modelBuilder.newSession();
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), mbs);
        Model orgModel = mbs.build(ModelBuilderRequest.builder()
                        .session(InternalSession.from(session))
                        .source(ModelSource.fromPath(file))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .build())
                .getEffectiveModel();

        MavenProject project = new MavenProject(orgModel);
        project.setOriginalModel(new org.apache.maven.model.Model(orgModel));
        Model model = builder.build(session, project, file);

        assertNotNull(model);
    }

    @Test
    void testSimpleConsumer() throws Exception {
        MavenExecutionRequest request = InternalMavenSession.from(InternalSession.from(session))
                .getMavenSession()
                .getRequest();
        request.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
        request.getUserProperties().setProperty("changelist", "MNG6957");

        Path file = Paths.get("src/test/resources/consumer/simple/simple-parent/simple-weather/pom.xml");

        ModelBuilder.ModelBuilderSession mbs = modelBuilder.newSession();
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), mbs);
        Model orgModel = mbs.build(ModelBuilderRequest.builder()
                        .session(InternalSession.from(session))
                        .source(ModelSource.fromPath(file))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .build())
                .getEffectiveModel();

        MavenProject project = new MavenProject(orgModel);
        project.setOriginalModel(new org.apache.maven.model.Model(orgModel));
        request.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
        Model model = builder.build(session, project, file);

        assertNotNull(model);
        assertTrue(model.getProfiles().isEmpty());
    }

    static class MyModelResolver implements ModelResolver {
        @Override
        public ModelSource resolveModel(
                Session session,
                List<RemoteRepository> repositories,
                String groupId,
                String artifactId,
                String version,
                String classifier,
                Consumer<String> resolvedVersion)
                throws ModelResolverException {
            String id = groupId + ":" + artifactId + ":" + version;
            if (id.startsWith("org.sonatype.mavenbook.multi:parent:")) {
                return ModelSource.fromPath(Paths.get("src/test/resources/consumer/simple/pom.xml"));
            } else if (id.startsWith("org.sonatype.mavenbook.multi:simple-parent:")) {
                return ModelSource.fromPath(Paths.get("src/test/resources/consumer/simple/simple-parent/pom.xml"));
            } else if (id.startsWith("org.my.group:parent:")) {
                return ModelSource.fromPath(Paths.get("src/test/resources/consumer/trivial/pom.xml"));
            }
            return null;
        }
    }
}
