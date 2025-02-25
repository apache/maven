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
package org.apache.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.impl.resolver.artifact.MavenArtifactProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

/**
 * <strong>Warning:</strong> This is an internal utility class that is only public for technical reasons, it is not part
 * of the public API. In particular, this class can be changed or deleted without prior notice.
 *
 */
public class RepositoryUtils {

    @Nullable
    private static String nullify(@Nullable String string) {
        return (string == null || string.isEmpty()) ? null : string;
    }

    @Nullable
    public static org.apache.maven.artifact.Artifact toArtifact(@Nullable Dependency dependency) {
        if (dependency == null) {
            return null;
        }

        org.apache.maven.artifact.Artifact result = toArtifact(dependency.getArtifact());
        result.setScope(dependency.getScope());
        result.setOptional(dependency.isOptional());

        return result;
    }

    @Nullable
    public static org.apache.maven.artifact.Artifact toArtifact(@Nullable Artifact artifact) {
        if (artifact == null) {
            return null;
        }

        ArtifactHandler handler = newHandler(artifact);

        /*
         * NOTE: From Artifact.hasClassifier(), an empty string and a null both denote "no classifier". However, some
         * plugins only check for null, so be sure to nullify an empty classifier.
         */
        org.apache.maven.artifact.Artifact result = new org.apache.maven.artifact.DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                null,
                artifact.getProperty(ArtifactProperties.TYPE, artifact.getExtension()),
                nullify(artifact.getClassifier()),
                handler);

        result.setFile(artifact.getFile());
        result.setResolved(artifact.getFile() != null);

        List<String> trail = new ArrayList<>(1);
        trail.add(result.getId());
        result.setDependencyTrail(trail);

