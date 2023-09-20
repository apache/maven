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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.ProjectCycleException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.feature.Features;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.building.ArtifactModelSource;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.building.TransformerContextBuilder;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.apache.maven.utils.Os;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultProjectBuilder
 */
@Named
@Singleton
public class DefaultProjectBuilder implements ProjectBuilder {
    public static final String BUILDER_PARALLELISM = "maven.projectBuilder.parallelism";
    public static final int DEFAULT_BUILDER_PARALLELISM = 4;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ModelBuilder modelBuilder;
    private final ModelProcessor modelProcessor;
    private final ProjectBuildingHelper projectBuildingHelper;
    private final MavenRepositorySystem repositorySystem;
    private final org.eclipse.aether.RepositorySystem repoSystem;
    private final RemoteRepositoryManager repositoryManager;
    private final ProjectDependenciesResolver dependencyResolver;
    private final ModelCacheFactory modelCacheFactory;

    private final RootLocator rootLocator;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    public DefaultProjectBuilder(
            ModelBuilder modelBuilder,
            ModelProcessor modelProcessor,
            ProjectBuildingHelper projectBuildingHelper,
            MavenRepositorySystem repositorySystem,
            RepositorySystem repoSystem,
            RemoteRepositoryManager repositoryManager,
            ProjectDependenciesResolver dependencyResolver,
            ModelCacheFactory modelCacheFactory,
            RootLocator rootLocator) {
        this.modelBuilder = modelBuilder;
        this.modelProcessor = modelProcessor;
        this.projectBuildingHelper = projectBuildingHelper;
        this.repositorySystem = repositorySystem;
        this.repoSystem = repoSystem;
        this.repositoryManager = repositoryManager;
        this.dependencyResolver = dependencyResolver;
        this.modelCacheFactory = modelCacheFactory;
        this.rootLocator = rootLocator;
    }
    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    @Override
    public ProjectBuildingResult build(File pomFile, ProjectBuildingRequest request) throws ProjectBuildingException {
        return new BuildSession(request, false).build(pomFile, new FileModelSource(pomFile));
    }

    @Override
    public ProjectBuildingResult build(ModelSource modelSource, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return new BuildSession(request, false).build(null, modelSource);
    }

    @Override
    public ProjectBuildingResult build(Artifact artifact, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return build(artifact, false, request);
    }

