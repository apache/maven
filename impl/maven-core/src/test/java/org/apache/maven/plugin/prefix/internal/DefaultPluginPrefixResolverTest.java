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
package org.apache.maven.plugin.prefix.internal;

import java.util.List;

import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPluginPrefixResolverTest {
    @Mock
    private BuildPluginManager pluginManager;

    @Mock
    private RepositorySystem repositorySystem;

    @Mock
    private MetadataReader metadataReader;

    @Mock
    private PluginVersionResolver pluginVersionResolver;

    @Mock
    private RepositorySystemSession repositorySession;

    @Mock
    private PluginVersionResult pluginVersionResult;

    @Test
    void resolvesVersionOnlyForMatchingProjectPlugin() throws Exception {
        Plugin matchingPlugin = plugin("com.example", "custom-maven-plugin");
        Plugin unrelatedPlugin = plugin("org.apache.maven.plugins", "maven-release-plugin");

        Build build = new Build();
        build.setPlugins(List.of(unrelatedPlugin, matchingPlugin));
        Model model = new Model();
        model.setBuild(build);

        when(pluginVersionResult.getVersion()).thenReturn("1.0");
        when(pluginVersionResolver.resolve(any())).thenReturn(pluginVersionResult);

        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setGroupId(matchingPlugin.getGroupId());
        descriptor.setArtifactId(matchingPlugin.getArtifactId());
        descriptor.setVersion("1.0");
        descriptor.setGoalPrefix("custom");
        when(pluginManager.loadPlugin(
                        argThat(plugin -> matchingPlugin.getArtifactId().equals(plugin.getArtifactId())
                                && "1.0".equals(plugin.getVersion())),
                        eq(List.of()),
                        eq(repositorySession)))
                .thenReturn(descriptor);

        DefaultPluginPrefixResolver resolver =
                new DefaultPluginPrefixResolver(pluginManager, repositorySystem, metadataReader, pluginVersionResolver);
        PluginPrefixResult result = resolver.resolve(new DefaultPluginPrefixRequest()
                .setPrefix("custom")
                .setPom(model)
                .setRepositorySession(repositorySession));

        assertEquals(matchingPlugin.getGroupId(), result.getGroupId());
        assertEquals(matchingPlugin.getArtifactId(), result.getArtifactId());

        verify(pluginVersionResolver)
                .resolve(argThat(request ->
                        matchingPlugin.getArtifactId().equals(request.getArtifactId()) && request.getPom() == model));
        verify(pluginVersionResolver, never())
                .resolve(argThat(request -> unrelatedPlugin.getArtifactId().equals(request.getArtifactId())));
        verify(pluginManager, never())
                .loadPlugin(
                        argThat(plugin -> unrelatedPlugin.getArtifactId().equals(plugin.getArtifactId())),
                        any(),
                        eq(repositorySession));
        verifyNoInteractions(repositorySystem);
    }

    private Plugin plugin(String groupId, String artifactId) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        return plugin;
    }
}
