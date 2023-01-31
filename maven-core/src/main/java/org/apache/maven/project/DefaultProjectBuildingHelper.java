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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;

/**
 * Assists the project builder. <strong>Warning:</strong> This is an internal utility class that is only public for
 * technical reasons, it is not part of the public API. In particular, this class can be changed or deleted without
 * prior notice.
 *
 * @author Benjamin Bentmann
 */
@Component(role = ProjectBuildingHelper.class)
public class DefaultProjectBuildingHelper implements ProjectBuildingHelper {

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private ClassRealmManager classRealmManager;

    @Requirement
    private ProjectRealmCache projectRealmCache;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private MavenPluginManager pluginManager;

    public List<ArtifactRepository> createArtifactRepositories(
            List<Repository> pomRepositories,
            List<ArtifactRepository> externalRepositories,
            ProjectBuildingRequest request)
            throws InvalidRepositoryException {
        List<ArtifactRepository> internalRepositories = new ArrayList<>();

        for (Repository repository : pomRepositories) {
            internalRepositories.add(repositorySystem.buildArtifactRepository(repository));
        }

        repositorySystem.injectMirror(request.getRepositorySession(), internalRepositories);

        repositorySystem.injectProxy(request.getRepositorySession(), internalRepositories);

        repositorySystem.injectAuthentication(request.getRepositorySession(), internalRepositories);

        List<ArtifactRepository> dominantRepositories;
        List<ArtifactRepository> recessiveRepositories;

        if (ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT.equals(request.getRepositoryMerging())) {
            dominantRepositories = externalRepositories;
            recessiveRepositories = internalRepositories;
        } else {
            dominantRepositories = internalRepositories;
            recessiveRepositories = externalRepositories;
        }

        List<ArtifactRepository> artifactRepositories = new ArrayList<>();
        Collection<String> repoIds = new HashSet<>();

        if (dominantRepositories != null) {
            for (ArtifactRepository repository : dominantRepositories) {
                repoIds.add(repository.getId());
                artifactRepositories.add(repository);
            }
        }

        if (recessiveRepositories != null) {
            for (ArtifactRepository repository : recessiveRepositories) {
                if (repoIds.add(repository.getId())) {
                    artifactRepositories.add(repository);
                }
            }
        }

        artifactRepositories = repositorySystem.getEffectiveRepositories(artifactRepositories);

        return artifactRepositories;
    }

    public synchronized ProjectRealmCache.CacheRecord createProjectRealm(
            MavenProject project, Model model, ProjectBuildingRequest request)
            throws PluginResolutionException, PluginVersionResolutionException, PluginManagerException {
        ClassRealm projectRealm;

        List<Plugin> extensionPlugins = new ArrayList<>();

        Build build = model.getBuild();

        if (build != null) {
            for (Extension extension : build.getExtensions()) {
                Plugin plugin = new Plugin();
                plugin.setGroupId(extension.getGroupId());
                plugin.setArtifactId(extension.getArtifactId());
                plugin.setVersion(extension.getVersion());
                extensionPlugins.add(plugin);
            }

            for (Plugin plugin : build.getPlugins()) {
                if (plugin.isExtensions()) {
                    extensionPlugins.add(plugin);
                }
            }
        }

        if (extensionPlugins.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Extension realms for project " + model.getId() + ": (none)");
            }

            return new ProjectRealmCache.CacheRecord(null, null);
        }

        List<ClassRealm> extensionRealms = new ArrayList<>();

        Map<ClassRealm, List<String>> exportedPackages = new HashMap<>();

        Map<ClassRealm, List<String>> exportedArtifacts = new HashMap<>();

        List<Artifact> publicArtifacts = new ArrayList<>();

        for (Plugin plugin : extensionPlugins) {
            ExtensionRealmCache.CacheRecord recordRealm =
                    pluginManager.setupExtensionsRealm(project, plugin, request.getRepositorySession());

            final ClassRealm extensionRealm = recordRealm.getRealm();
            final ExtensionDescriptor extensionDescriptor = recordRealm.getDescriptor();
            final List<Artifact> artifacts = recordRealm.getArtifacts();

            extensionRealms.add(extensionRealm);
            if (extensionDescriptor != null) {
                exportedPackages.put(extensionRealm, extensionDescriptor.getExportedPackages());
                exportedArtifacts.put(extensionRealm, extensionDescriptor.getExportedArtifacts());
            }

            if (!plugin.isExtensions()
                    && artifacts.size() == 1
                    && artifacts.get(0).getFile() != null) {
                /*
                 * This is purely for backward-compat with 2.x where <extensions> consisting of a single artifact where
                 * loaded into the core and hence available to plugins, in contrast to bigger extensions that were
                 * loaded into a dedicated realm which is invisible to plugins (MNG-2749).
                 */
                publicArtifacts.addAll(artifacts);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Extension realms for project " + model.getId() + ": " + extensionRealms);
        }

        ProjectRealmCache.Key projectRealmKey = projectRealmCache.createKey(extensionRealms);

        ProjectRealmCache.CacheRecord record = projectRealmCache.get(projectRealmKey);

        if (record == null) {
            projectRealm = classRealmManager.createProjectRealm(model, toAetherArtifacts(publicArtifacts));

            Set<String> exclusions = new LinkedHashSet<>();

            for (ClassRealm extensionRealm : extensionRealms) {
                List<String> excludes = exportedArtifacts.get(extensionRealm);

                if (excludes != null) {
                    exclusions.addAll(excludes);
                }

                List<String> exports = exportedPackages.get(extensionRealm);

                if (exports == null || exports.isEmpty()) {
                    /*
                     * Most existing extensions don't define exported packages, i.e. no classes are to be exposed to
                     * plugins, yet the components provided by the extension (e.g. artifact handlers) must be
                     * accessible, i.e. we still must import the extension realm into the project realm.
                     */
                    exports = Arrays.asList(extensionRealm.getId());
                }

                for (String export : exports) {
                    projectRealm.importFrom(extensionRealm, export);
                }
            }

            DependencyFilter extensionArtifactFilter = null;
            if (!exclusions.isEmpty()) {
                extensionArtifactFilter = new ExclusionsDependencyFilter(exclusions);
            }

            record = projectRealmCache.put(projectRealmKey, projectRealm, extensionArtifactFilter);
        }

        projectRealmCache.register(project, projectRealmKey, record);

        return record;
    }

    public void selectProjectRealm(MavenProject project) {
        ClassLoader projectRealm = project.getClassRealm();

        if (projectRealm == null) {
            projectRealm = classRealmManager.getCoreRealm();
        }

        Thread.currentThread().setContextClassLoader(projectRealm);
    }

    private List<org.eclipse.aether.artifact.Artifact> toAetherArtifacts(final List<Artifact> pluginArtifacts) {
        return new ArrayList<>(RepositoryUtils.toArtifacts(pluginArtifacts));
    }
}