    @Override
    public ProjectBuildingResult build(Artifact artifact, boolean allowStubModel, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return new BuildSession(request, false).build(artifact, allowStubModel);
    }

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return new BuildSession(request, true).build(pomFiles, recursive);
    }

    static class InterimResult {

        File pomFile;

        ModelBuildingRequest request;

        ModelBuildingResult result;

        MavenProject project;

        boolean root;

        List<InterimResult> modules = Collections.emptyList();

        ProjectBuildingResult projectBuildingResult;

        InterimResult(
                File pomFile,
                ModelBuildingRequest request,
                ModelBuildingResult result,
                MavenProject project,
                boolean root) {
            this.pomFile = pomFile;
            this.request = request;
            this.result = result;
            this.project = project;
            this.root = root;
        }

        InterimResult(ModelBuildingRequest request, ProjectBuildingResult projectBuildingResult) {
            this.request = request;
            this.projectBuildingResult = projectBuildingResult;
            this.pomFile = projectBuildingResult.getPomFile();
            this.project = projectBuildingResult.getProject();
        }
    }

    class BuildSession {
        private final ProjectBuildingRequest request;
        private final RepositorySystemSession session;
        private final List<RemoteRepository> repositories;
        private final ReactorModelPool modelPool;
        private final TransformerContextBuilder transformerContextBuilder;
        private final ForkJoinPool forkJoinPool;

        BuildSession(ProjectBuildingRequest request, boolean localProjects) {
            this.request = request;
            this.session =
                    RepositoryUtils.overlay(request.getLocalRepository(), request.getRepositorySession(), repoSystem);
            this.repositories = RepositoryUtils.toRepos(request.getRemoteRepositories());
            if (localProjects) {
                this.modelPool = new ReactorModelPool();
                this.transformerContextBuilder = modelBuilder.newTransformerContextBuilder();
                this.forkJoinPool = new ForkJoinPool(getParallelism(request));
            } else {
                this.modelPool = null;
                this.transformerContextBuilder = null;
                this.forkJoinPool = null;
            }
        }

        private int getParallelism(ProjectBuildingRequest request) {
            int parallelism = DEFAULT_BUILDER_PARALLELISM;
            try {
                String str = request.getUserProperties().getProperty(BUILDER_PARALLELISM);
                if (str == null) {
                    str = request.getSystemProperties().getProperty(BUILDER_PARALLELISM);
                }
                if (str != null) {
                    parallelism = Integer.parseInt(str);
                }
            } catch (Exception e) {
                // ignore
            }
            return Math.max(1, Math.min(parallelism, Runtime.getRuntime().availableProcessors()));
        }

        ProjectBuildingResult build(File pomFile, ModelSource modelSource) throws ProjectBuildingException {
            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                MavenProject project = request.getProject();

                List<ModelProblem> modelProblems = null;
                Throwable error = null;

                if (project == null) {
                    ModelBuildingRequest request = getModelBuildingRequest();

                    project = new MavenProject();
                    project.setFile(pomFile);

                    DefaultModelBuildingListener listener =
                            new DefaultModelBuildingListener(project, projectBuildingHelper, this.request);
                    request.setModelBuildingListener(listener);

                    request.setPomFile(pomFile);
                    request.setModelSource(modelSource);
                    request.setLocationTracking(true);

                    if (pomFile != null) {
                        project.setRootDirectory(
                                rootLocator.findRoot(pomFile.getParentFile().toPath()));
                    }

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

                    initProject(project, Collections.emptyMap(), result);
                } else if (request.isResolveDependencies()) {
                    projectBuildingHelper.selectProjectRealm(project);
                }

                DependencyResolutionResult resolutionResult = null;

                if (request.isResolveDependencies()) {
                    resolutionResult = resolveDependencies(project);
                }

                ProjectBuildingResult result =
                        new DefaultProjectBuildingResult(project, modelProblems, resolutionResult);

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

        ProjectBuildingResult build(Artifact artifact, boolean allowStubModel) throws ProjectBuildingException {
            org.eclipse.aether.artifact.Artifact pomArtifact = RepositoryUtils.toArtifact(artifact);
            pomArtifact = ArtifactDescriptorUtils.toPomArtifact(pomArtifact);

            boolean localProject;

            try {
                ArtifactRequest pomRequest = new ArtifactRequest();
                pomRequest.setArtifact(pomArtifact);
                pomRequest.setRepositories(repositories);
                ArtifactResult pomResult = repoSystem.resolveArtifact(session, pomRequest);

                pomArtifact = pomResult.getArtifact();
                localProject = pomResult.getRepository() instanceof WorkspaceRepository;
            } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
                if (e.getResults().get(0).isMissing() && allowStubModel) {
                    return build(null, createStubModelSource(artifact));
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

            if (localProject) {
                return build(pomFile, new FileModelSource(pomFile));
            } else {
                return build(
                        null,
                        new ArtifactModelSource(
                                pomFile, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
            }
        }

        List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive) throws ProjectBuildingException {
            ForkJoinTask<List<ProjectBuildingResult>> task = forkJoinPool.submit(() -> doBuild(pomFiles, recursive));

            // ForkJoinTask.getException rewraps the exception in a weird way
            // which cause an additional layer of exception, so try to unwrap it
            task.quietlyJoin();
            if (task.isCompletedAbnormally()) {
                Throwable e = task.getException();
                Throwable c = e.getCause();
                uncheckedThrow(c != null && c.getClass() == e.getClass() ? c : e);
            }

            List<ProjectBuildingResult> results = task.getRawResult();
            if (results.stream()
                    .flatMap(r -> r.getProblems().stream())
                    .anyMatch(p -> p.getSeverity() != ModelProblem.Severity.WARNING)) {
                ModelProblem cycle = results.stream()
                        .flatMap(r -> r.getProblems().stream())
                        .filter(p -> p.getException() instanceof CycleDetectedException)
                        .findAny()
                        .orElse(null);
                if (cycle != null) {
                    throw new RuntimeException(new ProjectCycleException(
                            "The projects in the reactor contain a cyclic reference: " + cycle.getMessage(),
                            (CycleDetectedException) cycle.getException()));
                }
                throw new ProjectBuildingException(results);
            }

            return results;
        }

        List<ProjectBuildingResult> doBuild(List<File> pomFiles, boolean recursive) {
            Map<File, MavenProject> projectIndex = new ConcurrentHashMap<>(256);

            // phase 1: get file Models from the reactor.
            List<InterimResult> interimResults = build(projectIndex, pomFiles, new LinkedHashSet<>(), true, recursive);

            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                // Phase 2: get effective models from the reactor
                List<ProjectBuildingResult> results = build(projectIndex, interimResults);

                if (Features.buildConsumer(request.getUserProperties())) {
                    request.getRepositorySession()
                            .getData()
                            .set(TransformerContext.KEY, transformerContextBuilder.build());
                }

                return results;
            } finally {
                Thread.currentThread().setContextClassLoader(oldContextClassLoader);
            }
        }

        @SuppressWarnings("checkstyle:parameternumber")
        private List<InterimResult> build(
                Map<File, MavenProject> projectIndex,
                List<File> pomFiles,
                Set<File> aggregatorFiles,
                boolean root,
                boolean recursive) {
            List<ForkJoinTask<InterimResult>> tasks = pomFiles.stream()
                    .map(pomFile -> ForkJoinTask.adapt(
                            () -> build(projectIndex, pomFile, concat(aggregatorFiles, pomFile), root, recursive)))
                    .collect(Collectors.toList());

            return ForkJoinTask.invokeAll(tasks).stream()
                    .map(ForkJoinTask::getRawResult)
                    .collect(Collectors.toList());
        }

        private <T> Set<T> concat(Set<T> set, T elem) {
            Set<T> newSet = new HashSet<>(set);
            newSet.add(elem);
            return newSet;
        }

        @SuppressWarnings("checkstyle:parameternumber")
        private InterimResult build(
                Map<File, MavenProject> projectIndex,
                File pomFile,
                Set<File> aggregatorFiles,
                boolean isRoot,
                boolean recursive) {
            MavenProject project = new MavenProject();
            project.setFile(pomFile);

            project.setRootDirectory(
                    rootLocator.findRoot(pomFile.getParentFile().toPath()));

            ModelBuildingRequest modelBuildingRequest = getModelBuildingRequest()
                    .setPomFile(pomFile)
                    .setTwoPhaseBuilding(true)
                    .setLocationTracking(true);

            DefaultModelBuildingListener listener =
                    new DefaultModelBuildingListener(project, projectBuildingHelper, request);
            modelBuildingRequest.setModelBuildingListener(listener);

            ModelBuildingResult result;
            try {
                result = modelBuilder.build(modelBuildingRequest);
            } catch (ModelBuildingException e) {
                result = e.getResult();
                if (result == null || result.getFileModel() == null) {
                    return new InterimResult(
                            modelBuildingRequest,
                            new DefaultProjectBuildingResult(e.getModelId(), pomFile, e.getProblems()));
                }
                // validation error, continue project building and delay failing to help IDEs
                // result.getProblems().addAll(e.getProblems()) ?
            }

            Model model = modelBuildingRequest.getFileModel();

            modelPool.put(model.getPomFile().toPath(), model);

            InterimResult interimResult = new InterimResult(pomFile, modelBuildingRequest, result, project, isRoot);

            if (recursive) {
                File basedir = pomFile.getParentFile();
                List<File> moduleFiles = new ArrayList<>();
                for (String module : model.getModules()) {
                    if (module == null || module.isEmpty()) {
                        continue;
                    }

                    module = module.replace('\\', File.separatorChar).replace('/', File.separatorChar);

                    File moduleFile = modelProcessor.locateExistingPom(new File(basedir, module));

                    if (moduleFile == null) {
                        ModelProblem problem = new DefaultModelProblem(
                                "Child module " + moduleFile + " of " + pomFile + " does not exist",
                                ModelProblem.Severity.ERROR,
                                ModelProblem.Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        result.getProblems().add(problem);

                        continue;
                    }

                    if (Os.IS_WINDOWS) {
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

                        continue;
                    }

                    moduleFiles.add(moduleFile);
                }

                if (!moduleFiles.isEmpty()) {
                    interimResult.modules = build(projectIndex, moduleFiles, aggregatorFiles, false, recursive);
                }
            }

            projectIndex.put(pomFile, project);

            return interimResult;
        }

        private List<ProjectBuildingResult> build(
                Map<File, MavenProject> projectIndex, List<InterimResult> interimResults) {
            // The transformation may need to access dependencies raw models,
            // which may cause some re-entrance in the build() method and can
            // actually cause deadlocks.  In order to workaround the problem,
            // we do a first pass by reading all rawModels in order.
            if (modelBuilder instanceof DefaultModelBuilder) {
                List<ProjectBuildingResult> results = new ArrayList<>();
                DefaultModelBuilder dmb = (DefaultModelBuilder) modelBuilder;
                boolean failure = false;
                for (InterimResult r : interimResults) {
                    DefaultProjectBuildingResult res;
                    try {
                        Model model = dmb.buildRawModel(r.request);
                        res = new DefaultProjectBuildingResult(model.getId(), model.getPomFile(), null);
                    } catch (ModelBuildingException e) {
                        failure = true;
                        res = new DefaultProjectBuildingResult(e.getModelId(), r.request.getPomFile(), e.getProblems());
                    }
                    results.add(res);
                }
                if (failure) {
                    return results;
                }
            }

            return interimResults.parallelStream()
                    .map(interimResult -> doBuild(projectIndex, interimResult))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        private List<ProjectBuildingResult> doBuild(Map<File, MavenProject> projectIndex, InterimResult interimResult) {
            if (interimResult.projectBuildingResult != null) {
                return Collections.singletonList(interimResult.projectBuildingResult);
            }
            MavenProject project = interimResult.project;
            try {
                ModelBuildingResult result = modelBuilder.build(interimResult.request, interimResult.result);

                // 2nd pass of initialization: resolve and build parent if necessary
                try {
                    initProject(project, projectIndex, result);
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

                List<ProjectBuildingResult> results = build(projectIndex, interimResult.modules);

                project.setExecutionRoot(interimResult.root);
                project.setCollectedProjects(
                        results.stream().map(ProjectBuildingResult::getProject).collect(Collectors.toList()));
                DependencyResolutionResult resolutionResult = null;
                if (request.isResolveDependencies()) {
                    resolutionResult = resolveDependencies(project);
                }

                results.add(new DefaultProjectBuildingResult(project, result.getProblems(), resolutionResult));

                return results;
            } catch (ModelBuildingException e) {
                DefaultProjectBuildingResult result;
                if (project == null || interimResult.result.getEffectiveModel() == null) {
                    result = new DefaultProjectBuildingResult(e.getModelId(), interimResult.pomFile, e.getProblems());
                } else {
                    project.setModel(interimResult.result.getEffectiveModel());
                    result = new DefaultProjectBuildingResult(project, e.getProblems(), null);
                }
                return Collections.singletonList(result);
            }
        }

        @SuppressWarnings("checkstyle:methodlength")
        private void initProject(MavenProject project, Map<File, MavenProject> projects, ModelBuildingResult result) {
            project.setModel(result.getEffectiveModel());
            project.setOriginalModel(result.getFileModel());

            initParent(project, projects, result);

            Artifact projectArtifact = repositorySystem.createArtifact(
                    project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging());
            project.setArtifact(projectArtifact);

            // only set those on 2nd phase, ignore on 1st pass
            if (project.getFile() != null) {
                Build build = project.getBuild();
                project.addScriptSourceRoot(build.getScriptSourceDirectory());
                project.addCompileSourceRoot(build.getSourceDirectory());
                project.addTestCompileSourceRoot(build.getTestSourceDirectory());
            }

            List<Profile> activeProfiles = new ArrayList<>();
            activeProfiles.addAll(
                    result.getActivePomProfiles(result.getModelIds().get(0)));
            activeProfiles.addAll(result.getActiveExternalProfiles());
            project.setActiveProfiles(activeProfiles);

            project.setInjectedProfileIds("external", getProfileIds(result.getActiveExternalProfiles()));
            for (String modelId : result.getModelIds()) {
                project.setInjectedProfileIds(modelId, getProfileIds(result.getActivePomProfiles(modelId)));
            }

            //
            // All the parts that were taken out of MavenProject for Maven 4.0.0
            //

            project.setProjectBuildingRequest(request);

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
                    if (ext.getVersion() == null || ext.getVersion().isEmpty()) {
                        version = "RELEASE";
                    } else {
                        version = ext.getVersion();
                    }

                    Artifact artifact = repositorySystem.createArtifact(
                            ext.getGroupId(), ext.getArtifactId(), version, null, "jar");

                    if (artifact != null) {
                        extensionArtifacts.add(artifact);
                    }
                }
            }
            project.setExtensionArtifacts(extensionArtifacts);

            // managedVersionMap
            Map<String, Artifact> map = Collections.emptyMap();
            final DependencyManagement dependencyManagement = project.getDependencyManagement();
            if (dependencyManagement != null
                    && dependencyManagement.getDependencies() != null
                    && !dependencyManagement.getDependencies().isEmpty()) {
                map = new LazyMap<>(() -> {
                    Map<String, Artifact> tmp = new HashMap<>();
                    for (Dependency d : dependencyManagement.getDependencies()) {
                        Artifact artifact = repositorySystem.createDependencyArtifact(d);
                        if (artifact != null) {
                            tmp.put(d.getManagementKey(), artifact);
                        }
                    }
                    return Collections.unmodifiableMap(tmp);
                });
            }
            project.setManagedVersionMap(map);

            // release artifact repository
            if (project.getDistributionManagement() != null
                    && project.getDistributionManagement().getRepository() != null) {
                try {
                    DeploymentRepository r = project.getDistributionManagement().getRepository();
                    if (r.getId() != null
                            && !r.getId().isEmpty()
                            && r.getUrl() != null
                            && !r.getUrl().isEmpty()) {
                        ArtifactRepository repo = MavenRepositorySystem.buildArtifactRepository(r);
                        repositorySystem.injectProxy(request.getRepositorySession(), Arrays.asList(repo));
                        repositorySystem.injectAuthentication(request.getRepositorySession(), Arrays.asList(repo));
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
                    if (r.getId() != null
                            && !r.getId().isEmpty()
                            && r.getUrl() != null
                            && !r.getUrl().isEmpty()) {
                        ArtifactRepository repo = MavenRepositorySystem.buildArtifactRepository(r);
                        repositorySystem.injectProxy(request.getRepositorySession(), Arrays.asList(repo));
                        repositorySystem.injectAuthentication(request.getRepositorySession(), Arrays.asList(repo));
                        project.setSnapshotArtifactRepository(repo);
                    }
                } catch (InvalidRepositoryException e) {
                    throw new IllegalStateException(
                            "Failed to create snapshot distribution repository for " + project.getId(), e);
                }
            }
        }

        private void initParent(MavenProject project, Map<File, MavenProject> projects, ModelBuildingResult result) {
            Model parentModel = result.getModelIds().size() > 1
                            && !result.getModelIds().get(1).isEmpty()
                    ? result.getRawModel(result.getModelIds().get(1))
                    : null;

            if (parentModel != null) {
                final String parentGroupId = inheritedGroupId(result, 1);
                final String parentVersion = inheritedVersion(result, 1);

                project.setParentArtifact(repositorySystem.createProjectArtifact(
                        parentGroupId, parentModel.getArtifactId(), parentVersion));

                // org.apache.maven.its.mng4834:parent:0.1
                String parentModelId = result.getModelIds().get(1);
                File parentPomFile = result.getRawModel(parentModelId).getPomFile();
                MavenProject parent = parentPomFile != null ? projects.get(parentPomFile) : null;
                if (parent == null) {
                    //
                    // At this point the DefaultModelBuildingListener has fired and it populates the
                    // remote repositories with those found in the pom.xml, along with the existing externally
                    // defined repositories.
                    //
                    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
                    if (parentPomFile != null) {
                        project.setParentFile(parentPomFile);
                        try {
                            parent = build(parentPomFile, new FileModelSource(parentPomFile))
                                    .getProject();
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
                            parent = build(parentArtifact, false).getProject();
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

        private ModelBuildingRequest getModelBuildingRequest() {
            ModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest();

            RequestTrace trace = RequestTrace.newChild(null, request).newChild(modelBuildingRequest);

            ModelResolver resolver = new ProjectModelResolver(
                    session,
                    trace,
                    repoSystem,
                    repositoryManager,
                    repositories,
                    request.getRepositoryMerging(),
                    modelPool);

            modelBuildingRequest.setValidationLevel(request.getValidationLevel());
            modelBuildingRequest.setProcessPlugins(request.isProcessPlugins());
            modelBuildingRequest.setProfiles(request.getProfiles());
            modelBuildingRequest.setActiveProfileIds(request.getActiveProfileIds());
            modelBuildingRequest.setInactiveProfileIds(request.getInactiveProfileIds());
            modelBuildingRequest.setSystemProperties(request.getSystemProperties());
            modelBuildingRequest.setUserProperties(request.getUserProperties());
            modelBuildingRequest.setBuildStartTime(request.getBuildStartTime());
            modelBuildingRequest.setModelResolver(resolver);
            // this is a hint that we want to build 1 file, so don't cache. See MNG-7063
            if (modelPool != null) {
                modelBuildingRequest.setModelCache(modelCacheFactory.createCache(session));
            }
            modelBuildingRequest.setTransformerContextBuilder(transformerContextBuilder);
            DefaultSession session = (DefaultSession) this.session.getData().get(DefaultSession.class);
            if (session != null) {
                try {
                    modelBuildingRequest.setRootDirectory(session.getRootDirectory());
                } catch (IllegalStateException e) {
                    // can happen if root directory cannot be found, just ignore
                }
            }

            return modelBuildingRequest;
        }

        private DependencyResolutionResult resolveDependencies(MavenProject project) {
            DependencyResolutionResult resolutionResult;

            try {
                DefaultDependencyResolutionRequest resolution =
                        new DefaultDependencyResolutionRequest(project, session);
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
    }

    private List<String> getProfileIds(List<org.apache.maven.model.Profile> profiles) {
        return profiles.stream().map(org.apache.maven.model.Profile::getId).collect(Collectors.toList());
    }

    private static ModelSource createStubModelSource(Artifact artifact) {
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
            version = result.getRawModel(modelId).getVersion();
            if (version == null) {
                version = inheritedVersion(result, modelIndex + 1);
            }
        }

        return version;
    }

    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t; // rely on vacuous cast
    }

    static class LazyMap<K, V> extends AbstractMap<K, V> {
        private final Supplier<Map<K, V>> supplier;
        private volatile Map<K, V> delegate;

        LazyMap(Supplier<Map<K, V>> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            if (delegate == null) {
                synchronized (this) {
                    if (delegate == null) {
                        delegate = supplier.get();
                    }
                }
            }
            return delegate.entrySet();
        }
    }
}
