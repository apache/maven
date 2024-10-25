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
package org.apache.maven.project.artifact;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Named("maven")
@Singleton
@Deprecated
public class MavenMetadataSource implements ArtifactMetadataSource {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RepositoryMetadataManager repositoryMetadataManager;
    private final ArtifactFactory artifactFactory;
    private final ProjectBuilder projectBuilder;
    private final MavenMetadataCache cache;
    private final LegacySupport legacySupport;

    private MavenRepositorySystem mavenRepositorySystem;

    @Inject
    public MavenMetadataSource(
            RepositoryMetadataManager repositoryMetadataManager,
            ArtifactFactory artifactFactory,
            ProjectBuilder projectBuilder,
            MavenMetadataCache cache,
            LegacySupport legacySupport,
            MavenRepositorySystem mavenRepositorySystem) {
        this.repositoryMetadataManager = repositoryMetadataManager;
        this.artifactFactory = artifactFactory;
        this.projectBuilder = projectBuilder;
        this.cache = cache;
        this.legacySupport = legacySupport;
        this.mavenRepositorySystem = mavenRepositorySystem;
    }

    private void injectSession(MetadataResolutionRequest request) {
        RepositorySystemSession session = legacySupport.getRepositorySession();

        if (session != null) {
            request.setOffline(session.isOffline());
            request.setForceUpdate(RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(session.getUpdatePolicy()));
        }
    }

