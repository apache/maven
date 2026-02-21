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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.Sources;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.impl.DefaultArtifactCoordinatesFactory;
import org.apache.maven.impl.DefaultDependencyCoordinatesFactory;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.cache.DefaultRequestCacheFactory;
import org.apache.maven.impl.resolver.MavenVersionScheme;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.transformation.AbstractRepositoryTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerPomBuilderTest extends AbstractRepositoryTestCase {

    @Inject
    PomBuilder builder;

    @Inject
    ModelBuilder modelBuilder;

    @Override
    protected List<Object> getSessionServices() {
        List<Object> services = new ArrayList<>(super.getSessionServices());

        DependencyResolver dependencyResolver = Mockito.mock(DependencyResolver.class);
        DependencyResolverResult resolverResult = Mockito.mock(DependencyResolverResult.class);
        Mockito.when(dependencyResolver.collect(
                        Mockito.any(Session.class),
                        Mockito.any(DependencyCoordinates.class),
                        Mockito.any(PathScope.class)))
                .thenReturn(resolverResult);
        Node node = Mockito.mock(Node.class);
        Mockito.when(resolverResult.getRoot()).thenReturn(node);
        Node child = Mockito.mock(Node.class);
        Mockito.when(node.getChildren()).thenReturn(List.of(child));

        services.addAll(List.of(
                new DefaultRequestCacheFactory(),
                new DefaultArtifactCoordinatesFactory(),
                new DefaultDependencyCoordinatesFactory(),
                new DefaultVersionParser(new DefaultModelVersionParser(new MavenVersionScheme())),
                dependencyResolver));
        return services;
    }

    /**
     * Configures {@link #session} with the root directory of a test in {@code src/test/resources/consumer}.
     * Returns the request in case the caller wants to apply more configuration.
     */
    private MavenExecutionRequest setRootDirectory(String test) {
        MavenExecutionRequest request = InternalMavenSession.from(InternalSession.from(session))
                .getMavenSession()
                .getRequest();
        request.setRootDirectory(Paths.get("src/test/resources/consumer", test));
        return request;
    }

    /**
     * Builds the effective model for the given {@code pom.xml} file.
     */
    private MavenProject getEffectiveModel(Path file) {
        ModelBuilder.ModelBuilderSession mbs = modelBuilder.newSession();
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), mbs);
        Model orgModel = mbs.build(ModelBuilderRequest.builder()
                        .session(InternalSession.from(session))
                        .source(Sources.buildSource(file))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .build())
                .getEffectiveModel();

        MavenProject project = new MavenProject(orgModel);
        project.setOriginalModel(new org.apache.maven.model.Model(orgModel));
        return project;
    }

    @Test
    void testTrivialConsumer() throws Exception {
        setRootDirectory("trivial");
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = builder.build(session, project, Sources.buildSource(file));

        assertNotNull(model);
        assertNotNull(model.getDependencies());
    }

    @Test
    void testSimpleConsumer() throws Exception {
        MavenExecutionRequest request = setRootDirectory("simple");
        request.getUserProperties().setProperty("changelist", "MNG6957");
        Path file = Paths.get("src/test/resources/consumer/simple/simple-parent/simple-weather/pom.xml");

        MavenProject project = getEffectiveModel(file);
        request.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
        Model model = builder.build(session, project, Sources.buildSource(file));

        assertNotNull(model);
        assertFalse(model.getDependencies().isEmpty());
        assertTrue(model.getProfiles().isEmpty());
    }

    @Test
    void testMultiModuleConsumer() throws Exception {
        setRootDirectory("multi-module");
        Path file = Paths.get("src/test/resources/consumer/multi-module/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = builder.build(session, project, Sources.buildSource(file));

        assertNotNull(model);
        assertNull(model.getBuild());
        assertTrue(model.getDependencies().isEmpty());
        assertFalse(model.getDependencyManagement().getDependencies().isEmpty());
    }

    @Test
    void testScmInheritance() throws Exception {
        Model model = Model.newBuilder()
                .scm(Scm.newBuilder()
                        .connection("scm:git:https://github.com/apache/maven-project.git")
                        .developerConnection("scm:git:https://github.com/apache/maven-project.git")
                        .url("https://github.com/apache/maven-project")
                        .childScmConnectionInheritAppendPath("true")
                        .childScmUrlInheritAppendPath("true")
                        .childScmDeveloperConnectionInheritAppendPath("true")
                        .build())
                .build();
        Model transformed = DefaultConsumerPomBuilder.transformNonPom(model, null);
        assertNull(transformed.getScm().getChildScmConnectionInheritAppendPath());
        assertNull(transformed.getScm().getChildScmUrlInheritAppendPath());
        assertNull(transformed.getScm().getChildScmDeveloperConnectionInheritAppendPath());
    }
}
