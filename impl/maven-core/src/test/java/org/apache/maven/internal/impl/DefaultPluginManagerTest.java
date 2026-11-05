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
package org.apache.maven.internal.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Plugin;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecyclePluginResolver;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultPluginManagerTest {

    @Test
    void loadsPluginRuntimeForProjectDeclaration() throws Exception {
        MavenPluginManager legacyPluginManager = mock(MavenPluginManager.class);
        LifecyclePluginResolver lifecyclePluginResolver = mock(LifecyclePluginResolver.class);
        Lookup lookup = mock(Lookup.class);
        InternalMavenSession session = mock(InternalMavenSession.class);
        MavenSession mavenSession = mock(MavenSession.class);
        MavenSession executionSession = mock(MavenSession.class);
        DefaultProject project = mock(DefaultProject.class);
        MavenProject mavenProject = new MavenProject();
        Build build = new Build();
        org.apache.maven.model.Plugin model = new org.apache.maven.model.Plugin();
        model.setGroupId("org.example");
        model.setArtifactId("example-plugin");
        model.setVersion("1.0");
        build.addPlugin(model);
        mavenProject.setBuild(build);

        PluginDescriptor descriptor = mock(PluginDescriptor.class);
        org.apache.maven.api.plugin.descriptor.PluginDescriptor apiDescriptor =
                mock(org.apache.maven.api.plugin.descriptor.PluginDescriptor.class);
        ClassRealm realm = mock(ClassRealm.class);
        DependencyNode dependencyNode = mock(DependencyNode.class);
        org.eclipse.aether.graph.Dependency resolverDependency = mock(org.eclipse.aether.graph.Dependency.class);
        org.apache.maven.api.Dependency dependency = mock(org.apache.maven.api.Dependency.class);

        when(session.getMavenSession()).thenReturn(mavenSession);
        when(mavenSession.clone()).thenReturn(executionSession);
        when(project.getProject()).thenReturn(mavenProject);
        when(legacyPluginManager.getPluginDescriptor(eq(model), any(), any())).thenReturn(descriptor);
        when(descriptor.getPluginDescriptorV4()).thenReturn(apiDescriptor);
        when(descriptor.getClassRealm()).thenReturn(realm);
        when(descriptor.getDependencyNode()).thenReturn(dependencyNode);
        when(dependencyNode.getDependency()).thenReturn(resolverDependency);
        when(dependencyNode.getChildren()).thenReturn(List.of());
        when(session.getDependency(resolverDependency)).thenReturn(dependency);
        when(dependency.getGroupId()).thenReturn("org.example");
        when(dependency.getArtifactId()).thenReturn("plugin-spi");

        DefaultPluginManager manager = new DefaultPluginManager(legacyPluginManager, lifecyclePluginResolver, lookup);

        Plugin result = manager.loadPlugin(session, project, model.getDelegate());

        assertSame(model.getDelegate(), result.getModel());
        assertSame(apiDescriptor, result.getDescriptor());
        assertSame(realm, result.getRealm().classLoader());
        assertEquals(Optional.empty(), result.getRealm().moduleLayer());
        assertEquals(List.of(dependency), result.getDependencies().stream().toList());
        assertSame(dependency, result.getDependenciesMap().get("org.example:plugin-spi"));
        verify(executionSession).setCurrentProject(mavenProject);
        verify(lifecyclePluginResolver).resolveMissingPluginVersions(mavenProject, executionSession);
        verify(legacyPluginManager).setupPluginRealm(descriptor, executionSession, null, null, null);
    }

    @Test
    void preservesUndeclaredPluginError() throws Exception {
        MavenPluginManager legacyPluginManager = mock(MavenPluginManager.class);
        LifecyclePluginResolver lifecyclePluginResolver = mock(LifecyclePluginResolver.class);
        InternalMavenSession session = mock(InternalMavenSession.class);
        MavenSession mavenSession = mock(MavenSession.class);
        MavenSession executionSession = mock(MavenSession.class);
        DefaultProject project = mock(DefaultProject.class);
        MavenProject mavenProject = new MavenProject();
        Build build = new Build();
        org.apache.maven.model.Plugin model = new org.apache.maven.model.Plugin();
        model.setGroupId("org.example");
        model.setArtifactId("missing-plugin");
        mavenProject.setGroupId("org.example");
        mavenProject.setArtifactId("example-project");
        mavenProject.setVersion("1.0");
        mavenProject.setBuild(build);

        when(session.getMavenSession()).thenReturn(mavenSession);
        when(mavenSession.clone()).thenReturn(executionSession);
        when(project.getProject()).thenReturn(mavenProject);

        DefaultPluginManager manager =
                new DefaultPluginManager(legacyPluginManager, lifecyclePluginResolver, mock(Lookup.class));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class, () -> manager.loadPlugin(session, project, model.getDelegate()));

        assertEquals(
                "Plugin org.example:missing-plugin is not declared by project " + mavenProject.getId(),
                failure.getMessage());
        verify(lifecyclePluginResolver).resolveMissingPluginVersions(mavenProject, executionSession);
        verifyNoInteractions(legacyPluginManager);
    }

    @Test
    void looksUpComponentsWithPluginClassLoaderAndRestoresOriginal() {
        ClassLoader pluginClassLoader = new ClassLoader() {};
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Object service = new Object();
        Lookup lookup = mock(Lookup.class, invocation -> {
            assertSame(pluginClassLoader, Thread.currentThread().getContextClassLoader());
            return switch (invocation.getMethod().getName()) {
                case "lookup" -> service;
                case "lookupOptional" -> Optional.of(service);
                case "lookupList" -> List.of(service);
                case "lookupMap" -> Map.of("service", service);
                default -> throw new AssertionError(invocation.getMethod());
            };
        });
        DefaultRealm realm = new DefaultRealm(pluginClassLoader, lookup);

        assertSame(service, realm.lookup().lookup(Object.class));
        assertSame(service, realm.lookup().lookup(Object.class, "service"));
        assertEquals(Optional.of(service), realm.lookup().lookupOptional(Object.class));
        assertEquals(Optional.of(service), realm.lookup().lookupOptional(Object.class, "service"));
        assertEquals(List.of(service), realm.lookup().lookupList(Object.class));
        assertEquals(Map.of("service", service), realm.lookup().lookupMap(Object.class));
        assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    void restoresOriginalClassLoaderWhenLookupFails() {
        Lookup lookup = mock(Lookup.class);
        ClassLoader pluginClassLoader = new ClassLoader() {};
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        IllegalStateException failure = new IllegalStateException("boom");

        doAnswer(invocation -> {
                    assertSame(pluginClassLoader, Thread.currentThread().getContextClassLoader());
                    throw failure;
                })
                .when(lookup)
                .lookupList(Object.class);
        DefaultRealm realm = new DefaultRealm(pluginClassLoader, lookup);

        assertSame(
                failure,
                assertThrows(IllegalStateException.class, () -> realm.lookup().lookupList(Object.class)));
        assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
    }
}