    @Override
    public ResolutionGroup retrieve(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException {
        return retrieve(artifact, localRepository, remoteRepositories, false);
    }

    public ResolutionGroup retrieve(
            Artifact artifact,
            ArtifactRepository localRepository,
            List<ArtifactRepository> remoteRepositories,
            boolean resolveManagedVersions)
            throws ArtifactMetadataRetrievalException {
        MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        injectSession(request);
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        request.setResolveManagedVersions(resolveManagedVersions);
        return retrieve(request);
    }

    @Override
    public ResolutionGroup retrieve(MetadataResolutionRequest request) throws ArtifactMetadataRetrievalException {
        Artifact artifact = request.getArtifact();

        //
        // If we have a system scoped artifact then we do not want any searching in local or remote repositories
        // and we want artifact resolution to only return the system scoped artifact itself.
        //
        if (artifact.getScope() != null && artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
            return new ResolutionGroup(null, null, null);
        }

        ResolutionGroup cached = cache.get(
                artifact,
                request.isResolveManagedVersions(),
                request.getLocalRepository(),
                request.getRemoteRepositories());

        if (cached != null
                // if the POM has no file, we cached a missing artifact, only return the cached data if no update forced
                && (!request.isForceUpdate() || hasFile(cached.getPomArtifact()))) {
            return cached;
        }

        List<Dependency> dependencies;

        List<Dependency> managedDependencies = null;

        List<ArtifactRepository> pomRepositories = null;

        Artifact pomArtifact;

        Artifact relocatedArtifact = null;

        // TODO hack: don't rebuild model if it was already loaded during reactor resolution
        RepositorySystemSession repositorySession = legacySupport.getRepositorySession();
        final WorkspaceReader workspace = repositorySession.getWorkspaceReader();
        Model model;
        if (workspace instanceof MavenWorkspaceReader) {
            model = ((MavenWorkspaceReader) workspace).findModel(RepositoryUtils.toArtifact(artifact));
        } else {
            model = null;
        }

        if (model != null) {
            pomArtifact = artifact;
            dependencies = model.getDependencies();
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            managedDependencies = dependencyManagement == null ? null : dependencyManagement.getDependencies();
            MavenSession session = legacySupport.getSession();
            if (session != null) {
                pomRepositories = session.getProjects().stream()
                        .filter(p -> artifact.equals(p.getArtifact()))
                        .map(MavenProject::getRemoteArtifactRepositories)
                        .findFirst()
                        .orElseGet(() -> getRepositoriesFromModel(repositorySession, model));
            } else {
                pomRepositories = new ArrayList<>();
            }
        } else if (artifact instanceof ArtifactWithDependencies) {
            pomArtifact = artifact;

            dependencies = ((ArtifactWithDependencies) artifact).getDependencies();

            managedDependencies = ((ArtifactWithDependencies) artifact).getManagedDependencies();
        } else {
            ProjectRelocation rel = retrieveRelocatedProject(artifact, request);

            if (rel == null) {
                return null;
            }

            pomArtifact = rel.pomArtifact;

            relocatedArtifact = rel.relocatedArtifact;

            if (rel.project == null) {
                // When this happens we have a Maven 1.x POM, or some invalid POM.
                // It should have never found its way into Maven 2.x repository but it did.
                dependencies = Collections.emptyList();
            } else {
                dependencies = rel.project.getModel().getDependencies();

                DependencyManagement depMgmt = rel.project.getModel().getDependencyManagement();
                managedDependencies = (depMgmt != null) ? depMgmt.getDependencies() : null;

                pomRepositories = rel.project.getRemoteArtifactRepositories();
            }
        }

        Set<Artifact> artifacts = Collections.emptySet();

        if (!artifact.getArtifactHandler().isIncludesDependencies()) {
            artifacts = new LinkedHashSet<>();

            for (Dependency dependency : dependencies) {
                Artifact dependencyArtifact = createDependencyArtifact(dependency, artifact, pomArtifact);

                if (dependencyArtifact != null) {
                    artifacts.add(dependencyArtifact);
                }
            }
        }

        Map<String, Artifact> managedVersions = null;

        if (managedDependencies != null && request.isResolveManagedVersions()) {
            managedVersions = new HashMap<>();

            for (Dependency managedDependency : managedDependencies) {
                Artifact managedArtifact = createDependencyArtifact(managedDependency, null, pomArtifact);

                managedVersions.put(managedDependency.getManagementKey(), managedArtifact);
            }
        }

        List<ArtifactRepository> aggregatedRepositories =
                aggregateRepositories(request.getRemoteRepositories(), pomRepositories);

        ResolutionGroup result =
                new ResolutionGroup(pomArtifact, relocatedArtifact, artifacts, managedVersions, aggregatedRepositories);

        cache.put(
                artifact,
                request.isResolveManagedVersions(),
                request.getLocalRepository(),
                request.getRemoteRepositories(),
                result);

        return result;
    }

    private List<ArtifactRepository> getRepositoriesFromModel(RepositorySystemSession repositorySession, Model model) {
        List<ArtifactRepository> pomRepositories = new ArrayList<>();
        for (Repository modelRepository : model.getRepositories()) {
            try {
                pomRepositories.add(MavenRepositorySystem.buildArtifactRepository(modelRepository));
            } catch (InvalidRepositoryException e) {
                // can not use this then
            }
        }
        mavenRepositorySystem.injectMirror(repositorySession, pomRepositories);
        mavenRepositorySystem.injectProxy(repositorySession, pomRepositories);
        mavenRepositorySystem.injectAuthentication(repositorySession, pomRepositories);
        return pomRepositories;
    }

    private boolean hasFile(Artifact artifact) {
        return artifact != null
                && artifact.getFile() != null
                && artifact.getFile().exists();
    }

    private List<ArtifactRepository> aggregateRepositories(
            List<ArtifactRepository> requestRepositories, List<ArtifactRepository> pomRepositories) {
        List<ArtifactRepository> repositories = requestRepositories;

        if (pomRepositories != null && !pomRepositories.isEmpty()) {
            Map<String, ArtifactRepository> repos = new LinkedHashMap<>();

            for (ArtifactRepository repo : requestRepositories) {
                if (!repos.containsKey(repo.getId())) {
                    repos.put(repo.getId(), repo);
                }
            }

            for (ArtifactRepository repo : pomRepositories) {
                if (!repos.containsKey(repo.getId())) {
                    repos.put(repo.getId(), repo);
                }
            }

            repositories = new ArrayList<>(repos.values());
        }

        return repositories;
    }

    private Artifact createDependencyArtifact(Dependency dependency, Artifact owner, Artifact pom)
            throws ArtifactMetadataRetrievalException {
        try {
            String inheritedScope = (owner != null) ? owner.getScope() : null;

            ArtifactFilter inheritedFilter = (owner != null) ? owner.getDependencyFilter() : null;

            return createDependencyArtifact(artifactFactory, dependency, inheritedScope, inheritedFilter);
        } catch (InvalidVersionSpecificationException e) {
            throw new ArtifactMetadataRetrievalException(
                    "Invalid version for dependency " + dependency.getManagementKey() + ": " + e.getMessage(), e, pom);
        }
    }

    private static Artifact createDependencyArtifact(
            ArtifactFactory factory, Dependency dependency, String inheritedScope, ArtifactFilter inheritedFilter)
            throws InvalidVersionSpecificationException {
        String effectiveScope = getEffectiveScope(dependency.getScope(), inheritedScope);

        if (effectiveScope == null) {
            return null;
        }

        VersionRange versionRange = VersionRange.createFromVersionSpec(dependency.getVersion());

        Artifact dependencyArtifact = factory.createDependencyArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                versionRange,
                dependency.getType(),
                dependency.getClassifier(),
                effectiveScope,
                dependency.isOptional());

        if (inheritedFilter != null && !inheritedFilter.include(dependencyArtifact)) {
            return null;
        }

        if (Artifact.SCOPE_SYSTEM.equals(effectiveScope)) {
            dependencyArtifact.setFile(new File(dependency.getSystemPath()));
        }

        dependencyArtifact.setDependencyFilter(createDependencyFilter(dependency, inheritedFilter));

        return dependencyArtifact;
    }

