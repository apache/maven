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
package org.apache.maven.impl.resolver.type;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.Type;
import org.apache.maven.api.services.TypeRegistry;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyGraphTransformationContext;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class TypeDeriverTest {
    private final TypeRegistry typeRegistry = new TypeRegistry() {
        private final Map<String, Type> types =
                new DefaultTypeProvider().types().stream().collect(Collectors.toMap(DefaultType::id, t -> t));

        @Override
        public Optional<Type> lookup(String id) {
            return Optional.ofNullable(types.get(id));
        }
    };
    private final TypeDeriver subject = new TypeDeriver(typeRegistry);

    /**
     * Hack: method to align maven and resolver classifier (null vs empty string).
     */
    private static ArtifactType resolverize(Type type) {
        if (type instanceof ArtifactType at) {
            return new ArtifactType() {
                @Override
                public String getId() {
                    return at.getId();
                }

                @Override
                public String getExtension() {
                    return at.getExtension();
                }

                @Override
                public String getClassifier() {
                    return at.getClassifier() == null ? "" : at.getClassifier();
                }

                @Override
                public Map<String, String> getProperties() {
                    return at.getProperties();
                }
            };
        }
        throw new IllegalArgumentException("Cannot resolverize: " + type);
    }

    @Test
    void project() throws Exception {
        ArtifactType jar = resolverize(typeRegistry.require(Type.JAR));
        ArtifactType modularJar = resolverize(typeRegistry.require(Type.MODULAR_JAR));
        ArtifactType processor = resolverize(typeRegistry.require(Type.PROCESSOR));

        // root: "the project"
        DefaultDependencyNode node = new DefaultDependencyNode(new DefaultArtifact("project:project:1.0", jar));

        // direct: a plain JAR dependency
        DefaultDependencyNode d1 =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("deps:lib-a:1.0", jar), "compile"));
        // direct: a plain JAR dependency
        DefaultDependencyNode d2 =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("deps:lib-b:1.0", jar), "compile"));
        // direct: a processor dependency
        DefaultDependencyNode d3 = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("deps:processor:1.0", processor), "compile"));

        // transitive: processor depends on JAR
        DefaultDependencyNode d31 =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("tdeps:lib-a:1.0", jar), "compile"));
        // transitive: processor depends on modularJar
        DefaultDependencyNode d32 = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("tdeps:lib-b:1.0", modularJar), "compile"));
        d3.setChildren(List.of(d31, d32));

        node.setChildren(List.of(d1, d2, d3));

        node.accept(new DependencyGraphDumper(
                System.out::println,
                DependencyGraphDumper.defaultsWith(
                        List.of(DependencyGraphDumper.artifactProperties(List.of(ArtifactProperties.TYPE))))));

        DependencyNode transformed = subject.transformGraph(
                node, new DefaultDependencyGraphTransformationContext(mock(RepositorySystemSession.class)));

        Assertions.assertNotNull(transformed);
        transformed.accept(new DependencyGraphDumper(
                System.out::println,
                DependencyGraphDumper.defaultsWith(
                        List.of(DependencyGraphDumper.artifactProperties(List.of(ArtifactProperties.TYPE))))));

        Assertions.assertEquals(Type.MODULAR_PROCESSOR, d32.getArtifact().getProperty(ArtifactProperties.TYPE, ""));
    }

    @Test
    void plugin() throws Exception {
        ArtifactType mavenPlugin = resolverize(typeRegistry.require(Type.MAVEN_PLUGIN));
        ArtifactType jar = resolverize(typeRegistry.require(Type.JAR));
        ArtifactType classpathJar = resolverize(typeRegistry.require(Type.CLASSPATH_JAR));
        ArtifactType modularJar = resolverize(typeRegistry.require(Type.MODULAR_JAR));
        ArtifactType processor = resolverize(typeRegistry.require(Type.PROCESSOR));

        // root: "the plugin"
        DefaultDependencyNode node = new DefaultDependencyNode(new DefaultArtifact("plugin:plugin:1.0", mavenPlugin));

        // direct: a plain JAR dependency
        DefaultDependencyNode d1 =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("deps:lib-a:1.0", jar), "compile"));
        // direct: a plain JAR dependency
        DefaultDependencyNode d2 =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("deps:lib-b:1.0", jar), "compile"));
        // direct: a processor dependency
        DefaultDependencyNode d3 = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("deps:processor:1.0", processor), "compile"));

        // transitive: processor depends on classpathJar
        DefaultDependencyNode d31 = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("tdeps:lib-a:1.0", classpathJar), "compile"));
        // transitive: processor depends on modularJar
        DefaultDependencyNode d32 = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("tdeps:lib-b:1.0", modularJar), "compile"));
        d3.setChildren(List.of(d31, d32));

        node.setChildren(List.of(d1, d2, d3));

        node.accept(new DependencyGraphDumper(
                System.out::println,
                DependencyGraphDumper.defaultsWith(
                        List.of(DependencyGraphDumper.artifactProperties(List.of(ArtifactProperties.TYPE))))));

        DependencyNode transformed = subject.transformGraph(
                node, new DefaultDependencyGraphTransformationContext(mock(RepositorySystemSession.class)));

        Assertions.assertNotNull(transformed);
        transformed.accept(new DependencyGraphDumper(
                System.out::println,
                DependencyGraphDumper.defaultsWith(
                        List.of(DependencyGraphDumper.artifactProperties(List.of(ArtifactProperties.TYPE))))));

        Assertions.assertEquals(Type.MODULAR_PROCESSOR, d32.getArtifact().getProperty(ArtifactProperties.TYPE, ""));
    }
}
