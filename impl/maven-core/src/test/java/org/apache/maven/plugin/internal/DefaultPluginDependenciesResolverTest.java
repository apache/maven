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
        assertTrue(captured.getManagedDependencies() == null
                || captured.getManagedDependencies().isEmpty());
    }
}
