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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.Type;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.impl.resolver.artifact.MavenArtifactProperties;
import org.apache.maven.impl.resolver.scopes.Maven4ScopeManagerConfiguration;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyGraphTransformationContext;
import org.eclipse.aether.internal.impl.scope.ManagedScopeDeriver;
import org.eclipse.aether.internal.impl.scope.ManagedScopeSelector;
import org.eclipse.aether.internal.impl.scope.ScopeManagerImpl;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConfigurableVersionSelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionRange;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Demonstrates the conflict between ConflictResolver and TypeDeriver:
 * when the same artifact is both a direct dependency (modular-jar)
 * and a transitive dependency of a processor, ConflictResolver eliminates
 * the transitive occurrence BEFORE TypeDeriver can assign processor path types.
 *
 * Scenario (reproduces maven-compiler-plugin#1039):
 * <pre>
 *   root (project)
 *   ├── shared-lib:1.0 (type=modular-jar)          ← direct dependency, goes to --module-path
 *   └── my-processor:1.0 (type=modular-processor)   ← annotation processor
 *       └── shared-lib:1.0 (type=jar)               ← transitive, SHOULD go to --processor-module-path
 * </pre>
 *
 * After conflict resolution, only one shared-lib node survives (the direct one).
 * TypeDeriver never sees the transitive occurrence, so it can't add processor path types.
 * Result: shared-lib ends up ONLY on --module-path, NOT on --processor-module-path.
 */
class TypeDeriverWithConflictResolutionTest {
    private final ArtifactTypeRegistry typeRegistry = new TypeRegistryAdapter(new TypeRegistry() {
        private final Map<String, Type> types =
                new DefaultTypeProvider().types().stream().collect(Collectors.toMap(DefaultType::id, t -> t));

        @Override
        public Optional<Type> lookup(String id) {
            return Optional.ofNullable(types.get(id));
        }
    });

    /**
     * This test demonstrates the problem: when a dependency is both a direct dep (modular-jar)
     * and a transitive dep of a processor, the full transformer chain (ConflictResolver + TypeDeriver)
     * loses the processor type information.
     *
     * The shared-lib should have BOTH modular-jar AND modular-processor path type properties,
     * but after conflict resolution it only retains modular-jar.
     */
    @Test
    void sharedDependencyLosesProcessorType() throws Exception {
        var scopeManager = new ScopeManagerImpl(Maven4ScopeManagerConfiguration.INSTANCE);

        RepositorySystemSession session = mock(RepositorySystemSession.class);
        when(session.getArtifactTypeRegistry()).thenReturn(typeRegistry);
        when(session.getConfigProperties()).thenReturn(Collections.emptyMap());

        ArtifactType jar = requireNonNull(typeRegistry.get(Type.JAR));
        ArtifactType modularJar = requireNonNull(typeRegistry.get(Type.MODULAR_JAR));
        ArtifactType modularProcessor = requireNonNull(typeRegistry.get(Type.MODULAR_PROCESSOR));

        // root: "the project"
        DefaultDependencyNode root = new DefaultDependencyNode(new DefaultArtifact("project:project:1.0", jar));

        // direct dep: shared-lib as modular-jar (goes to --module-path)
        DefaultDependencyNode directSharedLib = depNode("com.example:shared-lib:1.0", modularJar, "compile");

        // direct dep: annotation processor as modular-processor
        DefaultDependencyNode processorNode = depNode("com.example:my-processor:1.0", modularProcessor, "compile");

        // transitive dep of processor: shared-lib as plain jar
        // (this is how it appears in my-processor's POM — just a regular jar dependency)
        DefaultDependencyNode transitiveSharedLib = depNode("com.example:shared-lib:1.0", jar, "compile");
        processorNode.setChildren(new ArrayList<>(List.of(transitiveSharedLib)));

        root.setChildren(new ArrayList<>(List.of(directSharedLib, processorNode)));

        // Run the full transformer chain as configured in MavenSessionBuilderSupplier:
        // TypeCollector (before conflict resolution) → ConflictResolver → TypeDeriver (after)
        DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(
                new TypeCollector(),
                new ConflictResolver(
                        new ConfigurableVersionSelector(),
                        new ManagedScopeSelector(scopeManager),
                        new SimpleOptionalitySelector(),
                        new ManagedScopeDeriver(scopeManager)),
                new TypeDeriver());

        DependencyNode transformed =
                transformer.transformGraph(root, new DefaultDependencyGraphTransformationContext(session));

        // Find the surviving shared-lib node
        DependencyNode survivingSharedLib = findNode(transformed, "com.example", "shared-lib");
        assertNotNull(survivingSharedLib, "shared-lib should survive conflict resolution");

        // The main type should still be modular-jar (from the winning direct dep)
        String actualType = survivingSharedLib.getArtifact().getProperty(ArtifactProperties.TYPE, "");
        assertEquals(Type.MODULAR_JAR, actualType, "main type should remain modular-jar");

        // ASSERT: The PROCESSOR_TYPE property should be set, indicating the artifact
        // is also needed on --processor-module-path
        String processorType =
                survivingSharedLib.getArtifact().getProperty(MavenArtifactProperties.PROCESSOR_TYPE, null);
        assertEquals(
                Type.PROCESSOR,
                processorType,
                "shared-lib should have PROCESSOR_TYPE property because it's a transitive dep of a processor");
    }

    /**
     * Control test: TypeDeriver alone (without ConflictResolver) correctly derives
     * processor types for transitive deps. This passes — proving TypeDeriver logic is correct
     * when conflict resolution doesn't interfere.
     */
    @Test
    void typeDeriverAloneWorksCorrectly() throws Exception {
        RepositorySystemSession session = mock(RepositorySystemSession.class);
        when(session.getArtifactTypeRegistry()).thenReturn(typeRegistry);

        ArtifactType jar = requireNonNull(typeRegistry.get(Type.JAR));
        ArtifactType modularProcessor = requireNonNull(typeRegistry.get(Type.MODULAR_PROCESSOR));

        DefaultDependencyNode root = new DefaultDependencyNode(new DefaultArtifact("project:project:1.0", jar));

        // processor with a transitive jar dep
        DefaultDependencyNode processorNode = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("com.example:my-processor:1.0", modularProcessor), "compile"));

        DefaultDependencyNode transitiveLib = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("com.example:shared-lib:1.0", jar), "compile"));
        processorNode.setChildren(new ArrayList<>(List.of(transitiveLib)));

        root.setChildren(new ArrayList<>(List.of(processorNode)));

        // Run ONLY TypeDeriver (no ConflictResolver)
        TypeDeriver typeDeriver = new TypeDeriver();
        typeDeriver.transformGraph(root, new DefaultDependencyGraphTransformationContext(session));

        // TypeDeriver correctly derives: jar under modularProcessor → processor type
        String derivedType = transitiveLib.getArtifact().getProperty(ArtifactProperties.TYPE, "");
        assertEquals(Type.PROCESSOR, derivedType, "TypeDeriver should derive jar→processor under modularProcessor");
    }

    /**
     * Creates a DefaultDependencyNode with a proper VersionConstraint
     * (required by ConflictResolver's ConfigurableVersionSelector).
     */
    private static DefaultDependencyNode depNode(String coords, ArtifactType type, String scope) {
        DefaultDependencyNode node =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact(coords, type), scope));
        String version = node.getArtifact().getVersion();
        node.setVersionConstraint(new SimpleVersionConstraint(new SimpleVersion(version)));
        node.setVersion(new SimpleVersion(version));
        return node;
    }

    private static DependencyNode findNode(DependencyNode root, String groupId, String artifactId) {
        if (root.getArtifact() != null
                && groupId.equals(root.getArtifact().getGroupId())
                && artifactId.equals(root.getArtifact().getArtifactId())) {
            return root;
        }
        for (DependencyNode child : root.getChildren()) {
            DependencyNode found = findNode(child, groupId, artifactId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private record SimpleVersion(String version) implements Version {
        @Override
        public int compareTo(Version o) {
            return version.compareTo(o.toString());
        }

        @Override
        public String toString() {
            return version;
        }
    }

    private record SimpleVersionConstraint(Version version) implements VersionConstraint {
        @Override
        public VersionRange getRange() {
            return null; // fixed version, no range
        }

        @Override
        public Version getVersion() {
            return version;
        }

        @Override
        public boolean containsVersion(Version ver) {
            return version.equals(ver);
        }
    }
}
