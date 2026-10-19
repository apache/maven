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
        assertSame(realm, result.getClassLoader());
        assertEquals(List.of(dependency), result.getDependencies().stream().toList());
        assertSame(dependency, result.getDependenciesMap().get("org.example:plugin-spi"));
        verify(executionSession).setCurrentProject(mavenProject);
        verify(lifecyclePluginResolver).resolveMissingPluginVersions(mavenProject, executionSession);
        verify(legacyPluginManager).setupPluginRealm(descriptor, executionSession, null, null, null);
    }

    @Test
    void looksUpComponentsWithPluginClassLoaderAndRestoresOriginal() {
        MavenPluginManager legacyPluginManager = mock(MavenPluginManager.class);
        LifecyclePluginResolver lifecyclePluginResolver = mock(LifecyclePluginResolver.class);
        Lookup lookup = mock(Lookup.class);
        DefaultPluginManager manager = new DefaultPluginManager(legacyPluginManager, lifecyclePluginResolver, lookup);
        Plugin plugin = mock(Plugin.class);
        ClassLoader pluginClassLoader = new ClassLoader() {};
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Object service = new Object();

        when(plugin.getClassLoader()).thenReturn(pluginClassLoader);
        when(lookup.lookupList(Object.class)).thenAnswer(invocation -> {
            assertSame(pluginClassLoader, Thread.currentThread().getContextClassLoader());
            return List.of(service);
        });

        List<Object> result = manager.lookupList(plugin, Object.class);

        assertEquals(List.of(service), result);
        assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()));
    }

    @Test
    void restoresOriginalClassLoaderWhenLookupFails() {
        MavenPluginManager legacyPluginManager = mock(MavenPluginManager.class);
        LifecyclePluginResolver lifecyclePluginResolver = mock(LifecyclePluginResolver.class);
        Lookup lookup = mock(Lookup.class);
        DefaultPluginManager manager = new DefaultPluginManager(legacyPluginManager, lifecyclePluginResolver, lookup);
        Plugin plugin = mock(Plugin.class);
        ClassLoader pluginClassLoader = new ClassLoader() {};
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        IllegalStateException failure = new IllegalStateException("boom");

        when(plugin.getClassLoader()).thenReturn(pluginClassLoader);
        doAnswer(invocation -> {
                    assertSame(pluginClassLoader, Thread.currentThread().getContextClassLoader());
                    throw failure;
                })
                .when(lookup)
                .lookupList(Object.class);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> manager.lookupList(plugin, Object.class)));
        assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
    }
}
