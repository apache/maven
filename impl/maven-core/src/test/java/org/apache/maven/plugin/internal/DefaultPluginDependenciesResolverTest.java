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
package org.apache.maven.plugin.internal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginDependenciesResolverTest {

    @Test
    void testResolvePluginInjectsCoreManagedDependencies() throws Exception {
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        List<MavenPluginDependenciesValidator> validators = Collections.emptyList();

        ClassWorld classWorld = new ClassWorld();
        ClassRealm classRealm = classWorld.newRealm("test.core", getClass().getClassLoader());

        Set<String> exportedArtifacts = new LinkedHashSet<>();
        exportedArtifacts.add("org.apache.maven:maven-core");
        exportedArtifacts.add("org.apache.maven.test:dummy-missing");
        exportedArtifacts.add("org.slf4j:slf4j-api");

        CoreExports coreExports = new CoreExports(classRealm, exportedArtifacts, Collections.emptySet());

        DefaultPluginDependenciesResolver resolver =
                new DefaultPluginDependenciesResolver(repoSystem, validators, coreExports);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("3.13.0");

        Artifact pluginArtifact =
                new DefaultArtifact("org.apache.maven.plugins", "maven-compiler-plugin", "jar", "3.13.0");

        ArtifactTypeRegistry typeRegistry = mock(ArtifactTypeRegistry.class);
        ArtifactType pluginType = mock(ArtifactType.class);
        when(pluginType.getExtension()).thenReturn("jar");
        when(typeRegistry.get("maven-plugin")).thenReturn(pluginType);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setArtifactTypeRegistry(typeRegistry);
        InternalSession internalSession = mock(InternalSession.class);
        InternalSession.associate(session, internalSession);

        DependencyNode rootNode = mock(DependencyNode.class);
        CollectResult collectResult = new CollectResult(new CollectRequest());
        collectResult.setRoot(rootNode);
        when(repoSystem.collectDependencies(any(), any(CollectRequest.class))).thenReturn(collectResult);

        DependencyResult dependencyResult = new DependencyResult(new DependencyRequest());
        when(repoSystem.resolveDependencies(any(), any(DependencyRequest.class)))
                .thenReturn(dependencyResult);

        DependencyFilter filter = mock(DependencyFilter.class);

        resolver.resolvePluginAndFlatten(plugin, pluginArtifact, filter, Collections.emptyList(), session);

        ArgumentCaptor<CollectRequest> requestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(repoSystem).collectDependencies(any(), requestCaptor.capture());

        CollectRequest captured = requestCaptor.getValue();
        assertNotNull(captured.getManagedDependencies());

        List<Dependency> managedDeps = captured.getManagedDependencies();
        assertFalse(managedDeps.isEmpty(), "Managed dependencies should not be empty");

        Dependency mavenCoreDep = managedDeps.stream()
                .filter(d -> "org.apache.maven".equals(d.getArtifact().getGroupId())
                        && "maven-core".equals(d.getArtifact().getArtifactId()))
                .findFirst()
                .orElse(null);

        assertNotNull(mavenCoreDep, "maven-core managed dependency should be present");
        assertEquals(DependencyScope.PROVIDED.id(), mavenCoreDep.getScope());
        assertNotNull(mavenCoreDep.getArtifact().getVersion());
        assertFalse(mavenCoreDep.getArtifact().getVersion().isEmpty());

        boolean dummyPresent = managedDeps.stream()
                .anyMatch(d -> "org.apache.maven.test".equals(d.getArtifact().getGroupId()));
        assertFalse(dummyPresent, "Artifact without pom.properties should be safely omitted");

        boolean slf4jPresent = managedDeps.stream()
                .anyMatch(d -> "org.slf4j".equals(d.getArtifact().getGroupId()));
        assertFalse(slf4jPresent, "Third-party exported library slf4j-api should not be in managed dependencies");
    }

    @Test
    void testResolvePluginWithoutCoreExports() throws Exception {
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        List<MavenPluginDependenciesValidator> validators = Collections.emptyList();

        DefaultPluginDependenciesResolver resolver = new DefaultPluginDependenciesResolver(repoSystem, validators);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("3.13.0");

        org.apache.maven.model.Dependency coreDep = new org.apache.maven.model.Dependency();
        coreDep.setGroupId("org.apache.maven");
        coreDep.setArtifactId("maven-core");
        coreDep.setVersion("3.9.0");
        coreDep.setScope("compile");
        plugin.addDependency(coreDep);

        Artifact pluginArtifact =
                new DefaultArtifact("org.apache.maven.plugins", "maven-compiler-plugin", "jar", "3.13.0");

        ArtifactTypeRegistry typeRegistry = mock(ArtifactTypeRegistry.class);
        ArtifactType pluginType = mock(ArtifactType.class);
        when(pluginType.getExtension()).thenReturn("jar");
        when(typeRegistry.get("maven-plugin")).thenReturn(pluginType);
        when(typeRegistry.get("jar")).thenReturn(pluginType);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setArtifactTypeRegistry(typeRegistry);
        InternalSession internalSession = mock(InternalSession.class);
        InternalSession.associate(session, internalSession);

        DependencyNode rootNode = mock(DependencyNode.class);
        CollectResult collectResult = new CollectResult(new CollectRequest());
        collectResult.setRoot(rootNode);
        when(repoSystem.collectDependencies(any(), any(CollectRequest.class))).thenReturn(collectResult);

        DependencyResult dependencyResult = new DependencyResult(new DependencyRequest());
        when(repoSystem.resolveDependencies(any(), any(DependencyRequest.class)))
                .thenReturn(dependencyResult);

        DependencyFilter filter = mock(DependencyFilter.class);

        resolver.resolvePluginAndFlatten(plugin, pluginArtifact, filter, Collections.emptyList(), session);

        ArgumentCaptor<CollectRequest> requestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(repoSystem).collectDependencies(any(), requestCaptor.capture());

        CollectRequest captured = requestCaptor.getValue();
        assertTrue(captured.getManagedDependencies() == null
                || captured.getManagedDependencies().isEmpty());
        List<Dependency> capturedDeps = captured.getDependencies();
        assertEquals(1, capturedDeps.size());
        Dependency directCore = capturedDeps.get(0);
        assertEquals("runtime", directCore.getScope(), "Without core exports, dependency scope remains runtime");
        assertEquals(
                "3.9.0",
                directCore.getArtifact().getVersion(),
                "Without core exports, dependency version remains declared version");
    }

    @Test
    void testResolvePluginAlignsExplicitCoreDependencies() throws Exception {
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        List<MavenPluginDependenciesValidator> validators = Collections.emptyList();

        ClassWorld classWorld = new ClassWorld();
        ClassRealm classRealm =
                classWorld.newRealm("test.core.direct", getClass().getClassLoader());

        Set<String> exportedArtifacts = new LinkedHashSet<>();
        exportedArtifacts.add("org.apache.maven:maven-core");
        exportedArtifacts.add("org.slf4j:slf4j-api");

        CoreExports coreExports = new CoreExports(classRealm, exportedArtifacts, Collections.emptySet());

        DefaultPluginDependenciesResolver resolver =
                new DefaultPluginDependenciesResolver(repoSystem, validators, coreExports);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("3.13.0");

        org.apache.maven.model.Dependency coreDep = new org.apache.maven.model.Dependency();
        coreDep.setGroupId("org.apache.maven");
        coreDep.setArtifactId("maven-core");
        coreDep.setVersion("3.9.0");
        coreDep.setScope("compile");
        coreDep.setOptional(true);
        org.apache.maven.model.Exclusion exclusion = new org.apache.maven.model.Exclusion();
        exclusion.setGroupId("org.apache.maven");
        exclusion.setArtifactId("maven-settings");
        coreDep.addExclusion(exclusion);
        plugin.addDependency(coreDep);

        org.apache.maven.model.Dependency nonCoreDep = new org.apache.maven.model.Dependency();
        nonCoreDep.setGroupId("commons-io");
        nonCoreDep.setArtifactId("commons-io");
        nonCoreDep.setVersion("2.11.0");
        nonCoreDep.setScope("compile");
        plugin.addDependency(nonCoreDep);

        org.apache.maven.model.Dependency slf4jDep = new org.apache.maven.model.Dependency();
        slf4jDep.setGroupId("org.slf4j");
        slf4jDep.setArtifactId("slf4j-api");
        slf4jDep.setVersion("1.7.36");
        slf4jDep.setScope("compile");
        plugin.addDependency(slf4jDep);

        Artifact pluginArtifact =
                new DefaultArtifact("org.apache.maven.plugins", "maven-compiler-plugin", "jar", "3.13.0");

        ArtifactTypeRegistry typeRegistry = mock(ArtifactTypeRegistry.class);
        ArtifactType pluginType = mock(ArtifactType.class);
        when(pluginType.getExtension()).thenReturn("jar");
        when(typeRegistry.get("maven-plugin")).thenReturn(pluginType);
        when(typeRegistry.get("jar")).thenReturn(pluginType);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setArtifactTypeRegistry(typeRegistry);
        InternalSession internalSession = mock(InternalSession.class);
        InternalSession.associate(session, internalSession);

        DependencyNode rootNode = mock(DependencyNode.class);
        CollectResult collectResult = new CollectResult(new CollectRequest());
        collectResult.setRoot(rootNode);
        when(repoSystem.collectDependencies(any(), any(CollectRequest.class))).thenReturn(collectResult);

        DependencyResult dependencyResult = new DependencyResult(new DependencyRequest());
        when(repoSystem.resolveDependencies(any(), any(DependencyRequest.class)))
                .thenReturn(dependencyResult);

        DependencyFilter filter = mock(DependencyFilter.class);

        resolver.resolvePluginAndFlatten(plugin, pluginArtifact, filter, Collections.emptyList(), session);

        ArgumentCaptor<CollectRequest> requestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(repoSystem).collectDependencies(any(), requestCaptor.capture());

        CollectRequest captured = requestCaptor.getValue();
        List<Dependency> capturedDeps = captured.getDependencies();
        assertEquals(3, capturedDeps.size());

        Dependency directCore = capturedDeps.stream()
                .filter(d -> "org.apache.maven".equals(d.getArtifact().getGroupId())
                        && "maven-core".equals(d.getArtifact().getArtifactId()))
                .findFirst()
                .orElse(null);
        assertNotNull(directCore, "Direct core dependency should be present");
        assertEquals(
                DependencyScope.PROVIDED.id(),
                directCore.getScope(),
                "Direct core dependency scope should be aligned to provided");
        assertNotNull(directCore.getArtifact().getVersion());
        assertFalse(
                "3.9.0".equals(directCore.getArtifact().getVersion()),
                "Direct core dependency version should be aligned to running version");
        assertFalse(directCore.getArtifact().getVersion().isEmpty());
        assertTrue(directCore.isOptional(), "Direct core dependency optional flag should be preserved");
        assertEquals(1, directCore.getExclusions().size(), "Direct core dependency exclusions should be preserved");
        Exclusion capturedExclusion = directCore.getExclusions().iterator().next();
        assertEquals("org.apache.maven", capturedExclusion.getGroupId());
        assertEquals("maven-settings", capturedExclusion.getArtifactId());

        Dependency directNonCore = capturedDeps.stream()
                .filter(d -> "commons-io".equals(d.getArtifact().getGroupId()))
                .findFirst()
                .orElse(null);
        assertNotNull(directNonCore, "Direct non-core dependency should be present");
        assertEquals("runtime", directNonCore.getScope());
        assertEquals("2.11.0", directNonCore.getArtifact().getVersion());
        assertFalse(directNonCore.isOptional(), "Direct non-core dependency optional flag should be false");

        Dependency capturedSlf4j = capturedDeps.stream()
                .filter(d -> "org.slf4j".equals(d.getArtifact().getGroupId())
                        && "slf4j-api".equals(d.getArtifact().getArtifactId()))
                .findFirst()
                .orElse(null);
        assertNotNull(capturedSlf4j, "Direct slf4j dependency should be present");
        assertEquals("runtime", capturedSlf4j.getScope(), "Excluded third-party library should remain runtime scope");
        assertEquals(
                "1.7.36",
                capturedSlf4j.getArtifact().getVersion(),
                "Excluded third-party library version should not be aligned");

        Dependency managedCore = captured.getManagedDependencies().stream()
                .filter(d -> "org.apache.maven".equals(d.getArtifact().getGroupId())
                        && "maven-core".equals(d.getArtifact().getArtifactId()))
                .findFirst()
                .orElse(null);
        assertNotNull(
                managedCore,
                "Core dependencies should still be present in managed dependencies for transitive mediation");
        assertEquals(
                managedCore.getArtifact().getVersion(),
                directCore.getArtifact().getVersion(),
                "Direct core dependency version should match the synthetic managed core dependency version");
    }

    @Test
    void testResolveCoreExtensionDoesNotInjectManagedDependencies() throws Exception {
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        List<MavenPluginDependenciesValidator> validators = Collections.emptyList();

        ClassWorld classWorld = new ClassWorld();
        ClassRealm classRealm = classWorld.newRealm("test.core.ext", getClass().getClassLoader());

        Set<String> exportedArtifacts = new LinkedHashSet<>();
        exportedArtifacts.add("org.apache.maven:maven-core");

        CoreExports coreExports = new CoreExports(classRealm, exportedArtifacts, Collections.emptySet());

        DefaultPluginDependenciesResolver resolver =
                new DefaultPluginDependenciesResolver(repoSystem, validators, coreExports);

        Plugin plugin = new Plugin();
        plugin.setGroupId("eu.maveniverse.maven.mimir");
        plugin.setArtifactId("extension3");
        plugin.setVersion("0.12.3");

        org.apache.maven.model.Dependency coreDep = new org.apache.maven.model.Dependency();
        coreDep.setGroupId("org.apache.maven");
        coreDep.setArtifactId("maven-core");
        coreDep.setVersion("3.9.0");
        coreDep.setScope("compile");
        plugin.addDependency(coreDep);

        org.apache.maven.model.Dependency nonCoreDep = new org.apache.maven.model.Dependency();
        nonCoreDep.setGroupId("commons-io");
        nonCoreDep.setArtifactId("commons-io");
        nonCoreDep.setVersion("2.11.0");
        nonCoreDep.setScope("compile");
        plugin.addDependency(nonCoreDep);

        Artifact pluginArtifact = new DefaultArtifact("eu.maveniverse.maven.mimir", "extension3", "jar", "0.12.3");

        ArtifactTypeRegistry typeRegistry = mock(ArtifactTypeRegistry.class);
        ArtifactType pluginType = mock(ArtifactType.class);
        when(pluginType.getExtension()).thenReturn("jar");
        when(typeRegistry.get("maven-plugin")).thenReturn(pluginType);
        when(typeRegistry.get("jar")).thenReturn(pluginType);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setArtifactTypeRegistry(typeRegistry);
        InternalSession internalSession = mock(InternalSession.class);
        InternalSession.associate(session, internalSession);

        org.eclipse.aether.resolution.ArtifactDescriptorResult descriptorResult =
                new org.eclipse.aether.resolution.ArtifactDescriptorResult(
                        new org.eclipse.aether.resolution.ArtifactDescriptorRequest());
        descriptorResult.setArtifact(pluginArtifact);
        when(repoSystem.readArtifactDescriptor(any(), any())).thenReturn(descriptorResult);

        DependencyNode rootNode = mock(DependencyNode.class);
        CollectResult collectResult = new CollectResult(new CollectRequest());
        collectResult.setRoot(rootNode);
        when(repoSystem.collectDependencies(any(), any(CollectRequest.class))).thenReturn(collectResult);

        DependencyResult dependencyResult = new DependencyResult(new DependencyRequest());
        when(repoSystem.resolveDependencies(any(), any(DependencyRequest.class)))
                .thenReturn(dependencyResult);

        DependencyFilter filter = mock(DependencyFilter.class);

        resolver.resolveCoreExtensionAndFlatten(plugin, filter, Collections.emptyList(), session);

        ArgumentCaptor<CollectRequest> requestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(repoSystem).collectDependencies(any(), requestCaptor.capture());

        CollectRequest captured = requestCaptor.getValue();
        assertTrue(
                captured.getManagedDependencies() == null
                        || captured.getManagedDependencies().isEmpty(),
                "Core extension resolution should not receive managed dependencies");

        List<Dependency> capturedDeps = captured.getDependencies();
        assertEquals(2, capturedDeps.size());

        Dependency directCore = capturedDeps.stream()
                .filter(d -> "org.apache.maven".equals(d.getArtifact().getGroupId())
                        && "maven-core".equals(d.getArtifact().getArtifactId()))
                .findFirst()
                .orElse(null);
        assertNotNull(directCore, "Direct core dependency in core extension should be present");
        assertEquals(
                "runtime",
                directCore.getScope(),
                "Core extension direct dependencies must not be aligned to provided scope");
        assertEquals(
                "3.9.0",
                directCore.getArtifact().getVersion(),
                "Core extension direct dependency version must remain untouched");

        Dependency directNonCore = capturedDeps.stream()
                .filter(d -> "commons-io".equals(d.getArtifact().getGroupId()))
                .findFirst()
                .orElse(null);
        assertNotNull(directNonCore, "Direct non-core dependency in core extension should be present");
        assertEquals("runtime", directNonCore.getScope());
        assertEquals("2.11.0", directNonCore.getArtifact().getVersion());
    }
}
