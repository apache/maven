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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Type;
import org.apache.maven.api.services.TypeRegistry;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Default implementation of {@link Type} and Resolver {@link ArtifactType}.
 *
 * @since 4.0.0
 */
public class TypeDeriver implements DependencyGraphTransformer {
    private final TypeRegistry typeRegistry;

    public TypeDeriver(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public DependencyNode transformGraph(DependencyNode root, DependencyGraphTransformationContext context)
            throws RepositoryException {
        if (typeRegistry == null) {
            return root;
        }
        if (root.getArtifact() == null) {
            return root;
        }
        Optional<Type> rt = typeRegistry.lookup(root.getArtifact().getProperty(ArtifactProperties.TYPE, ""));
        if (rt.isEmpty() || !rt.orElseThrow().needsDerive()) {
            return root;
        }

        Type type = rt.orElseThrow();
        ArrayDeque<DependencyNode> stack = new ArrayDeque<>();
        root.getChildren().forEach(stack::push);
        while (!stack.isEmpty()) {
            DependencyNode node = stack.pop();
            if (node.getArtifact() != null) {
                Artifact artifact = node.getArtifact();
                Map<String, String> props = new HashMap<>(artifact.getProperties());
                Optional<Type> nt = typeRegistry.lookup(props.getOrDefault(ArtifactProperties.TYPE, ""));
                if (nt.isPresent()) {
                    Type derived = type.derive(nt.orElseThrow());
                    props.put(ArtifactProperties.TYPE, derived.id());
                    node.setArtifact(artifact.setProperties(props));
                }
            }
            node.getChildren().forEach(stack::push);
        }

        return root;
    }
}
