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
import org.apache.maven.api.model.Dependency;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        .source(Sources.buildSource(file))
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
                        .source(Sources.buildSource(file))
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

    @Test
    void testNewMaven4ScopesInConsumerPom() throws Exception {
        // Create dependencies with new Maven 4 scopes
        Dependency compileOnlyDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("compile-only-dep")
                .version("1.0.0")
                .scope("compile-only")
                .build();

        Dependency testOnlyDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("test-only-dep")
                .version("1.0.0")
                .scope("test-only")
                .build();

        Dependency testRuntimeDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("test-runtime-dep")
                .version("1.0.0")
                .scope("test-runtime")
                .build();

        Dependency compileDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("compile-dep")
                .version("1.0.0")
                .scope("compile")
                .build();

        // Transform using the consumer POM builder
        DefaultConsumerPomBuilder builder = new DefaultConsumerPomBuilder(null);

        // Test the transformation method directly
        Dependency transformedCompileOnly = builder.transformDependencyForConsumerPom(compileOnlyDep);
        Dependency transformedTestOnly = builder.transformDependencyForConsumerPom(testOnlyDep);
        Dependency transformedTestRuntime = builder.transformDependencyForConsumerPom(testRuntimeDep);
        Dependency transformedCompile = builder.transformDependencyForConsumerPom(compileDep);

        // New Maven 4 scopes handling
        assertNull(transformedCompileOnly, "compile-only dependencies should be omitted from consumer POM");
        assertNull(transformedTestOnly, "test-only dependencies should be omitted from consumer POM");
        assertNotNull(
                transformedTestRuntime, "test-runtime dependencies should be preserved as 'test' in consumer POM");
        assertEquals("test", transformedTestRuntime.getScope());

        // Standard scopes should be preserved
        assertNotNull(transformedCompile, "compile dependencies should be preserved in consumer POM");
        assertEquals("compile", transformedCompile.getScope());
    }

    @Test
    void testNewMaven4ScopesInDependencyManagement() throws Exception {
        // Create managed dependencies with new Maven 4 scopes
        Dependency compileOnlyManaged = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("compile-only-managed")
                .version("1.0.0")
                .scope("compile-only")
                .build();

        Dependency testOnlyManaged = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("test-only-managed")
                .version("1.0.0")
                .scope("test-only")
                .build();

        // Test the transformation method directly
        DefaultConsumerPomBuilder builder = new DefaultConsumerPomBuilder(null);

        Dependency transformedCompileOnlyManaged = builder.transformDependencyForConsumerPom(compileOnlyManaged);
        Dependency transformedTestOnlyManaged = builder.transformDependencyForConsumerPom(testOnlyManaged);

        // New Maven 4 scopes should be filtered out even in dependency management
        assertNull(
                transformedCompileOnlyManaged, "compile-only managed dependencies should be omitted from consumer POM");
        assertNull(transformedTestOnlyManaged, "test-only managed dependencies should be omitted from consumer POM");
    }
}
