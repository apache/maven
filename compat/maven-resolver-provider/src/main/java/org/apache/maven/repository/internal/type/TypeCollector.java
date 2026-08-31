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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.Type;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Collects processor type information from the dependency graph BEFORE conflict resolution.
 *
 * @since 4.0.0
 * @deprecated since 4.0.0, use {@code maven-api-impl} jar instead
 * @see TypeDeriver
 */
@Deprecated(since = "4.0.0")
public class TypeCollector implements DependencyGraphTransformer {

    public static final Object CONTEXT_KEY = TypeCollector.class.getName() + ".processorTypes";

    static final Set<String> PROCESSOR_TYPE_IDS =
            Set.of(Type.PROCESSOR, Type.CLASSPATH_PROCESSOR, Type.MODULAR_PROCESSOR);

    private static final Map<String, String> DERIVE_MAP = Map.of(
            Type.JAR, Type.PROCESSOR,
            Type.CLASSPATH_JAR, Type.CLASSPATH_PROCESSOR,
            Type.MODULAR_JAR, Type.MODULAR_PROCESSOR);

    @Override
    public DependencyNode transformGraph(DependencyNode root, DependencyGraphTransformationContext context)
            throws RepositoryException {
        Map<String, String> processorTypes = null;
        for (DependencyNode child : root.getChildren()) {
            if (child.getArtifact() == null) {
                continue;
            }
            String childType = child.getArtifact().getProperty(ArtifactProperties.TYPE, "");
            if (!PROCESSOR_TYPE_IDS.contains(childType)) {
                continue;
            }
            for (DependencyNode transitive : child.getChildren()) {
                if (transitive.getArtifact() == null) {
                    continue;
                }
                String transitiveType = transitive.getArtifact().getProperty(ArtifactProperties.TYPE, "");
                String derived = DERIVE_MAP.get(transitiveType);
                if (derived != null) {
                    if (processorTypes == null) {
                        processorTypes = new HashMap<>();
                    }
                    processorTypes.put(conflictKey(transitive), derived);
                }
            }
        }
        if (processorTypes != null) {
            context.put(CONTEXT_KEY, processorTypes);
        }
        return root;
    }

    /**
     * Builds a unique key for an artifact based on the same identity components
     * used by conflict resolution: groupId, artifactId, extension, and classifier.
     */
    static String conflictKey(DependencyNode node) {
        var a = node.getArtifact();
        return a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getExtension() + ':' + a.getClassifier();
    }
}
