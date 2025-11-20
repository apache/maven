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
package org.apache.maven.cli.internal;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.services.ArtifactCoordinatesFactory;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.VersionRangeResolver;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.impl.DefaultArtifactCoordinatesFactory;
import org.apache.maven.impl.DefaultArtifactResolver;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultRepositoryFactory;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.impl.DefaultVersionRangeResolver;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.internal.impl.DefaultArtifactManager;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.internal.DefaultPluginDependenciesResolver;
import org.apache.maven.resolver.MavenChainedWorkspaceReader;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryKeyFunctionFactory;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.sisu.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BootstrapCoreExtensionManager
 */
@Deprecated
@Named
public class BootstrapCoreExtensionManager {
    public static final String STRATEGY_PARENT_FIRST = "parent-first";
    public static final String STRATEGY_PLUGIN = "plugin";
    public static final String STRATEGY_SELF_FIRST = "self-first";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DefaultPluginDependenciesResolver pluginDependenciesResolver;

    private final RepositorySystemSessionFactory repositorySystemSessionFactory;

    private final CoreExports coreExports;

    private final ClassWorld classWorld;

    private final ClassRealm parentRealm;

    private final WorkspaceReader ideWorkspaceReader;

    private final RepositorySystem repoSystem;

    @Inject
    public BootstrapCoreExtensionManager(
            DefaultPluginDependenciesResolver pluginDependenciesResolver,
            RepositorySystemSessionFactory repositorySystemSessionFactory,
            CoreExports coreExports,
            PlexusContainer container,
            @Nullable @Named("ide") WorkspaceReader ideWorkspaceReader,
            RepositorySystem repoSystem) {
        this.pluginDependenciesResolver = pluginDependenciesResolver;
        this.repositorySystemSessionFactory = repositorySystemSessionFactory;
        this.coreExports = coreExports;
        this.classWorld = ((DefaultPlexusContainer) container).getClassWorld();
        this.parentRealm = container.getContainerRealm();
        this.ideWorkspaceReader = ideWorkspaceReader;
        this.repoSystem = repoSystem;
    }

    public List<CoreExtensionEntry> loadCoreExtensions(
            MavenExecutionRequest request, Set<String> providedArtifacts, List<CoreExtension> extensions)
            throws Exception {
        try (CloseableSession repoSession = repositorySystemSessionFactory
                .newRepositorySessionBuilder(request)
                .setWorkspaceReader(new MavenChainedWorkspaceReader(request.getWorkspaceReader(), ideWorkspaceReader))
                .build()) {
            MavenSession mSession = new MavenSession(repoSession, request, new DefaultMavenExecutionResult());
            InternalSession iSession = new SimpleSession(mSession, repoSystem, null);
            InternalSession.associate(repoSession, iSession);

            List<RemoteRepository> repositories = RepositoryUtils.toRepos(request.getPluginArtifactRepositories());
            UnaryOperator<String> interpolator = createInterpolator(request);

            return resolveCoreExtensions(repoSession, repositories, providedArtifacts, extensions, interpolator);
        }
    }

    private List<CoreExtensionEntry> resolveCoreExtensions(
            RepositorySystemSession repoSession,
            List<RemoteRepository> repositories,
            Set<String> providedArtifacts,
            List<CoreExtension> configuration,
            UnaryOperator<String> interpolator)
            throws Exception {
        List<CoreExtensionEntry> extensions = new ArrayList<>();

        DependencyFilter dependencyFilter = new ExclusionsDependencyFilter(providedArtifacts);

        for (CoreExtension extension : configuration) {
            List<Artifact> artifacts =
                    resolveExtension(extension, repoSession, repositories, dependencyFilter, interpolator);
            if (!artifacts.isEmpty()) {
                extensions.add(createExtension(extension, artifacts));
            }
        }

        return Collections.unmodifiableList(extensions);
    }