    private static String getEffectiveScope(String originalScope, String inheritedScope) {
        String effectiveScope = Artifact.SCOPE_RUNTIME;

        if (originalScope == null) {
            originalScope = Artifact.SCOPE_COMPILE;
        }

        if (inheritedScope == null) {
            // direct dependency retains its scope
            effectiveScope = originalScope;
        } else if (Artifact.SCOPE_TEST.equals(originalScope) || Artifact.SCOPE_PROVIDED.equals(originalScope)) {
            // test and provided are not transitive, so exclude them
            effectiveScope = null;
        } else if (Artifact.SCOPE_SYSTEM.equals(originalScope)) {
            // system scope come through unchanged...
            effectiveScope = Artifact.SCOPE_SYSTEM;
        } else if (Artifact.SCOPE_COMPILE.equals(originalScope) && Artifact.SCOPE_COMPILE.equals(inheritedScope)) {
            // added to retain compile scope. Remove if you want compile inherited as runtime
            effectiveScope = Artifact.SCOPE_COMPILE;
        } else if (Artifact.SCOPE_TEST.equals(inheritedScope)) {
            effectiveScope = Artifact.SCOPE_TEST;
        } else if (Artifact.SCOPE_PROVIDED.equals(inheritedScope)) {
            effectiveScope = Artifact.SCOPE_PROVIDED;
        }

        return effectiveScope;
    }

    private static ArtifactFilter createDependencyFilter(Dependency dependency, ArtifactFilter inheritedFilter) {
        ArtifactFilter effectiveFilter = inheritedFilter;

        if (!dependency.getExclusions().isEmpty()) {
            effectiveFilter = new ExclusionArtifactFilter(dependency.getExclusions());

            if (inheritedFilter != null) {
                effectiveFilter = new AndArtifactFilter(Arrays.asList(inheritedFilter, effectiveFilter));
            }
        }

        return effectiveFilter;
    }

    @Override
    public List<ArtifactVersion> retrieveAvailableVersions(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException {
        MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        injectSession(request);
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        return retrieveAvailableVersions(request);
    }

    @Override
    public List<ArtifactVersion> retrieveAvailableVersions(MetadataResolutionRequest request)
            throws ArtifactMetadataRetrievalException {
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata(request.getArtifact());

        try {
            repositoryMetadataManager.resolve(metadata, request);
        } catch (RepositoryMetadataResolutionException e) {
            throw new ArtifactMetadataRetrievalException(e.getMessage(), e, request.getArtifact());
        }

        List<String> availableVersions = request.getLocalRepository().findVersions(request.getArtifact());

        return retrieveAvailableVersionsFromMetadata(metadata.getMetadata(), availableVersions);
    }

    @Override
    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
            Artifact artifact, ArtifactRepository localRepository, ArtifactRepository deploymentRepository)
            throws ArtifactMetadataRetrievalException {
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact);

        try {
            repositoryMetadataManager.resolveAlways(metadata, localRepository, deploymentRepository);
        } catch (RepositoryMetadataResolutionException e) {
            throw new ArtifactMetadataRetrievalException(e.getMessage(), e, artifact);
        }

