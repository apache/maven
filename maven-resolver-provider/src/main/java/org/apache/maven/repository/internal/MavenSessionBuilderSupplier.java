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
package org.apache.maven.repository.internal;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

import org.apache.maven.utils.Os;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
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
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConfigurableVersionSelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

import static java.util.Objects.requireNonNull;

/**
 * A simple {@link Supplier} of {@link SessionBuilder} instances, that on each call supplies newly
 * constructed instance. To create session out of builder, use {@link SessionBuilder#build()}. For proper closing
 * of sessions, use {@link CloseableSession#close()} method on built instance(s).
 * <p>
 * Extend this class and override methods to customize, if needed.
 *
 * @since 3.10.0
 */
public class MavenSessionBuilderSupplier implements Supplier<SessionBuilder> {
    protected final RepositorySystem repositorySystem;
    protected final InternalScopeManager scopeManager;

    public MavenSessionBuilderSupplier(RepositorySystem repositorySystem, InternalScopeManager scopeManager) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.scopeManager = requireNonNull(scopeManager);
    }

    protected void configureSessionBuilder(SessionBuilder session) {
        session.setSystemProperties(System.getProperties());
        boolean caseSensitive = !Os.IS_WINDOWS;
        System.getenv().forEach((key, value) -> {
            key = "env." + (caseSensitive ? key : key.toUpperCase(Locale.ENGLISH));
            session.setSystemProperty(key, value);
        });
        session.setScopeManager(scopeManager);
        session.setDependencyTraverser(getDependencyTraverser());
        session.setDependencyManager(getDependencyManager());
        session.setDependencySelector(getDependencySelector());
        session.setDependencyGraphTransformer(getDependencyGraphTransformer());
        session.setArtifactTypeRegistry(getArtifactTypeRegistry());
        session.setArtifactDescriptorPolicy(getArtifactDescriptorPolicy());
    }

    protected InternalScopeManager getScopeManager() {
        return this.scopeManager;
    }

    protected DependencyTraverser getDependencyTraverser() {
        return new FatArtifactTraverser();
    }

    protected DependencyManager getDependencyManager() {
        return new ClassicDependencyManager(getScopeManager());
    }

    protected DependencySelector getDependencySelector() {
        return new AndDependencySelector(
                ScopeDependencySelector.legacy(null, Arrays.asList(JavaScopes.TEST, JavaScopes.PROVIDED)),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector());
    }

    protected DependencyGraphTransformer getDependencyGraphTransformer() {
        return new ChainedDependencyGraphTransformer(
                new ConflictResolver(
                        new ConfigurableVersionSelector(),
                        new ManagedScopeSelector(getScopeManager()),
                        new SimpleOptionalitySelector(),
                        new ManagedScopeDeriver(getScopeManager())),
                new ManagedDependencyContextRefiner(getScopeManager()));
    }

    protected ArtifactTypeRegistry getArtifactTypeRegistry() {
        DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
        stereotypes.add(new DefaultArtifactType("pom"));
        stereotypes.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("jar", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("ejb", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("ejb-client", "jar", "client", "java"));
        stereotypes.add(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
        stereotypes.add(new DefaultArtifactType("javadoc", "jar", "javadoc", "java"));
        stereotypes.add(new DefaultArtifactType("java-source", "jar", "sources", "java", false, false));
        stereotypes.add(new DefaultArtifactType("war", "war", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("ear", "ear", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("rar", "rar", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("par", "par", "", "java", false, true));
        return stereotypes;
    }

    protected ArtifactDescriptorPolicy getArtifactDescriptorPolicy() {
        return new SimpleArtifactDescriptorPolicy(true, true);
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
        SessionBuilder builder = repositorySystem.createSessionBuilder();
        configureSessionBuilder(builder);
        return builder;
    }
}
