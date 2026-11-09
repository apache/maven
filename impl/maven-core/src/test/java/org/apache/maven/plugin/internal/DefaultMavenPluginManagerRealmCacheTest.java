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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.DefaultPluginRealmCache;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A cached plugin realm must keep the resolver dependency node. The second execution otherwise
 * cannot read plugin dependencies.
 */
class DefaultMavenPluginManagerRealmCacheTest {

    @Test
    void cachedRealmRestoresDependencyNode() throws Exception {
        ClassRealm realm = mock(ClassRealm.class);
        ClassRealm apiRealm = mock(ClassRealm.class);
        DependencyNode node = mock(DependencyNode.class);
        DependencyResult result = mock(DependencyResult.class);
        when(result.getRoot()).thenReturn(node);
        when(result.getDependencyNodeResults()).thenReturn(List.of());

        PluginDependenciesResolver resolver = mock(PluginDependenciesResolver.class);
        when(resolver.resolvePluginAndFlatten(any(), any(), any(), any(), any()))
                .thenReturn(result);

        ClassRealmManager classRealmManager = mock(ClassRealmManager.class);
        when(classRealmManager.getMavenApiRealm()).thenReturn(apiRealm);
        when(classRealmManager.createPluginRealm(any(), any(), any(), any(), any()))
                .thenReturn(realm);

        DefaultMavenPluginManager manager = new DefaultMavenPluginManager(
                mock(DefaultPlexusContainer.class),
                classRealmManager,
                mock(PluginDescriptorCache.class),
                new DefaultPluginRealmCache(),
                resolver,
                mock(org.apache.maven.plugin.ExtensionRealmCache.class),
                mock(PluginVersionResolver.class),
                mock(PluginArtifactsCache.class),
                mock(MavenPluginValidator.class),
                List.of(),
                mock(PluginValidationManager.class),
                List.of());

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.example");
        plugin.setArtifactId("example-plugin");
        plugin.setVersion("1.0");
        plugin.setDependencies(new ArrayList<>());

        MavenProject project = new MavenProject();
        MavenSession session = mock(MavenSession.class);
        when(session.getCurrentProject()).thenReturn(project);
        when(session.getRepositorySession()).thenReturn(mock(RepositorySystemSession.class));

        PluginDescriptor first = descriptor(plugin);
        manager.setupPluginRealm(first, session, null, null, null);
        assertSame(node, first.getDependencyNode());

        PluginDescriptor second = descriptor(plugin);
        assertNull(second.getDependencyNode());
        manager.setupPluginRealm(second, session, null, null, null);

        verify(resolver, times(1)).resolvePluginAndFlatten(any(), any(), any(), any(), any());
        assertSame(node, second.getDependencyNode());
    }

    private static PluginDescriptor descriptor(Plugin plugin) {
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setPlugin(plugin);
        descriptor.setPluginArtifact(pluginArtifact());
        descriptor.setComponents(new ArrayList<>());
        return descriptor;
    }

    private static Artifact pluginArtifact() {
        ArtifactHandler handler = mock(ArtifactHandler.class);
        when(handler.getExtension()).thenReturn("jar");
        when(handler.getClassifier()).thenReturn("");
        when(handler.getLanguage()).thenReturn("java");
        Artifact artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn("org.example");
        when(artifact.getArtifactId()).thenReturn("example-plugin");
        when(artifact.getVersion()).thenReturn("1.0");
        when(artifact.getType()).thenReturn("jar");
        when(artifact.getArtifactHandler()).thenReturn(handler);
        return artifact;
    }
}