        List<String> availableVersions = localRepository.findVersions(artifact);

        return retrieveAvailableVersionsFromMetadata(metadata.getMetadata(), availableVersions);
    }

    private List<ArtifactVersion> retrieveAvailableVersionsFromMetadata(
            Metadata repoMetadata, List<String> availableVersions) {
        Collection<String> versions = new LinkedHashSet<>();

        if ((repoMetadata != null) && (repoMetadata.getVersioning() != null)) {
            versions.addAll(repoMetadata.getVersioning().getVersions());
        }

        versions.addAll(availableVersions);

        List<ArtifactVersion> artifactVersions = new ArrayList<>(versions.size());

        for (String version : versions) {
            artifactVersions.add(new DefaultArtifactVersion(version));
        }

        return artifactVersions;
    }

    // USED BY MAVEN ASSEMBLY PLUGIN
    @Deprecated
    public static Set<Artifact> createArtifacts(
            ArtifactFactory artifactFactory,
            List<Dependency> dependencies,
            String inheritedScope,
            ArtifactFilter dependencyFilter,
            MavenProject project)
            throws InvalidDependencyVersionException {
        Set<Artifact> artifacts = new LinkedHashSet<>();

        for (Dependency d : dependencies) {
            Artifact dependencyArtifact;
            try {
                dependencyArtifact = createDependencyArtifact(artifactFactory, d, inheritedScope, dependencyFilter);
            } catch (InvalidVersionSpecificationException e) {
                throw new InvalidDependencyVersionException(project.getId(), d, project.getFile(), e);
            }

            if (dependencyArtifact != null) {
                artifacts.add(dependencyArtifact);
            }
        }

        return artifacts;
    }

    @SuppressWarnings("checkstyle:methodlength")
    private ProjectRelocation retrieveRelocatedProject(Artifact artifact, MetadataResolutionRequest repositoryRequest)
            throws ArtifactMetadataRetrievalException {
        MavenProject project;

        Artifact pomArtifact;
        Artifact relocatedArtifact = null;
        boolean done = false;
        do {
            project = null;

            pomArtifact = artifactFactory.createProjectArtifact(
                    artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope());

            if ("pom".equals(artifact.getType())) {
                pomArtifact.setFile(artifact.getFile());
            }

            if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
                done = true;
            } else {
                try {
                    ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
                    configuration.setLocalRepository(repositoryRequest.getLocalRepository());
                    configuration.setRemoteRepositories(repositoryRequest.getRemoteRepositories());
                    configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                    configuration.setProcessPlugins(false);
                    configuration.setRepositoryMerging(ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT);
                    MavenSession session = legacySupport.getSession();
                    if (session != null) {
                        configuration.setSystemProperties(session.getSystemProperties());
                        configuration.setUserProperties(session.getUserProperties());
                    } else {
                        configuration.setSystemProperties(getSystemProperties());
                        configuration.setUserProperties(new Properties());
                    }
                    configuration.setRepositorySession(legacySupport.getRepositorySession());

                    project = projectBuilder.build(pomArtifact, configuration).getProject();
                } catch (ProjectBuildingException e) {
                    ModelProblem missingParentPom = hasMissingParentPom(e);
                    if (missingParentPom != null) {
                        throw new ArtifactMetadataRetrievalException(
                                "Failed to process POM for " + artifact.getId() + ": " + missingParentPom.getMessage(),
                                missingParentPom.getException(),
                                artifact);
                    }

                    String message;

                    if (isMissingPom(e)) {
                        message = "Missing POM for " + artifact.getId();
                    } else if (isNonTransferablePom(e)) {
                        throw new ArtifactMetadataRetrievalException(
                                "Failed to retrieve POM for " + artifact.getId() + ": "
                                        + e.getCause().getMessage(),
                                e.getCause(),
                                artifact);
                    } else {
                        message = "Invalid POM for " + artifact.getId()
                                + ", transitive dependencies (if any) will not be available"
                                + ", enable verbose output (-X) for more details";
                    }

                    if (logger.isDebugEnabled()) {
                        message += ": " + e.getMessage();
                    }

                    logger.warn(message);
                }

                if (project != null) {
                    Relocation relocation = null;

                    DistributionManagement distMgmt = project.getModel().getDistributionManagement();
                    if (distMgmt != null) {
                        relocation = distMgmt.getRelocation();

                        artifact.setDownloadUrl(distMgmt.getDownloadUrl());
                        pomArtifact.setDownloadUrl(distMgmt.getDownloadUrl());
                    }

                    if (relocation != null) {
                        if (relocation.getGroupId() != null) {
                            artifact.setGroupId(relocation.getGroupId());
                            relocatedArtifact = artifact;
                            project.setGroupId(relocation.getGroupId());
                        }
                        if (relocation.getArtifactId() != null) {
                            artifact.setArtifactId(relocation.getArtifactId());
                            relocatedArtifact = artifact;
                            project.setArtifactId(relocation.getArtifactId());
                        }
                        if (relocation.getVersion() != null) {
                            // note: see MNG-3454. This causes a problem, but fixing it may break more.
                            artifact.setVersionRange(VersionRange.createFromVersion(relocation.getVersion()));
                            relocatedArtifact = artifact;
                            project.setVersion(relocation.getVersion());
                        }

                        if (artifact.getDependencyFilter() != null
                                && !artifact.getDependencyFilter().include(artifact)) {
                            return null;
                        }

                        // MNG-2861: the artifact data has changed. If the available versions where previously
                        // retrieved, we need to update it.
                        // TODO shouldn't the versions be merged across relocations?
                        List<ArtifactVersion> available = artifact.getAvailableVersions();
                        if (available != null && !available.isEmpty()) {
                            MetadataResolutionRequest metadataRequest =
                                    new DefaultMetadataResolutionRequest(repositoryRequest);
                            metadataRequest.setArtifact(artifact);
                            available = retrieveAvailableVersions(metadataRequest);
                            artifact.setAvailableVersions(available);
                        }

                        String message = "  this artifact has been relocated to " + artifact.getGroupId() + ":"
                                + artifact.getArtifactId() + ":" + artifact.getVersion() + ".";

                        if (relocation.getMessage() != null) {
                            message += "  " + relocation.getMessage();
                        }

                        if (artifact.getDependencyTrail() != null
                                && artifact.getDependencyTrail().size() == 1) {
                            logger.warn(
                                    "While downloading {}:{}:{}{}",
                                    pomArtifact.getGroupId(),
                                    pomArtifact.getArtifactId(),
                                    pomArtifact.getVersion(),
                                    message);
                        } else {
                            logger.debug(
                                    "While downloading {}:{}:{}{}",
                                    pomArtifact.getGroupId(),
                                    pomArtifact.getArtifactId(),
                                    pomArtifact.getVersion(),
                                    message);
                        }
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        } while (!done);

        ProjectRelocation rel = new ProjectRelocation();
        rel.project = project;
        rel.pomArtifact = pomArtifact;
        rel.relocatedArtifact = relocatedArtifact;

        return rel;
    }

    private ModelProblem hasMissingParentPom(ProjectBuildingException e) {
        if (e.getCause() instanceof ModelBuildingException) {
            ModelBuildingException mbe = (ModelBuildingException) e.getCause();
            for (ModelProblem problem : mbe.getProblems()) {
                if (problem.getException() instanceof UnresolvableModelException) {
                    return problem;
                }
            }
        }
        return null;
    }

    private boolean isMissingPom(Exception e) {
        if (e.getCause() instanceof MultipleArtifactsNotFoundException) {
            return true;
        }
        return e.getCause() instanceof org.eclipse.aether.resolution.ArtifactResolutionException
                && e.getCause().getCause() instanceof ArtifactNotFoundException;
    }

    private boolean isNonTransferablePom(Exception e) {
        if (e.getCause() instanceof ArtifactResolutionException) {
            return true;
        }
        return e.getCause() instanceof org.eclipse.aether.resolution.ArtifactResolutionException
                && !(e.getCause().getCause() instanceof ArtifactNotFoundException);
    }

    private Properties getSystemProperties() {
        Properties props = new Properties();

        EnvironmentUtils.addEnvVars(props);

        SystemProperties.addSystemProperties(props);

        return props;
    }

    private static final class ProjectRelocation {
        private MavenProject project;

        private Artifact pomArtifact;

        private Artifact relocatedArtifact;
    }
}
