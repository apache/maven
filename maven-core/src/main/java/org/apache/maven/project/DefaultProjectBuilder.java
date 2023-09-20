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
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.DefaultModelCacheFactory;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * DefaultProjectBuilder
 */
@Component(role = ProjectBuilder.class)
public class DefaultProjectBuilder implements ProjectBuilder {

    public static final String DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY =
            "maven.defaultProjectBuilder.disableGlobalModelCache";

    @Requirement
    private Logger logger;

    @Requirement
    private ModelBuilder modelBuilder;

    @Requirement
    private ModelProcessor modelProcessor;

    @Requirement
    private ProjectBuildingHelper projectBuildingHelper;

    @Requirement
    private MavenRepositorySystem repositorySystem;

    @Requirement
    private org.eclipse.aether.RepositorySystem repoSystem;

    @Requirement
    private RemoteRepositoryManager repositoryManager;

    @Requirement
    private ProjectDependenciesResolver dependencyResolver;

    @Requirement
    private ModelCacheFactory modelCacheFactory;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    @Override
    public ProjectBuildingResult build(File pomFile, ProjectBuildingRequest request) throws ProjectBuildingException {
        return build(
                pomFile,
                new FileModelSource(pomFile),
                new InternalConfig(
                        request,
                        null,
                        useGlobalModelCache() ? createModelCache(request.getRepositorySession()) : null));
    }

    private boolean useGlobalModelCache() {
        return !Boolean.getBoolean(DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY);
    }

    @Override
    public ProjectBuildingResult build(ModelSource modelSource, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return build(
                null,
                modelSource,
                new InternalConfig(
                        request,
                        null,
                        useGlobalModelCache() ? createModelCache(request.getRepositorySession()) : null));
    }

    private ProjectBuildingResult build(File pomFile, ModelSource modelSource, InternalConfig config)
            throws ProjectBuildingException {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            ProjectBuildingRequest projectBuildingRequest = config.request;

            MavenProject project = projectBuildingRequest.getProject();

            List<ModelProblem> modelProblems = null;
            Throwable error = null;

            if (project == null) {
                ModelBuildingRequest request = getModelBuildingRequest(config);

                project = new MavenProject();
                project.setFile(pomFile);

                DefaultModelBuildingListener listener =
                        new DefaultModelBuildingListener(project, projectBuildingHelper, projectBuildingRequest);
                request.setModelBuildingListener(listener);

                request.setPomFile(pomFile);
                request.setModelSource(modelSource);
                request.setLocationTracking(true);

                ModelBuildingResult result;
                try {
                    result = modelBuilder.build(request);
                } catch (ModelBuildingException e) {
                    result = e.getResult();
                    if (result == null || result.getEffectiveModel() == null) {
                        throw new ProjectBuildingException(e.getModelId(), e.getMessage(), pomFile, e);
                    }
                    // validation error, continue project building and delay failing to help IDEs
                    error = e;
                }

                modelProblems = result.getProblems();

                initProject(
                        project,
                        Collections.<String, MavenProject>emptyMap(),
                        true,
                        result,
                        new HashMap<File, Boolean>(),
                        projectBuildingRequest);
            } else if (projectBuildingRequest.isResolveDependencies()) {
                projectBuildingHelper.selectProjectRealm(project);
            }

            DependencyResolutionResult resolutionResult = null;

            if (projectBuildingRequest.isResolveDependencies()) {
                resolutionResult = resolveDependencies(project, config.session);
            }

            ProjectBuildingResult result = new DefaultProjectBuildingResult(project, modelProblems, resolutionResult);

            if (error != null) {
                ProjectBuildingException e = new ProjectBuildingException(Arrays.asList(result));
                e.initCause(error);
                throw e;
            }

            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }

