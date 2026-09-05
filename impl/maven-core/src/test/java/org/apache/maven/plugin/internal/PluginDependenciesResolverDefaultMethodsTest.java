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

import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PluginDependenciesResolver} is documented as internal, but tools that embed Maven — most visibly
 * IntelliJ IDEA's {@code Maven40PluginDependenciesResolver} — implement it out of tree and are compiled against
 * one Maven version while running against another. When {@code resolveCoreExtensionAndFlatten} and
 * {@code resolvePluginAndFlatten} were forward-ported from Maven 3.10.0 as <em>abstract</em> methods, every such
 * implementation started failing with {@code AbstractMethodError} as soon as core invoked them.
 *
 * <p>These tests pin the compatibility contract: an implementation providing only the methods that existed before
 * the forward port must remain a legal implementation, and the two newer methods must stay {@code default}.
 */
class PluginDependenciesResolverDefaultMethodsTest {

    private static final DependencyResult RESULT = new DependencyResult(new DependencyRequest());

    /**
     * Implements exactly the method set of the pre-forward-port interface — nothing more. This class failing to
     * compile <em>is</em> the regression: it means the two newer methods went back to being abstract.
     */
    private static final class LegacyResolver implements PluginDependenciesResolver {

        private Plugin plugin;
        private Artifact pluginArtifact;
        private DependencyFilter dependencyFilter;
        private List<RemoteRepository> repositories;
        private RepositorySystemSession session;

        @Override
        public Artifact resolve(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.eclipse.aether.graph.DependencyNode resolve(
                Plugin plugin,
                Artifact pluginArtifact,
                DependencyFilter dependencyFilter,
                List<RemoteRepository> repositories,
                RepositorySystemSession session) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DependencyResult resolvePlugin(
                Plugin plugin,
                Artifact pluginArtifact,
                DependencyFilter dependencyFilter,
                List<RemoteRepository> repositories,
                RepositorySystemSession session) {
            this.plugin = plugin;
            this.pluginArtifact = pluginArtifact;
            this.dependencyFilter = dependencyFilter;
            this.repositories = repositories;
            this.session = session;
            return RESULT;
        }
    }

    @Test
    void resolvePluginAndFlattenDelegatesToResolvePlugin() throws PluginResolutionException {
        LegacyResolver resolver = new LegacyResolver();
        Plugin plugin = new Plugin();
        Artifact artifact = new DefaultArtifact("g:a:1.0");
        DependencyFilter filter = (node, parents) -> true;
        List<RemoteRepository> repositories = List.of();

        assertSame(RESULT, resolver.resolvePluginAndFlatten(plugin, artifact, filter, repositories, null));

        assertSame(plugin, resolver.plugin);
        assertSame(artifact, resolver.pluginArtifact);
        assertSame(filter, resolver.dependencyFilter);
        assertSame(repositories, resolver.repositories);
        assertNull(resolver.session);
    }

    @Test
    void resolveCoreExtensionAndFlattenDelegatesToResolvePlugin() throws PluginResolutionException {
        LegacyResolver resolver = new LegacyResolver();
        Plugin plugin = new Plugin();
        DependencyFilter filter = (node, parents) -> true;
        List<RemoteRepository> repositories = List.of();

        assertSame(RESULT, resolver.resolveCoreExtensionAndFlatten(plugin, filter, repositories, null));

        assertSame(plugin, resolver.plugin);
        assertNull(resolver.pluginArtifact, "the extension's main artifact is resolved from the plugin GAV");
        assertSame(filter, resolver.dependencyFilter);
        assertSame(repositories, resolver.repositories);
    }

    /**
     * Guards the property that actually broke IntelliJ IDEA: these methods are invoked on implementations compiled
     * against an older Maven, so they must carry an implementation in the interface itself. Turning either back into
     * an abstract method reintroduces {@code AbstractMethodError} for every out-of-tree implementation.
     */
    @Test
    void newerMethodsAreDefaultMethods() throws NoSuchMethodException {
        Method resolveCoreExtensionAndFlatten = PluginDependenciesResolver.class.getMethod(
                "resolveCoreExtensionAndFlatten",
                Plugin.class,
                DependencyFilter.class,
                List.class,
                RepositorySystemSession.class);
        Method resolvePluginAndFlatten = PluginDependenciesResolver.class.getMethod(
                "resolvePluginAndFlatten",
                Plugin.class,
                Artifact.class,
                DependencyFilter.class,
                List.class,
                RepositorySystemSession.class);

        assertTrue(
                resolveCoreExtensionAndFlatten.isDefault(),
                "resolveCoreExtensionAndFlatten must stay a default method");
        assertTrue(resolvePluginAndFlatten.isDefault(), "resolvePluginAndFlatten must stay a default method");
    }
}
