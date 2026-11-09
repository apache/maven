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
package org.apache.maven.its.gh13068;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.internal.DefaultPluginDependenciesResolver;
import org.apache.maven.plugin.internal.PluginDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.sisu.Priority;

/**
 * A decorating {@link PluginDependenciesResolver} that implements only the methods which existed up to
 * Maven 4.0.0-rc-5, delegating everything to the default implementation. This mirrors how IDEs embed
 * Maven, most visibly IntelliJ IDEA's {@code Maven40PluginDependenciesResolver}.
 * <p>
 * Compiled against maven-core 4.0.0-rc-5 (see this module's POM) and run against the Maven under test,
 * so the newer interface methods have no implementation here. They must therefore be resolved through
 * {@code default} methods on the interface; if they are abstract, plugin resolution dies with
 * {@code AbstractMethodError} before a single goal runs.
 */
@Named
@Singleton
@Priority(10)
public class LegacyPluginDependenciesResolver implements PluginDependenciesResolver {

    private final PluginDependenciesResolver delegate;

    @Inject
    public LegacyPluginDependenciesResolver(DefaultPluginDependenciesResolver delegate) {
        this.delegate = delegate;
        System.out.println("[gh-13068] legacy PluginDependenciesResolver installed");
    }

    @Override
    public Artifact resolve(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException {
        return delegate.resolve(plugin, repositories, session);
    }

    @Override
    public DependencyNode resolve(
            Plugin plugin,
            Artifact pluginArtifact,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        return delegate.resolve(plugin, pluginArtifact, dependencyFilter, repositories, session);
    }

    @Override
    public DependencyResult resolvePlugin(
            Plugin plugin,
            Artifact pluginArtifact,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        System.out.println("[gh-13068] resolvePlugin reached for " + plugin.getArtifactId());
        return delegate.resolvePlugin(plugin, pluginArtifact, dependencyFilter, repositories, session);
    }
}