    private DependencyResolutionResult resolveDependencies(MavenProject project, RepositorySystemSession session) {
        DependencyResolutionResult resolutionResult;

        try {
            DefaultDependencyResolutionRequest resolution = new DefaultDependencyResolutionRequest(project, session);
            resolutionResult = dependencyResolver.resolve(resolution);
        } catch (DependencyResolutionException e) {
            resolutionResult = e.getResult();
        }

        Set<Artifact> artifacts = new LinkedHashSet<>();
        if (resolutionResult.getDependencyGraph() != null) {
            RepositoryUtils.toArtifacts(
                    artifacts,
                    resolutionResult.getDependencyGraph().getChildren(),
                    Collections.singletonList(project.getArtifact().getId()),
                    null);

            // Maven 2.x quirk: an artifact always points at the local repo, regardless whether resolved or not
            LocalRepositoryManager lrm = session.getLocalRepositoryManager();
            for (Artifact artifact : artifacts) {
                if (!artifact.isResolved()) {
                    String path = lrm.getPathForLocalArtifact(RepositoryUtils.toArtifact(artifact));
                    artifact.setFile(new File(lrm.getRepository().getBasedir(), path));
                }
            }
        }
        project.setResolvedArtifacts(artifacts);
        project.setArtifacts(artifacts);

        return resolutionResult;
    }

    private List<String> getProfileIds(List<Profile> profiles) {
        List<String> ids = new ArrayList<>(profiles.size());

        for (Profile profile : profiles) {
            ids.add(profile.getId());
        }

        return ids;
    }

    private ModelBuildingRequest getModelBuildingRequest(InternalConfig config) {
        ProjectBuildingRequest configuration = config.request;

        ModelBuildingRequest request = new DefaultModelBuildingRequest();

        RequestTrace trace = RequestTrace.newChild(null, configuration).newChild(request);

        ModelResolver resolver = new ProjectModelResolver(
                config.session,
                trace,
                repoSystem,
                repositoryManager,
                config.repositories,
                configuration.getRepositoryMerging(),
                config.modelPool);

        request.setValidationLevel(configuration.getValidationLevel());
        request.setProcessPlugins(configuration.isProcessPlugins());
        request.setProfiles(configuration.getProfiles());
        request.setActiveProfileIds(configuration.getActiveProfileIds());
        request.setInactiveProfileIds(configuration.getInactiveProfileIds());
        request.setSystemProperties(configuration.getSystemProperties());
        request.setUserProperties(configuration.getUserProperties());
        request.setBuildStartTime(configuration.getBuildStartTime());
        request.setModelResolver(resolver);
        request.setModelCache(config.modelCache);

        return request;
    }

    @Override
    public ProjectBuildingResult build(Artifact artifact, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return build(artifact, false, request);
    }

    @Override
    public ProjectBuildingResult build(Artifact artifact, boolean allowStubModel, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        org.eclipse.aether.artifact.Artifact pomArtifact = RepositoryUtils.toArtifact(artifact);
        pomArtifact = ArtifactDescriptorUtils.toPomArtifact(pomArtifact);

        InternalConfig config = new InternalConfig(
                request, null, useGlobalModelCache() ? createModelCache(request.getRepositorySession()) : null);

        boolean localProject;

        try {
            ArtifactRequest pomRequest = new ArtifactRequest();
            pomRequest.setArtifact(pomArtifact);
            pomRequest.setRepositories(config.repositories);
            ArtifactResult pomResult = repoSystem.resolveArtifact(config.session, pomRequest);

            pomArtifact = pomResult.getArtifact();
            localProject = pomResult.getRepository() instanceof WorkspaceRepository;
        } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
            if (e.getResults().get(0).isMissing() && allowStubModel) {
                return build(null, createStubModelSource(artifact), config);
            }
            throw new ProjectBuildingException(
                    artifact.getId(), "Error resolving project artifact: " + e.getMessage(), e);
        }

        File pomFile = pomArtifact.getFile();

        if ("pom".equals(artifact.getType())) {
            artifact.selectVersion(pomArtifact.getVersion());
            artifact.setFile(pomFile);
            artifact.setResolved(true);
        }

