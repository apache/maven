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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPluginPrefixResolverTest {

    @Mock
    BuildPluginManager pluginManager;

    @Mock
    RepositorySystem repositorySystem;

    @Mock
    MetadataReader metadataReader;

    DefaultPluginPrefixResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DefaultPluginPrefixResolver(pluginManager, repositorySystem, metadataReader);
    }

    private static PluginDescriptor descriptorWithPrefix(String prefix) {
        PluginDescriptor pd = new PluginDescriptor();
        pd.setGoalPrefix(prefix);
        return pd;
    }

    private static Plugin plugin(String groupId, String artifactId) {
        Plugin p = new Plugin();
        p.setGroupId(groupId);
        p.setArtifactId(artifactId);
        return p;
    }

    private static PluginPrefixRequest request(String prefix, Model model) {
        RepositorySystemSession repoSession = org.mockito.Mockito.mock(RepositorySystemSession.class);
        return new DefaultPluginPrefixRequest()
                .setPrefix(prefix)
                .setPom(model)
                .setRepositories(Collections.emptyList())
                .setRepositorySession(repoSession)
                .setPluginGroups(Collections.emptyList());
    }

    @Test
    void resolvesUsingHeuristicWithoutLoadingAllPlugins() throws Exception {
        // Given a project with many plugins and one obvious candidate by artifactId
        String wanted = "my-prefix";
        Plugin candidate = plugin("org.acme", "maven-" + wanted + "-plugin");

        List<Plugin> plugins = new ArrayList<>();
        // add non-candidates
        Plugin nc1 = plugin("org.foo", "random-plugin-1");
        Plugin nc2 = plugin("org.bar", "something-else");
        Plugin nc3 = plugin("org.baz", "another-plugin");
        plugins.add(nc1);
        plugins.add(nc2);
        plugins.add(candidate);
        plugins.add(nc3);

        Model model = new Model();
        Build build = new Build();
        build.setPlugins(plugins);
        model.setBuild(build);

        // Only the candidate plugin should be loaded
        when(pluginManager.loadPlugin(eq(candidate), anyList(), any())).thenReturn(descriptorWithPrefix(wanted));

        // When
        var result = resolver.resolve(request(wanted, model));

        // Then
        assertNotNull(result);
        assertEquals(candidate.getGroupId(), result.getGroupId());
        assertEquals(candidate.getArtifactId(), result.getArtifactId());

        // Verify only the candidate was loaded
        verify(pluginManager, times(1)).loadPlugin(eq(candidate), anyList(), any());
        verify(pluginManager, never()).loadPlugin(eq(nc1), anyList(), any());
        verify(pluginManager, never()).loadPlugin(eq(nc2), anyList(), any());
        verify(pluginManager, never()).loadPlugin(eq(nc3), anyList(), any());
    }

    @Test
    void fallsBackToFullScanForCustomGoalPrefix() throws Exception {
        // Given a plugin whose artifactId does not imply the requested prefix
        String wanted = "custom";
        Plugin odd = plugin("org.acme", "strange-artifact");

        Model model = new Model();
        Build build = new Build();
        build.setPlugins(Collections.singletonList(odd));
        model.setBuild(build);

        // Heuristic will not select this as candidate, but fallback full scan should load and match by descriptor
        when(pluginManager.loadPlugin(eq(odd), anyList(), any())).thenReturn(descriptorWithPrefix(wanted));

        // When
        var result = resolver.resolve(request(wanted, model));

        // Then
        assertNotNull(result);
        assertEquals(odd.getGroupId(), result.getGroupId());
        assertEquals(odd.getArtifactId(), result.getArtifactId());

        // Verify it was loaded exactly once via fallback
        verify(pluginManager, times(1)).loadPlugin(eq(odd), anyList(), any());
    }
}