        return result;
    }

    public static void toArtifacts(
            @Nonnull Collection<org.apache.maven.artifact.Artifact> artifacts,
            @Nonnull Collection<? extends DependencyNode> nodes,
            @Nonnull List<String> trail,
            @Nullable DependencyFilter filter) {
        for (DependencyNode node : nodes) {
            org.apache.maven.artifact.Artifact artifact = toArtifact(node.getDependency());

            List<String> nodeTrail = new ArrayList<>(trail.size() + 1);
            nodeTrail.addAll(trail);
            nodeTrail.add(artifact.getId());

            if (filter == null || filter.accept(node, Collections.emptyList())) {
                artifact.setDependencyTrail(nodeTrail);
                artifacts.add(artifact);
            }

            toArtifacts(artifacts, node.getChildren(), nodeTrail, filter);
        }
    }

    @Nullable
    public static Artifact toArtifact(@Nullable org.apache.maven.artifact.Artifact artifact) {
        if (artifact == null) {
            return null;
        }

        String version = artifact.getVersion();
        if (version == null && artifact.getVersionRange() != null) {
            version = artifact.getVersionRange().toString();
        }

        Map<String, String> props = null;
        if (org.apache.maven.artifact.Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
            String localPath = (artifact.getFile() != null) ? artifact.getFile().getPath() : "";
            props = Collections.singletonMap(MavenArtifactProperties.LOCAL_PATH, localPath);
        }

        Artifact result = new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getArtifactHandler().getExtension(),
                version,
                props,
                newArtifactType(artifact.getType(), artifact.getArtifactHandler()));
        result = result.setFile(artifact.getFile());

        return result;
    }

    public static Dependency toDependency(
            @Nullable org.apache.maven.artifact.Artifact artifact,
            @Nullable Collection<org.apache.maven.model.Exclusion> exclusions) {
        if (artifact == null) {
            return null;
        }

        Artifact result = toArtifact(artifact);

        List<Exclusion> excl = Optional.ofNullable(exclusions).orElse(Collections.emptyList()).stream()
                .map(RepositoryUtils::toExclusion)
                .collect(Collectors.toList());
        return new Dependency(result, artifact.getScope(), artifact.isOptional(), excl);
    }

    @Nonnull
    public static List<RemoteRepository> toRepos(@Nullable List<ArtifactRepository> repos) {
        return Optional.ofNullable(repos).orElse(Collections.emptyList()).stream()
                .map(RepositoryUtils::toRepo)
                .collect(Collectors.toList());
    }

    @Nullable
    public static RemoteRepository toRepo(@Nullable ArtifactRepository repo) {
        RemoteRepository result = null;
        if (repo != null) {
            RemoteRepository.Builder builder =
                    new RemoteRepository.Builder(repo.getId(), getLayout(repo), repo.getUrl());
            builder.setSnapshotPolicy(toPolicy(repo.getSnapshots()));
            builder.setReleasePolicy(toPolicy(repo.getReleases()));
            builder.setAuthentication(toAuthentication(repo.getAuthentication()));
            builder.setProxy(toProxy(repo.getProxy()));
            builder.setMirroredRepositories(toRepos(repo.getMirroredRepositories()));
            builder.setBlocked(repo.isBlocked());
            result = builder.build();
        }
        return result;
    }

    @Nonnull
    public static String getLayout(@Nonnull ArtifactRepository repo) {
        try {
            return repo.getLayout().getId();
        } catch (LinkageError e) {
            /*
             * NOTE: getId() was added in 3.x and is as such not implemented by plugins compiled against 2.x APIs.
             */
            String className = repo.getLayout().getClass().getSimpleName();
            if (className.endsWith("RepositoryLayout")) {
                String layout = className.substring(0, className.length() - "RepositoryLayout".length());
                if (!layout.isEmpty()) {
                    layout = Character.toLowerCase(layout.charAt(0)) + layout.substring(1);
                    return layout;
                }
            }
            return "";
        }
    }

    @Nullable
    private static RepositoryPolicy toPolicy(@Nullable ArtifactRepositoryPolicy policy) {
        RepositoryPolicy result = null;
        if (policy != null) {
            result = new RepositoryPolicy(policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy());
        }
        return result;
    }

    @Nullable
    private static Authentication toAuthentication(@Nullable org.apache.maven.artifact.repository.Authentication auth) {
        Authentication result = null;
        if (auth != null) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(auth.getUsername()).addPassword(auth.getPassword());
            authBuilder.addPrivateKey(auth.getPrivateKey(), auth.getPassphrase());
            result = authBuilder.build();
        }
        return result;
    }

    @Nullable
    private static Proxy toProxy(@Nullable org.apache.maven.repository.Proxy proxy) {
        Proxy result = null;
        if (proxy != null) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUserName()).addPassword(proxy.getPassword());
            result = new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build());
        }
        return result;
    }

    @Nonnull
    public static ArtifactHandler newHandler(@Nonnull Artifact artifact) {
        String type = artifact.getProperty(ArtifactProperties.TYPE, artifact.getExtension());
        return new DefaultArtifactHandler(
                type,
                artifact.getExtension(),
                null,
                null,
                null,
                Boolean.parseBoolean(artifact.getProperty(MavenArtifactProperties.INCLUDES_DEPENDENCIES, "")),
                artifact.getProperty(ArtifactProperties.LANGUAGE, null),
                Boolean.parseBoolean(artifact.getProperty(MavenArtifactProperties.CONSTITUTES_BUILD_PATH, "")));
    }

    @Nonnull
    public static ArtifactType newArtifactType(@Nonnull String id, @Nonnull ArtifactHandler handler) {
        return new DefaultArtifactType(
                id,
                handler.getExtension(),
                handler.getClassifier(),
                handler.getLanguage(),
                handler.isAddedToClasspath(),
                handler.isIncludesDependencies());
    }

    @Nonnull
    public static Dependency toDependency(
            @Nonnull org.apache.maven.model.Dependency dependency, @Nonnull ArtifactTypeRegistry stereotypes) {
        ArtifactType stereotype = stereotypes.get(dependency.getType());
        if (stereotype == null) {
            stereotype = new DefaultArtifactType(dependency.getType());
        }

        boolean system =
                dependency.getSystemPath() != null && dependency.getSystemPath().length() > 0;

        Map<String, String> props = null;
        if (system) {
            props = Collections.singletonMap(MavenArtifactProperties.LOCAL_PATH, dependency.getSystemPath());
        }

        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                null,
                dependency.getVersion(),
                props,
                stereotype);

        List<Exclusion> exclusions = dependency.getExclusions().stream()
                .map(RepositoryUtils::toExclusion)
                .collect(Collectors.toList());

        return new Dependency(
                artifact,
                dependency.getScope(),
                dependency.getOptional() != null ? dependency.isOptional() : null,
                exclusions);
    }

    @Nonnull
    public static Exclusion toExclusion(@Nonnull org.apache.maven.model.Exclusion exclusion) {
        return new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*");
    }

    @Nonnull
    public static ArtifactTypeRegistry newArtifactTypeRegistry(@Nonnull ArtifactHandlerManager handlerManager) {
        return new MavenArtifactTypeRegistry(handlerManager);
    }

    static class MavenArtifactTypeRegistry implements ArtifactTypeRegistry {

        @Nonnull
        private final ArtifactHandlerManager handlerManager;

        MavenArtifactTypeRegistry(@Nonnull ArtifactHandlerManager handlerManager) {
            this.handlerManager = handlerManager;
        }

        @Nullable
        @Override
        public ArtifactType get(@Nonnull String stereotypeId) {
            ArtifactHandler handler = handlerManager.getArtifactHandler(stereotypeId);
            return newArtifactType(stereotypeId, handler);
        }
    }

    @Nonnull
    public static Collection<Artifact> toArtifacts(
            @Nonnull Collection<org.apache.maven.artifact.Artifact> artifactsToConvert) {
        return artifactsToConvert.stream().map(RepositoryUtils::toArtifact).collect(Collectors.toList());
    }

    @Nullable
    public static WorkspaceRepository getWorkspace(@Nonnull RepositorySystemSession session) {
        WorkspaceReader reader = session.getWorkspaceReader();
        return (reader != null) ? reader.getRepository() : null;
    }

    public static boolean repositoriesEquals(@Nonnull List<RemoteRepository> r1, @Nonnull List<RemoteRepository> r2) {
        if (r1.size() != r2.size()) {
            return false;
        }

        for (Iterator<RemoteRepository> it1 = r1.iterator(), it2 = r2.iterator(); it1.hasNext(); ) {
            if (!repositoryEquals(it1.next(), it2.next())) {
                return false;
            }
        }

        return true;
    }

    public static int repositoriesHashCode(@Nonnull List<RemoteRepository> repositories) {
        int result = 17;
        for (RemoteRepository repository : repositories) {
            result = 31 * result + repositoryHashCode(repository);
        }
        return result;
    }

    @Nullable
    public static RepositorySystemSession overlay(
            @Nullable ArtifactRepository repository,
            @Nullable RepositorySystemSession session,
            @Nonnull RepositorySystem system) {
        if (repository == null || repository.getBasedir() == null) {
            return session;
        }

        DefaultRepositorySystemSession newSession;
        if (session != null) {
            LocalRepositoryManager lrm = session.getLocalRepositoryManager();
            if (lrm != null && lrm.getRepository().getBasedir().equals(new File(repository.getBasedir()))) {
                return session;
            }
            newSession = new DefaultRepositorySystemSession(session);
        } else {
            newSession = new DefaultRepositorySystemSession(h -> false); // no close handle used
        }

        final LocalRepositoryManager llrm =
                system.newLocalRepositoryManager(newSession, new LocalRepository(repository.getBasedir()));
        newSession.setLocalRepositoryManager(llrm);
        return newSession;
    }

    private static int repositoryHashCode(@Nonnull RemoteRepository repository) {
        int result = 17;
        Object obj = repository.getUrl();
        result = 31 * result + (obj != null ? obj.hashCode() : 0);
        return result;
    }

    private static boolean policyEquals(@Nonnull RepositoryPolicy p1, @Nonnull RepositoryPolicy p2) {
        if (p1 == p2) {
            return true;
        }
        // update policy doesn't affect contents
        return p1.isEnabled() == p2.isEnabled() && Objects.equals(p1.getChecksumPolicy(), p2.getChecksumPolicy());
    }

    private static boolean repositoryEquals(@Nonnull RemoteRepository r1, @Nonnull RemoteRepository r2) {
        if (r1 == r2) {
            return true;
        }

        return Objects.equals(r1.getId(), r2.getId())
                && Objects.equals(r1.getUrl(), r2.getUrl())
                && policyEquals(r1.getPolicy(false), r2.getPolicy(false))
                && policyEquals(r1.getPolicy(true), r2.getPolicy(true));
    }
}