        return build(localProject ? pomFile : null, new FileModelSource(pomFile), config);
    }

    private ModelSource createStubModelSource(Artifact artifact) {
        StringBuilder buffer = new StringBuilder(1024);

        buffer.append("<?xml version='1.0'?>");
        buffer.append("<project>");
        buffer.append("<modelVersion>4.0.0</modelVersion>");
        buffer.append("<groupId>").append(artifact.getGroupId()).append("</groupId>");
        buffer.append("<artifactId>").append(artifact.getArtifactId()).append("</artifactId>");
        buffer.append("<version>").append(artifact.getBaseVersion()).append("</version>");
        buffer.append("<packaging>").append(artifact.getType()).append("</packaging>");
        buffer.append("</project>");

        return new StringModelSource(buffer, artifact.getId());
    }

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        List<ProjectBuildingResult> results = new ArrayList<>();

        List<InterimResult> interimResults = new ArrayList<>();

        ReactorModelPool modelPool = new ReactorModelPool();

        InternalConfig config = new InternalConfig(
                request,
                modelPool,
                useGlobalModelCache() ? createModelCache(request.getRepositorySession()) : new ReactorModelCache());

        Map<String, MavenProject> projectIndex = new HashMap<>(256);

        boolean noErrors = build(
                results, interimResults, projectIndex, pomFiles, new LinkedHashSet<File>(), true, recursive, config);

        populateReactorModelPool(modelPool, interimResults);

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            noErrors = build(
                            results,
                            new ArrayList<MavenProject>(),
                            projectIndex,
                            interimResults,
                            request,
                            new HashMap<File, Boolean>(),
                            config.session)
                    && noErrors;
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }

        if (!noErrors) {
            throw new ProjectBuildingException(results);
        }

        return results;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean build(
            List<ProjectBuildingResult> results,
            List<InterimResult> interimResults,
            Map<String, MavenProject> projectIndex,
            List<File> pomFiles,
            Set<File> aggregatorFiles,
            boolean isRoot,
            boolean recursive,
            InternalConfig config) {
        boolean noErrors = true;

        for (File pomFile : pomFiles) {
            aggregatorFiles.add(pomFile);

            if (!build(results, interimResults, projectIndex, pomFile, aggregatorFiles, isRoot, recursive, config)) {
                noErrors = false;
            }

            aggregatorFiles.remove(pomFile);
        }

        return noErrors;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean build(
            List<ProjectBuildingResult> results,
            List<InterimResult> interimResults,
            Map<String, MavenProject> projectIndex,
            File pomFile,
            Set<File> aggregatorFiles,
            boolean isRoot,
            boolean recursive,
            InternalConfig config) {
        boolean noErrors = true;

        ModelBuildingRequest request = getModelBuildingRequest(config);

        MavenProject project = new MavenProject();
        project.setFile(pomFile);

        request.setPomFile(pomFile);
        request.setTwoPhaseBuilding(true);
        request.setLocationTracking(true);

        DefaultModelBuildingListener listener =
                new DefaultModelBuildingListener(project, projectBuildingHelper, config.request);
        request.setModelBuildingListener(listener);

        ModelBuildingResult result;
        try {
            result = modelBuilder.build(request);
        } catch (ModelBuildingException e) {
            result = e.getResult();
            if (result == null || result.getEffectiveModel() == null) {
                results.add(new DefaultProjectBuildingResult(e.getModelId(), pomFile, e.getProblems()));

                return false;
            }
            // validation error, continue project building and delay failing to help IDEs
            // result.getProblems().addAll(e.getProblems()) ?
            noErrors = false;
        }

        Model model = result.getEffectiveModel();
        try {
            // first pass: build without building parent.
            initProject(project, projectIndex, false, result, new HashMap<File, Boolean>(0), config.request);
        } catch (InvalidArtifactRTException iarte) {
            result.getProblems()
                    .add(new DefaultModelProblem(null, ModelProblem.Severity.ERROR, null, model, -1, -1, iarte));
        }

        projectIndex.put(result.getModelIds().get(0), project);

        InterimResult interimResult = new InterimResult(pomFile, request, result, listener, isRoot);
        interimResults.add(interimResult);

        if (recursive && !model.getModules().isEmpty()) {
            File basedir = pomFile.getParentFile();

            List<File> moduleFiles = new ArrayList<>();

            for (String module : model.getModules()) {
                if (StringUtils.isEmpty(module)) {
                    continue;
                }

                module = module.replace('\\', File.separatorChar).replace('/', File.separatorChar);

                File moduleFile = new File(basedir, module);

                if (moduleFile.isDirectory()) {
                    moduleFile = modelProcessor.locatePom(moduleFile);
                }

                if (!moduleFile.isFile()) {
                    ModelProblem problem = new DefaultModelProblem(
                            "Child module " + moduleFile + " of " + pomFile + " does not exist",
                            ModelProblem.Severity.ERROR,
                            ModelProblem.Version.BASE,
                            model,
                            -1,
                            -1,
                            null);
                    result.getProblems().add(problem);

                    noErrors = false;

                    continue;
                }

                if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                    // we don't canonicalize on unix to avoid interfering with symlinks
                    try {
                        moduleFile = moduleFile.getCanonicalFile();
                    } catch (IOException e) {
                        moduleFile = moduleFile.getAbsoluteFile();
                    }
                } else {
                    moduleFile = new File(moduleFile.toURI().normalize());
                }

                if (aggregatorFiles.contains(moduleFile)) {
                    StringBuilder buffer = new StringBuilder(256);
                    for (File aggregatorFile : aggregatorFiles) {
                        buffer.append(aggregatorFile).append(" -> ");
                    }
                    buffer.append(moduleFile);

                    ModelProblem problem = new DefaultModelProblem(
                            "Child module " + moduleFile + " of " + pomFile + " forms aggregation cycle " + buffer,
                            ModelProblem.Severity.ERROR,
                            ModelProblem.Version.BASE,
                            model,
                            -1,
                            -1,
                            null);
                    result.getProblems().add(problem);

                    noErrors = false;

                    continue;
                }

                moduleFiles.add(moduleFile);
            }

            interimResult.modules = new ArrayList<>();

            if (!build(
                    results,
                    interimResult.modules,
                    projectIndex,
                    moduleFiles,
                    aggregatorFiles,
                    false,
                    recursive,
                    config)) {
                noErrors = false;
            }
        }

        return noErrors;
    }

    static class InterimResult {

        File pomFile;

        ModelBuildingRequest request;

        ModelBuildingResult result;

        DefaultModelBuildingListener listener;

        boolean root;

        List<InterimResult> modules = Collections.emptyList();

        InterimResult(
                File pomFile,
                ModelBuildingRequest request,
                ModelBuildingResult result,
                DefaultModelBuildingListener listener,
                boolean root) {
            this.pomFile = pomFile;
            this.request = request;
            this.result = result;
            this.listener = listener;
            this.root = root;
        }
    }

    private void populateReactorModelPool(ReactorModelPool reactorModelPool, List<InterimResult> interimResults) {
        for (InterimResult interimResult : interimResults) {
            Model model = interimResult.result.getEffectiveModel();
            reactorModelPool.put(model.getGroupId(), model.getArtifactId(), model.getVersion(), model.getPomFile());

            populateReactorModelPool(reactorModelPool, interimResult.modules);
        }
    }

    private boolean build(
            List<ProjectBuildingResult> results,
            List<MavenProject> projects,
            Map<String, MavenProject> projectIndex,
            List<InterimResult> interimResults,
            ProjectBuildingRequest request,
            Map<File, Boolean> profilesXmls,
            RepositorySystemSession session) {
        boolean noErrors = true;

        for (InterimResult interimResult : interimResults) {
            MavenProject project = interimResult.listener.getProject();
            try {
                ModelBuildingResult result = modelBuilder.build(interimResult.request, interimResult.result);

                // 2nd pass of initialization: resolve and build parent if necessary
                try {
                    initProject(project, projectIndex, true, result, profilesXmls, request);
                } catch (InvalidArtifactRTException iarte) {
                    result.getProblems()
                            .add(new DefaultModelProblem(
                                    null,
                                    ModelProblem.Severity.ERROR,
                                    null,
                                    result.getEffectiveModel(),
                                    -1,
                                    -1,
                                    iarte));
                }

                List<MavenProject> modules = new ArrayList<>();
                noErrors = build(results, modules, projectIndex, interimResult.modules, request, profilesXmls, session)
                        && noErrors;

                projects.addAll(modules);
                projects.add(project);

                project.setExecutionRoot(interimResult.root);
                project.setCollectedProjects(modules);
                DependencyResolutionResult resolutionResult = null;
                if (request.isResolveDependencies()) {
                    resolutionResult = resolveDependencies(project, session);
                }

                results.add(new DefaultProjectBuildingResult(project, result.getProblems(), resolutionResult));
            } catch (ModelBuildingException e) {
                DefaultProjectBuildingResult result = null;
                if (project == null) {
                    result = new DefaultProjectBuildingResult(e.getModelId(), interimResult.pomFile, e.getProblems());
                } else {
                    result = new DefaultProjectBuildingResult(project, e.getProblems(), null);
                }
                results.add(result);

                noErrors = false;
            }
        }

        return noErrors;
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void initProject(
            MavenProject project,
            Map<String, MavenProject> projects,
            boolean buildParentIfNotExisting,
            ModelBuildingResult result,
            Map<File, Boolean> profilesXmls,
            ProjectBuildingRequest projectBuildingRequest) {
        Model model = result.getEffectiveModel();

        project.setModel(model);
        project.setOriginalModel(result.getRawModel());
        project.setFile(model.getPomFile());

        initParent(project, projects, buildParentIfNotExisting, result, projectBuildingRequest);

        Artifact projectArtifact = repositorySystem.createArtifact(
                project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging());
        project.setArtifact(projectArtifact);

        if (project.getFile() != null && buildParentIfNotExisting) // only set those on 2nd phase, ignore on 1st pass
        {
            Build build = project.getBuild();
            project.addScriptSourceRoot(build.getScriptSourceDirectory());
            project.addCompileSourceRoot(build.getSourceDirectory());
            project.addTestCompileSourceRoot(build.getTestSourceDirectory());
        }

        List<Profile> activeProfiles = new ArrayList<>();
        activeProfiles.addAll(result.getActivePomProfiles(result.getModelIds().get(0)));
        activeProfiles.addAll(result.getActiveExternalProfiles());
        project.setActiveProfiles(activeProfiles);

        project.setInjectedProfileIds("external", getProfileIds(result.getActiveExternalProfiles()));
        for (String modelId : result.getModelIds()) {
            project.setInjectedProfileIds(modelId, getProfileIds(result.getActivePomProfiles(modelId)));
        }

        String modelId = findProfilesXml(result, profilesXmls);
        if (modelId != null) {
            ModelProblem problem = new DefaultModelProblem(
                    "Detected profiles.xml alongside " + modelId + ", this file is no longer supported and was ignored"
                            + ", please use the settings.xml instead",
                    ModelProblem.Severity.WARNING,
                    ModelProblem.Version.V30,
                    model,
                    -1,
                    -1,
                    null);
            result.getProblems().add(problem);
        }

        //
        // All the parts that were taken out of MavenProject for Maven 4.0.0
        //

        project.setProjectBuildingRequest(projectBuildingRequest);

        // pluginArtifacts
        Set<Artifact> pluginArtifacts = new HashSet<>();
        for (Plugin plugin : project.getBuildPlugins()) {
            Artifact artifact = repositorySystem.createPluginArtifact(plugin);

            if (artifact != null) {
                pluginArtifacts.add(artifact);
            }
        }
        project.setPluginArtifacts(pluginArtifacts);

        // reportArtifacts
        Set<Artifact> reportArtifacts = new HashSet<>();
        for (ReportPlugin report : project.getReportPlugins()) {
            Plugin pp = new Plugin();
            pp.setGroupId(report.getGroupId());
            pp.setArtifactId(report.getArtifactId());
            pp.setVersion(report.getVersion());

            Artifact artifact = repositorySystem.createPluginArtifact(pp);

            if (artifact != null) {
                reportArtifacts.add(artifact);
            }
        }
        project.setReportArtifacts(reportArtifacts);

        // extensionArtifacts
        Set<Artifact> extensionArtifacts = new HashSet<>();
        List<Extension> extensions = project.getBuildExtensions();
        if (extensions != null) {
            for (Extension ext : extensions) {
                String version;
                if (StringUtils.isEmpty(ext.getVersion())) {
                    version = "RELEASE";
                } else {
                    version = ext.getVersion();
                }

                Artifact artifact =
                        repositorySystem.createArtifact(ext.getGroupId(), ext.getArtifactId(), version, null, "jar");

                if (artifact != null) {
                    extensionArtifacts.add(artifact);
                }
            }
        }
        project.setExtensionArtifacts(extensionArtifacts);

        // managedVersionMap
        Map<String, Artifact> map = null;
        if (repositorySystem != null) {
            final DependencyManagement dependencyManagement = project.getDependencyManagement();
            if ((dependencyManagement != null)
                    && ((dependencyManagement.getDependencies()) != null)
                    && (dependencyManagement.getDependencies().size() > 0)) {
                map = new AbstractMap<String, Artifact>() {
                    HashMap<String, Artifact> delegate;

                    @Override
                    public Set<Entry<String, Artifact>> entrySet() {
                        return Collections.unmodifiableSet(compute().entrySet());
                    }

                    @Override
                    public Set<String> keySet() {
                        return Collections.unmodifiableSet(compute().keySet());
                    }

                    @Override
                    public Collection<Artifact> values() {
                        return Collections.unmodifiableCollection(compute().values());
                    }

                    @Override
                    public boolean containsValue(Object value) {
                        return compute().containsValue(value);
                    }

                    @Override
                    public boolean containsKey(Object key) {
                        return compute().containsKey(key);
                    }

                    @Override
                    public Artifact get(Object key) {
                        return compute().get(key);
                    }

                    HashMap<String, Artifact> compute() {
                        if (delegate == null) {
                            delegate = new HashMap<>();
                            for (Dependency d : dependencyManagement.getDependencies()) {
                                Artifact artifact = repositorySystem.createDependencyArtifact(d);

                                if (artifact != null) {
                                    delegate.put(d.getManagementKey(), artifact);
                                }
                            }
                        }

                        return delegate;
                    }
                };
            } else {
                map = Collections.emptyMap();
            }
        }
        project.setManagedVersionMap(map);

        // release artifact repository
        if (project.getDistributionManagement() != null
                && project.getDistributionManagement().getRepository() != null) {
            try {
                DeploymentRepository r = project.getDistributionManagement().getRepository();
                if (!StringUtils.isEmpty(r.getId()) && !StringUtils.isEmpty(r.getUrl())) {
                    ArtifactRepository repo = repositorySystem.buildArtifactRepository(r);
                    repositorySystem.injectProxy(projectBuildingRequest.getRepositorySession(), Arrays.asList(repo));
                    repositorySystem.injectAuthentication(
                            projectBuildingRequest.getRepositorySession(), Arrays.asList(repo));
                    project.setReleaseArtifactRepository(repo);
                }
            } catch (InvalidRepositoryException e) {
                throw new IllegalStateException(
                        "Failed to create release distribution repository for " + project.getId(), e);
            }
        }

        // snapshot artifact repository
        if (project.getDistributionManagement() != null
                && project.getDistributionManagement().getSnapshotRepository() != null) {
            try {
                DeploymentRepository r = project.getDistributionManagement().getSnapshotRepository();
                if (!StringUtils.isEmpty(r.getId()) && !StringUtils.isEmpty(r.getUrl())) {
                    ArtifactRepository repo = repositorySystem.buildArtifactRepository(r);
                    repositorySystem.injectProxy(projectBuildingRequest.getRepositorySession(), Arrays.asList(repo));
                    repositorySystem.injectAuthentication(
                            projectBuildingRequest.getRepositorySession(), Arrays.asList(repo));
                    project.setSnapshotArtifactRepository(repo);
                }
            } catch (InvalidRepositoryException e) {
                throw new IllegalStateException(
                        "Failed to create snapshot distribution repository for " + project.getId(), e);
            }
        }
    }

    private void initParent(
            MavenProject project,
            Map<String, MavenProject> projects,
            boolean buildParentIfNotExisting,
            ModelBuildingResult result,
            ProjectBuildingRequest projectBuildingRequest) {
        Model parentModel =
                result.getModelIds().size() > 1 && !result.getModelIds().get(1).isEmpty()
                        ? result.getRawModel(result.getModelIds().get(1))
                        : null;

        if (parentModel != null) {
            final String parentGroupId = inheritedGroupId(result, 1);
            final String parentVersion = inheritedVersion(result, 1);

            project.setParentArtifact(
                    repositorySystem.createProjectArtifact(parentGroupId, parentModel.getArtifactId(), parentVersion));

            // org.apache.maven.its.mng4834:parent:0.1
            String parentModelId = result.getModelIds().get(1);
            File parentPomFile = result.getRawModel(parentModelId).getPomFile();
            MavenProject parent = projects.get(parentModelId);
            if (parent == null && buildParentIfNotExisting) {
                //
                // At this point the DefaultModelBuildingListener has fired and it populates the
                // remote repositories with those found in the pom.xml, along with the existing externally
                // defined repositories.
                //
                projectBuildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
                if (parentPomFile != null) {
                    project.setParentFile(parentPomFile);
                    try {
                        parent = build(parentPomFile, projectBuildingRequest).getProject();
                    } catch (ProjectBuildingException e) {
                        // MNG-4488 where let invalid parents slide on by
                        if (logger.isDebugEnabled()) {
                            // Message below is checked for in the MNG-2199 core IT.
                            logger.warn("Failed to build parent project for " + project.getId(), e);
                        } else {
                            // Message below is checked for in the MNG-2199 core IT.
                            logger.warn("Failed to build parent project for " + project.getId());
                        }
                    }
                } else {
                    Artifact parentArtifact = project.getParentArtifact();
                    try {
                        parent = build(parentArtifact, projectBuildingRequest).getProject();
                    } catch (ProjectBuildingException e) {
                        // MNG-4488 where let invalid parents slide on by
                        if (logger.isDebugEnabled()) {
                            // Message below is checked for in the MNG-2199 core IT.
                            logger.warn("Failed to build parent project for " + project.getId(), e);
                        } else {
                            // Message below is checked for in the MNG-2199 core IT.
                            logger.warn("Failed to build parent project for " + project.getId());
                        }
                    }
                }
            }
            project.setParent(parent);
            if (project.getParentFile() == null && parent != null) {
                project.setParentFile(parent.getFile());
            }
        }
    }

    private static String inheritedGroupId(final ModelBuildingResult result, final int modelIndex) {
        String groupId = null;
        final String modelId = result.getModelIds().get(modelIndex);

        if (!modelId.isEmpty()) {
            final Model model = result.getRawModel(modelId);
            groupId = model.getGroupId() != null ? model.getGroupId() : inheritedGroupId(result, modelIndex + 1);
        }

        return groupId;
    }

    private static String inheritedVersion(final ModelBuildingResult result, final int modelIndex) {
        String version = null;
        final String modelId = result.getModelIds().get(modelIndex);

        if (!modelId.isEmpty()) {
            final Model model = result.getRawModel(modelId);
            version = model.getVersion() != null ? model.getVersion() : inheritedVersion(result, modelIndex + 1);
        }

        return version;
    }

    private String findProfilesXml(ModelBuildingResult result, Map<File, Boolean> profilesXmls) {
        for (String modelId : result.getModelIds()) {
            Model model = result.getRawModel(modelId);

            File basedir = model.getProjectDirectory();
            if (basedir == null) {
                break;
            }

            Boolean profilesXml = profilesXmls.get(basedir);
            if (profilesXml == null) {
                profilesXml = new File(basedir, "profiles.xml").exists();
                profilesXmls.put(basedir, profilesXml);
            }
            if (profilesXml) {
                return modelId;
            }
        }

        return null;
    }

    /**
     * InternalConfig
     */
    class InternalConfig {

        private final ProjectBuildingRequest request;

        private final RepositorySystemSession session;

        private final List<RemoteRepository> repositories;

        private final ReactorModelPool modelPool;

        private final ModelCache modelCache;

        InternalConfig(ProjectBuildingRequest request, ReactorModelPool modelPool, ModelCache modelCache) {
            this.request = request;
            this.modelPool = modelPool;
            this.modelCache = modelCache;
            session = LegacyLocalRepositoryManager.overlay(
                    request.getLocalRepository(), request.getRepositorySession(), repoSystem);
            repositories = RepositoryUtils.toRepos(request.getRemoteRepositories());
        }
    }

    private ModelCache createModelCache(RepositorySystemSession session) {
        // MNG-7759: very old clients (Maven2 plugins) will not even have session, as setter was added in Maven 3
        if (session == null) {
            return null;
        }
        // MNG-7693: for older clients (not injecting ModelCacheFactory), make this work OOTB w/ defaults
        return modelCacheFactory != null
                ? modelCacheFactory.createCache(session)
                : new DefaultModelCacheFactory().createCache(session);
    }
}
