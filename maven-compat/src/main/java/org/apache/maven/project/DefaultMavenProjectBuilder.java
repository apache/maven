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
package org.apache.maven.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 */
@Component(role = MavenProjectBuilder.class)
@Deprecated
public class DefaultMavenProjectBuilder implements MavenProjectBuilder {

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private LegacySupport legacySupport;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    private ProjectBuildingRequest toRequest(ProjectBuilderConfiguration configuration) {
        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();

        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0);
        request.setResolveDependencies(false);

        request.setLocalRepository(configuration.getLocalRepository());
        request.setBuildStartTime(configuration.getBuildStartTime());
        request.setUserProperties(configuration.getUserProperties());
        request.setSystemProperties(configuration.getExecutionProperties());

        ProfileManager profileManager = configuration.getGlobalProfileManager();
        if (profileManager != null) {
            request.setActiveProfileIds(profileManager.getExplicitlyActivatedIds());
            request.setInactiveProfileIds(profileManager.getExplicitlyDeactivatedIds());
        } else {
            /*
             * MNG-4900: Hack to workaround deficiency of legacy API which makes it impossible for plugins to access the
             * global profile manager which is required to build a POM like a CLI invocation does. Failure to consider
             * the activated profiles can cause repo declarations to be lost which in turn will result in artifact
             * resolution failures, in particular when using the enhanced local repo which guards access to local files
             * based on the configured remote repos.
             */
            MavenSession session = legacySupport.getSession();
            if (session != null) {
                MavenExecutionRequest req = session.getRequest();
                if (req != null) {
                    request.setActiveProfileIds(req.getActiveProfiles());
                    request.setInactiveProfileIds(req.getInactiveProfiles());
                }
            }
        }

        return request;
    }

    private ProjectBuildingRequest injectSession(ProjectBuildingRequest request) {
        MavenSession session = legacySupport.getSession();
        if (session != null) {
            request.setRepositorySession(session.getRepositorySession());
            request.setSystemProperties(session.getSystemProperties());
            if (request.getUserProperties().isEmpty()) {
                request.setUserProperties(session.getUserProperties());
            }

            MavenExecutionRequest req = session.getRequest();
            if (req != null) {
                request.setRemoteRepositories(req.getRemoteRepositories());
            }
        } else {
            Properties props = new Properties();
            EnvironmentUtils.addEnvVars(props);
            props.putAll(System.getProperties());
            request.setSystemProperties(props);
        }

        return request;
    }

    @SuppressWarnings("unchecked")
    private List<ArtifactRepository> normalizeToArtifactRepositories(
            List<?> repositories, ProjectBuildingRequest request) throws ProjectBuildingException {
        /*
         * This provides backward-compat with 2.x that allowed plugins like the maven-remote-resources-plugin:1.0 to
         * populate the builder configuration with model repositories instead of artifact repositories.
         */

        if (repositories != null) {
            boolean normalized = false;

            List<ArtifactRepository> repos = new ArrayList<>(repositories.size());

            for (Object repository : repositories) {
                if (repository instanceof Repository) {
                    try {
                        ArtifactRepository repo = repositorySystem.buildArtifactRepository((Repository) repository);
                        repositorySystem.injectMirror(request.getRepositorySession(), Arrays.asList(repo));
                        repositorySystem.injectProxy(request.getRepositorySession(), Arrays.asList(repo));
                        repositorySystem.injectAuthentication(request.getRepositorySession(), Arrays.asList(repo));
                        repos.add(repo);
                    } catch (InvalidRepositoryException e) {
                        throw new ProjectBuildingException("", "Invalid remote repository " + repository, e);
                    }
                    normalized = true;
                } else {
                    repos.add((ArtifactRepository) repository);
                }
            }

            if (normalized) {
                return repos;
            }
        }

        return (List<ArtifactRepository>) repositories;
    }

    private ProjectBuildingException transformError(ProjectBuildingException e) {
        if (e.getCause() instanceof ModelBuildingException) {
            return new InvalidProjectModelException(e.getProjectId(), e.getMessage(), e.getPomFile());
        }

        return e;
    }

    public MavenProject build(File pom, ProjectBuilderConfiguration configuration) throws ProjectBuildingException {
        ProjectBuildingRequest request = injectSession(toRequest(configuration));

        try {
            return projectBuilder.build(pom, request).getProject();
        } catch (ProjectBuildingException e) {
            throw transformError(e);
        }
    }

    // This is used by the SITE plugin.
    public MavenProject build(File pom, ArtifactRepository localRepository, ProfileManager profileManager)
            throws ProjectBuildingException {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository(localRepository);
        configuration.setGlobalProfileManager(profileManager);

        return build(pom, configuration);
    }

    public MavenProject buildFromRepository(
            Artifact artifact,
            List<ArtifactRepository> remoteRepositories,
            ProjectBuilderConfiguration configuration,
            boolean allowStubModel)
            throws ProjectBuildingException {
        ProjectBuildingRequest request = injectSession(toRequest(configuration));
        request.setRemoteRepositories(normalizeToArtifactRepositories(remoteRepositories, request));
        request.setProcessPlugins(false);
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        try {
            return projectBuilder.build(artifact, allowStubModel, request).getProject();
        } catch (ProjectBuildingException e) {
            throw transformError(e);
        }
    }

    public MavenProject buildFromRepository(
            Artifact artifact,
            List<ArtifactRepository> remoteRepositories,
            ArtifactRepository localRepository,
            boolean allowStubModel)
            throws ProjectBuildingException {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository(localRepository);

        return buildFromRepository(artifact, remoteRepositories, configuration, allowStubModel);
    }

    public MavenProject buildFromRepository(
            Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository)
            throws ProjectBuildingException {
        return buildFromRepository(artifact, remoteRepositories, localRepository, true);
    }

    /**
     * This is used for pom-less execution like running archetype:generate. I am taking out the profile handling and the
     * interpolation of the base directory until we spec this out properly.
     */
    public MavenProject buildStandaloneSuperProject(ProjectBuilderConfiguration configuration)
            throws ProjectBuildingException {
        ProjectBuildingRequest request = injectSession(toRequest(configuration));
        request.setProcessPlugins(false);
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        ModelSource modelSource = new UrlModelSource(getClass().getResource("standalone.xml"));

        MavenProject project = projectBuilder.build(modelSource, request).getProject();
        project.setExecutionRoot(true);
        return project;
    }

    public MavenProject buildStandaloneSuperProject(ArtifactRepository localRepository)
            throws ProjectBuildingException {
        return buildStandaloneSuperProject(localRepository, null);
    }

    public MavenProject buildStandaloneSuperProject(ArtifactRepository localRepository, ProfileManager profileManager)
            throws ProjectBuildingException {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository(localRepository);
        configuration.setGlobalProfileManager(profileManager);

        return buildStandaloneSuperProject(configuration);
    }

    public MavenProject buildWithDependencies(
            File pom,
            ArtifactRepository localRepository,
            ProfileManager profileManager,
            TransferListener transferListener)
            throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository(localRepository);
        configuration.setGlobalProfileManager(profileManager);

        ProjectBuildingRequest request = injectSession(toRequest(configuration));

        request.setResolveDependencies(true);

        try {
            return projectBuilder.build(pom, request).getProject();
        } catch (ProjectBuildingException e) {
            throw transformError(e);
        }
    }

    public MavenProject buildWithDependencies(
            File pom, ArtifactRepository localRepository, ProfileManager profileManager)
            throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        return buildWithDependencies(pom, localRepository, profileManager, null);
    }
}
