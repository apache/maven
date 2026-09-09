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
 * <p>
 * For each direct dependency that is a processor type, this transformer records which of
 * its children (transitive deps) would need processor path types. This information is stored
 * in the transformation context so that {@link TypeDeriver} (which runs after conflict resolution)
 * can apply processor path types even to nodes whose transitive processor occurrence
 * was eliminated by conflict resolution.
 * <p>
 * Without this collector, the following scenario fails:
 * <pre>
 *   root
 *   ├── shared-lib:1.0 (type=modular-jar)         → --module-path
 *   └── my-processor:1.0 (type=modular-processor)
 *       └── shared-lib:1.0 (type=jar)              → should go to --processor-module-path
 * </pre>
 * ConflictResolver removes the transitive shared-lib (same GA, loser), so TypeDeriver
 * never sees it under the processor. This collector preserves that information.
 *
 * @since 4.0.0
 * @see TypeDeriver
 */
public class TypeCollector implements DependencyGraphTransformer {

    /**
     * Context key under which the collected processor type map is stored.
     * The value is a {@code Map<String, String>} mapping artifact conflict keys
     * (groupId:artifactId:extension:classifier) to derived processor type IDs.
     */
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
            // This direct dep is a processor — record its children's derived types
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
