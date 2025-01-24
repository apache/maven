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
package org.apache.maven.impl.resolver;

import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.impl.resolver.artifact.FatArtifactTraverser;
import org.apache.maven.impl.resolver.scopes.Maven4ScopeManagerConfiguration;
import org.apache.maven.impl.resolver.type.DefaultTypeProvider;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.impl.scope.InternalScopeManager;
import org.eclipse.aether.internal.impl.scope.ManagedDependencyContextRefiner;
import org.eclipse.aether.internal.impl.scope.ManagedScopeDeriver;
import org.eclipse.aether.internal.impl.scope.ManagedScopeSelector;
import org.eclipse.aether.internal.impl.scope.OptionalDependencySelector;
import org.eclipse.aether.internal.impl.scope.ScopeDependencySelector;
import org.eclipse.aether.internal.impl.scope.ScopeManagerImpl;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.manager.TransitiveDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConfigurableVersionSelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

import static java.util.Objects.requireNonNull;

/**
 * A simple {@link Supplier} of {@link SessionBuilder} instances, that on each call supplies newly
 * constructed instance. To create session out of builder, use {@link SessionBuilder#build()}. For proper closing
 * of sessions, use {@link CloseableSession#close()} method on built instance(s).
 * <p>
 * Extend this class and override methods to customize, if needed.
 *
 * @since 4.0.0
 */
public class MavenSessionBuilderSupplier implements Supplier<SessionBuilder> {
    protected final RepositorySystem repositorySystem;
    protected final InternalScopeManager scopeManager;

    public MavenSessionBuilderSupplier(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.scopeManager = new ScopeManagerImpl(Maven4ScopeManagerConfiguration.INSTANCE);
    }

    protected DependencyTraverser getDependencyTraverser() {
        return new FatArtifactTraverser();
    }

    protected InternalScopeManager getScopeManager() {
        return scopeManager;
    }

    protected DependencyManager getDependencyManager() {
        return getDependencyManager(true); // same default as in Maven4
    }

    public DependencyManager getDependencyManager(boolean transitive) {
        if (transitive) {
            return new TransitiveDependencyManager(getScopeManager());
        }
        return new ClassicDependencyManager(getScopeManager());
    }

    protected DependencySelector getDependencySelector() {
        return new AndDependencySelector(
                ScopeDependencySelector.legacy(
                        null, Arrays.asList(DependencyScope.TEST.id(), DependencyScope.PROVIDED.id())),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector());
    }

    protected DependencyGraphTransformer getDependencyGraphTransformer() {
        return new ChainedDependencyGraphTransformer(
                new ConflictResolver(
                        new ConfigurableVersionSelector(), new ManagedScopeSelector(getScopeManager()),
                        new SimpleOptionalitySelector(), new ManagedScopeDeriver(getScopeManager())),
                new ManagedDependencyContextRefiner(getScopeManager()));
    }

    /**
     * This method produces "surrogate" type registry that is static: it aims users that want to use
     * Maven-Resolver without involving Maven Core and related things.
     * <p>
     * This type registry is NOT used by Maven Core: Maven replaces it during Session creation with a type registry
     * that supports extending it (i.e. via Maven Extensions).
     * <p>
     * Important: this "static" list of types should be in-sync with core provided types.
     */
    protected ArtifactTypeRegistry getArtifactTypeRegistry() {
        DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
        new DefaultTypeProvider().types().forEach(stereotypes::add);
        return stereotypes;
    }

    protected ArtifactDescriptorPolicy getArtifactDescriptorPolicy() {
        return new SimpleArtifactDescriptorPolicy(true, true);
    }

    protected void configureSessionBuilder(SessionBuilder session) {
        session.setDependencyTraverser(getDependencyTraverser());
        session.setDependencyManager(getDependencyManager());
        session.setDependencySelector(getDependencySelector());
        session.setDependencyGraphTransformer(getDependencyGraphTransformer());
        session.setArtifactTypeRegistry(getArtifactTypeRegistry());
        session.setArtifactDescriptorPolicy(getArtifactDescriptorPolicy());
        session.setScopeManager(getScopeManager());
    }

    /**
     * Creates a new Maven-like repository system session by initializing the session with values typical for
     * Maven-based resolution. In more detail, this method configures settings relevant for the processing of dependency
     * graphs, most other settings remain at their generic default value. Use the various setters to further configure
     * the session with authentication, mirror, proxy and other information required for your environment. At least,
     * local repository manager needs to be configured to make session be able to create session instance.
     *
     * @return SessionBuilder configured with minimally required things for "Maven-based resolution". At least LRM must
     * be set on builder to make it able to create session instances.
     */
    @Override
    public SessionBuilder get() {
        requireNonNull(repositorySystem, "repositorySystem");
        SessionBuilder builder = repositorySystem.createSessionBuilder();
        configureSessionBuilder(builder);
        return builder;
    }
}