    private CoreExtensionEntry createExtension(CoreExtension extension, List<Artifact> artifacts) throws Exception {
        String realmId = "coreExtension>" + extension.getGroupId() + ":" + extension.getArtifactId() + ":"
                + extension.getVersion();
        final ClassRealm realm = classWorld.newRealm(realmId, null);
        Set<String> providedArtifacts = Collections.emptySet();
        String classLoadingStrategy = extension.getClassLoadingStrategy();
        if (STRATEGY_PARENT_FIRST.equals(classLoadingStrategy)) {
            realm.importFrom(parentRealm, "");
        } else if (STRATEGY_PLUGIN.equals(classLoadingStrategy)) {
            coreExports.getExportedPackages().forEach((p, cl) -> realm.importFrom(cl, p));
            providedArtifacts = coreExports.getExportedArtifacts();
        } else if (STRATEGY_SELF_FIRST.equals(classLoadingStrategy)) {
            realm.setParentRealm(parentRealm);
        } else {
            throw new IllegalArgumentException("Unsupported class-loading strategy '"
                    + classLoadingStrategy + "'. Supported values are: " + STRATEGY_PARENT_FIRST
                    + ", " + STRATEGY_PLUGIN + " and " + STRATEGY_SELF_FIRST);
        }
        log.debug("Populating class realm {}", realm.getId());
        for (Artifact artifact : artifacts) {
            String id = artifact.getGroupId() + ":" + artifact.getArtifactId();
            if (providedArtifacts.contains(id)) {
                log.debug("  Excluded {}", id);
            } else {
                File file = artifact.getFile();
                log.debug("  Included {} located at {}", id, file);
                realm.addURL(file.toURI().toURL());
            }
        }
        return CoreExtensionEntry.discoverFrom(
                realm,
                Collections.singleton(artifacts.get(0).getFile()),
                extension.getGroupId() + ":" + extension.getArtifactId(),
                extension.getConfiguration());
    }

    private List<Artifact> resolveExtension(
            CoreExtension extension,
            RepositorySystemSession repoSession,
            List<RemoteRepository> repositories,
            DependencyFilter dependencyFilter,
            UnaryOperator<String> interpolator)
            throws ExtensionResolutionException {
        try {
            /* TODO: Enhance the PluginDependenciesResolver to provide a
             * resolveCoreExtension method which uses a CoreExtension
             * object instead of a Plugin as this makes no sense.
             */
            Plugin plugin = Plugin.newBuilder()
                    .groupId(interpolator.apply(extension.getGroupId()))
                    .artifactId(interpolator.apply(extension.getArtifactId()))
                    .version(interpolator.apply(extension.getVersion()))
                    .build();

            DependencyResult result = pluginDependenciesResolver.resolveCoreExtension(
                    new org.apache.maven.model.Plugin(plugin), dependencyFilter, repositories, repoSession);
            return result.getArtifactResults().stream()
                    .filter(ArtifactResult::isResolved)
                    .map(ArtifactResult::getArtifact)
                    .collect(Collectors.toList());
        } catch (PluginResolutionException | InterpolatorException e) {
            throw new ExtensionResolutionException(extension, e);
        }
    }

    private static UnaryOperator<String> createInterpolator(MavenExecutionRequest request) {
        Interpolator interpolator = new DefaultInterpolator();
        UnaryOperator<String> callback = v -> {
            String r = request.getUserProperties().getProperty(v);
            if (r == null) {
                r = request.getSystemProperties().getProperty(v);
            }
            return r != null ? r : v;
        };
        return v -> interpolator.interpolate(v, callback);
    }

    static class SimpleSession extends DefaultSession {
        SimpleSession(
                MavenSession session,
                RepositorySystem repositorySystem,
                List<org.apache.maven.api.RemoteRepository> repositories) {
            super(session, repositorySystem, repositories, null, null, null);
        }

        @Override
        protected Session newSession(
                MavenSession mavenSession, List<org.apache.maven.api.RemoteRepository> repositories) {
            return new SimpleSession(mavenSession, getRepositorySystem(), repositories);
        }

        @Override
        public <T extends Service> T getService(Class<T> clazz) throws NoSuchElementException {
            if (clazz == ArtifactCoordinatesFactory.class) {
                return (T) new DefaultArtifactCoordinatesFactory();
            } else if (clazz == VersionParser.class) {
                return (T) new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));
            } else if (clazz == VersionRangeResolver.class) {
                return (T) new DefaultVersionRangeResolver(repositorySystem);
            } else if (clazz == ArtifactResolver.class) {
                return (T) new DefaultArtifactResolver();
            } else if (clazz == ArtifactManager.class) {
                return (T) new DefaultArtifactManager(this);
            } else if (clazz == RepositoryFactory.class) {
                return (T) new DefaultRepositoryFactory(new DefaultRemoteRepositoryManager(
                        new DefaultUpdatePolicyAnalyzer(),
                        new DefaultChecksumPolicyProvider(),
                        new DefaultRepositoryKeyFunctionFactory()));
            } else if (clazz == Interpolator.class) {
                return (T) new DefaultInterpolator();
                // } else if (clazz == ModelResolver.class) {
                //    return (T) new DefaultModelResolver();
            }
            throw new NoSuchElementException("No service for " + clazz.getName());
        }
    }
}
