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
package org.apache.maven.repository.internal.type;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.Type;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Type deriver, that handles special case of "processor" type: if a dependency node is of this type, all of its
 * children need to be remapped to certain processor type as well, to end up on proper path type.
 *
 * @since 4.0.0
 * @deprecated since 4.0.0, this is internal detail of Maven.
 */
@Deprecated(since = "4.0.0")
public class TypeDeriver implements DependencyGraphTransformer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public DependencyNode transformGraph(DependencyNode root, DependencyGraphTransformationContext context) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            root.accept(new DependencyGraphDumper(
                    l -> sb.append(l).append("\n"),
                    DependencyGraphDumper.defaultsWith(
                            List.of(DependencyGraphDumper.artifactProperties(List.of(ArtifactProperties.TYPE))))));
            logger.debug("TYPES: Before transform:\n {}", sb);
        }
        root.accept(new TypeDeriverVisitor(context.getSession().getArtifactTypeRegistry()));
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            root.accept(new DependencyGraphDumper(
                    l -> sb.append(l).append("\n"),
                    DependencyGraphDumper.defaultsWith(
                            List.of(DependencyGraphDumper.artifactProperties(List.of(ArtifactProperties.TYPE))))));
            logger.debug("TYPES: After transform:\n {}", sb);
        }
        return root;
    }

    private static class TypeDeriverVisitor implements DependencyVisitor {
        private final ArtifactTypeRegistry registry;
        private final ArtifactType jar;
        private final ArtifactType classpathJar;
        private final ArtifactType modularJar;
        private final ArtifactType processor;
        private final ArtifactType classpathProcessor;
        private final ArtifactType modularProcessor;
        private final Set<String> needsDerive;
        private final ArrayDeque<ArtifactType> stack;

        private TypeDeriverVisitor(ArtifactTypeRegistry registry) {
            this.registry = requireNonNull(registry);
            this.jar = requireType(Type.JAR);
            this.classpathJar = requireType(Type.CLASSPATH_JAR);
            this.modularJar = requireType(Type.MODULAR_JAR);
            this.processor = requireType(Type.PROCESSOR);
            this.classpathProcessor = requireType(Type.CLASSPATH_PROCESSOR);
            this.modularProcessor = requireType(Type.MODULAR_PROCESSOR);
            this.needsDerive = Set.of(Type.PROCESSOR, Type.CLASSPATH_PROCESSOR, Type.MODULAR_PROCESSOR);
            this.stack = new ArrayDeque<>();
        }

        private ArtifactType requireType(String id) {
            return requireNonNull(registry.get(id), "Type " + id + " not found but is required");
        }

        @Override
        public boolean visitEnter(DependencyNode node) {
            ArtifactType currentType = jar;
            if (node.getArtifact() != null) {
                if (node.getArtifact().getProperties().containsKey(ArtifactProperties.TYPE)) {
                    currentType = registry.get(node.getArtifact()
                            .getProperty(
                                    ArtifactProperties.TYPE, node.getArtifact().getExtension()));
                    if (currentType == null) {
                        currentType = jar;
                    }
                }
                if (!stack.isEmpty()) {
                    ArtifactType parentType = stack.peek();
                    if (needsDerive.contains(parentType.getId())) {
                        Artifact artifact = node.getArtifact();
                        Map<String, String> props = new HashMap<>(artifact.getProperties());
                        ArtifactType derived = derive(parentType, currentType);
                        props.putAll(derived.getProperties());
                        node.setArtifact(artifact.setProperties(props));
                    }
                }
            }
            stack.push(currentType);
            return true;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            stack.pop();
            return true;
        }

        private ArtifactType derive(ArtifactType parentType, ArtifactType currentType) {
            ArtifactType result = currentType;
            if (jar.getId().equals(currentType.getId())) {
                result = processor;
            } else if (classpathJar.getId().equals(currentType.getId())) {
                result = classpathProcessor;
            } else if (modularJar.getId().equals(currentType.getId())) {
                result = modularProcessor;
            }
            return result;
        }
    }
}
