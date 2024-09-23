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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.ProjectCycleException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DeploymentRepository;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.model.ModelBuildingEvent;
import org.apache.maven.api.services.model.ModelBuildingListener;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.building.ModelSource3;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
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

    public static final int DEFAULT_BUILDER_PARALLELISM = Runtime.getRuntime().availableProcessors() / 2 + 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ModelBuilder modelBuilder;
    private final ModelProcessor modelProcessor;
    private final ProjectBuildingHelper projectBuildingHelper;
    private final MavenRepositorySystem repositorySystem;
    private final org.eclipse.aether.RepositorySystem repoSystem;
    private final RemoteRepositoryManager repositoryManager;
    private final ProjectDependenciesResolver dependencyResolver;
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
            RootLocator rootLocator) {
        this.modelBuilder = modelBuilder;
        this.modelProcessor = modelProcessor;
        this.projectBuildingHelper = projectBuildingHelper;
        this.repositorySystem = repositorySystem;
        this.repoSystem = repoSystem;
        this.repositoryManager = repositoryManager;
        this.dependencyResolver = dependencyResolver;
        this.rootLocator = rootLocator;
    }
    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    @Override
    public ProjectBuildingResult build(File pomFile, ProjectBuildingRequest request) throws ProjectBuildingException {
        try (BuildSession bs = new BuildSession(request, false)) {
            Path path = pomFile.toPath();
            return bs.build(path, ModelSource.fromPath(path));
        }
    }

    @Deprecated
    @Override
    public ProjectBuildingResult build(
            org.apache.maven.model.building.ModelSource modelSource, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return build(toSource(modelSource), request);
    }

    @Deprecated
    static ModelSource toSource(org.apache.maven.model.building.ModelSource modelSource) {
        if (modelSource instanceof FileModelSource fms) {
            return ModelSource.fromPath(fms.getPath());
        } else {
            return new WrapModelSource(modelSource);
        }
    }

    @Override
    public ProjectBuildingResult build(ModelSource modelSource, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        try (BuildSession bs = new BuildSession(request, false)) {
            return bs.build(null, modelSource);
        }
    }

    @Override
    public ProjectBuildingResult build(Artifact artifact, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return build(artifact, false, request);
    }

    @Override
    public ProjectBuildingResult build(Artifact artifact, boolean allowStubModel, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        try (BuildSession bs = new BuildSession(request, false)) {
            return bs.build(artifact, allowStubModel);
        }
    }

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        try (BuildSession bs = new BuildSession(request, true)) {
            return bs.build(pomFiles, recursive);
        }
    }

    static class InterimResult {

        File pomFile;

        ModelBuilderRequest request;

        ModelBuilderResult result;

        MavenProject project;

        boolean root;

        List<InterimResult> subprojects = Collections.emptyList();

        ProjectBuildingResult projectBuildingResult;

        InterimResult(
                File pomFile,
                ModelBuilderRequest request,
                ModelBuilderResult result,
                MavenProject project,
                boolean root) {
            this.pomFile = pomFile;
            this.request = request;
            this.result = result;
            this.project = project;
            this.root = root;
        }

        InterimResult(ModelBuilderRequest request, ProjectBuildingResult projectBuildingResult) {
            this.request = request;
            this.projectBuildingResult = projectBuildingResult;
            this.pomFile = projectBuildingResult.getPomFile();
            this.project = projectBuildingResult.getProject();
        }
    }

    private static class StubModelSource implements ModelSource {
        private final String xml;
        private final Artifact artifact;

        StubModelSource(String xml, Artifact artifact) {
            this.xml = xml;
            this.artifact = artifact;
        }

        @Override
        public ModelSource resolve(ModelLocator modelLocator, String relative) {
            return null;
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String getLocation() {
            return artifact.getId();
        }

        @Override
        public Source resolve(String relative) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StubModelSource that = (StubModelSource) o;
            return Objects.equals(xml, that.xml) && Objects.equals(artifact, that.artifact);
        }

        @Override
        public int hashCode() {
            return Objects.hash(xml, artifact);
        }
    }

    private static class WrapModelSource implements ModelSource {
        private final org.apache.maven.model.building.ModelSource modelSource;

        WrapModelSource(org.apache.maven.model.building.ModelSource modelSource) {
            this.modelSource = modelSource;
        }

        @Override
        public ModelSource resolve(ModelLocator modelLocator, String relative) {
            if (modelSource instanceof ModelSource3 ms) {
                return toSource(ms.getRelatedSource(
                        new org.apache.maven.model.locator.ModelLocator() {
                            @Override
                            public File locatePom(File projectDirectory) {
                                return null;
                            }

                            @Override
                            public Path locatePom(Path projectDirectory) {
                                return null;
                            }

                            @Override
                            public Path locateExistingPom(Path project) {
                                return modelLocator.locateExistingPom(project);
                            }
                        },
                        relative));
            }
            return null;
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public InputStream openStream() throws IOException {
            return modelSource.getInputStream();
        }

        @Override
        public String getLocation() {
            return modelSource.getLocation();
        }

        @Override
        public Source resolve(String relative) {
            if (modelSource instanceof ModelSource2 ms) {
                return toSource(ms.getRelatedSource(relative));
            } else {
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            WrapModelSource that = (WrapModelSource) o;
            return Objects.equals(modelSource, that.modelSource);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(modelSource);
        }
    }

    class BuildSession implements AutoCloseable {
        private final ProjectBuildingRequest request;
        private final RepositorySystemSession session;
        private final ExecutorService executor;
        private final ModelBuilder.ModelBuilderSession modelBuilderSession;

        BuildSession(ProjectBuildingRequest request, boolean localProjects) {
            this.request = request;
            this.session =
                    RepositoryUtils.overlay(request.getLocalRepository(), request.getRepositorySession(), repoSystem);
            InternalSession.from(session);
            this.executor = createExecutor(getParallelism(request));
            this.modelBuilderSession = modelBuilder.newSession();
        }

        ExecutorService createExecutor(int parallelism) {
            //
            // We need an executor that will not block.
            // We can't use work stealing, as we are building a graph
            // and this could lead to cycles where a thread waits for
            // a task to finish, then execute another one which waits
            // for the initial task...
            // In order to work around that problem, we override the
            // invokeAll method, so that whenever the method is called,
            // the pool core size will be incremented before submitting
            // all the tasks, then the thread will block waiting for all
            // those subtasks to finish.
            // This ensures the number of running workers is no more than
            // the defined parallism, while making sure the pool will not
            // be exhausted
            //
            return new ThreadPoolExecutor(
                    parallelism, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
                final AtomicInteger parked = new AtomicInteger();

                @Override
                public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                        throws InterruptedException {
                    setCorePoolSize(parallelism + parked.incrementAndGet());
                    try {
                        return super.invokeAll(tasks);
                    } finally {
                        setCorePoolSize(parallelism + parked.decrementAndGet());
                    }
                }
            };
        }

        @Override
        public void close() {
            this.executor.shutdownNow();
        }

        private int getParallelism(ProjectBuildingRequest request) {
            int parallelism = DEFAULT_BUILDER_PARALLELISM;
            try {
                String str = request.getUserProperties().getProperty(Constants.MAVEN_PROJECT_BUILDER_PARALLELISM);
                if (str != null) {
                    parallelism = Integer.parseInt(str);
                }
            } catch (Exception e) {
                // ignore
            }
            return Math.max(1, Math.min(parallelism, Runtime.getRuntime().availableProcessors()));
        }

        ProjectBuildingResult build(Path pomFile, ModelSource modelSource) throws ProjectBuildingException {
            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                MavenProject project = request.getProject();

                List<ModelProblem> modelProblems = null;
                Throwable error = null;

                if (project == null) {
                    project = new MavenProject();
                    project.setFile(pomFile != null ? pomFile.toFile() : null);

                    ModelBuildingListener listener =
                            new DefaultModelBuildingListener(project, projectBuildingHelper, this.request);

                    ModelBuilderRequest.ModelBuilderRequestBuilder builder = getModelBuildingRequest();
                    ModelBuilderRequest request = builder.source(modelSource)
                            .requestType(
                                    pomFile != null
                                                    && this.request.isProcessPlugins()
                                                    && this.request.getValidationLevel()
                                                            == ModelBuildingRequest.VALIDATION_LEVEL_STRICT
                                            ? ModelBuilderRequest.RequestType.BUILD_POM
                                            : ModelBuilderRequest.RequestType.DEPENDENCY)
                            .locationTracking(true)
                            .listener(listener)
                            .build();

                    if (pomFile != null) {
                        project.setRootDirectory(rootLocator.findRoot(pomFile.getParent()));
                    }

                    ModelBuilderResult result;
                    try {
                        result = modelBuilderSession.build(request);
                    } catch (ModelBuilderException e) {
                        result = e.getResult();
                        if (result == null || result.getEffectiveModel() == null) {
                            throw new ProjectBuildingException(
                                    e.getModelId(), e.getMessage(), pomFile != null ? pomFile.toFile() : null, e);
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
                        new DefaultProjectBuildingResult(project, convert(modelProblems), resolutionResult);

                if (error != null) {
                    ProjectBuildingException e = new ProjectBuildingException(List.of(result));
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
                pomRequest.setRepositories(RepositoryUtils.toRepos(request.getRemoteRepositories()));
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

            Path pomFile = pomArtifact.getPath();

            if (!artifact.isResolved() && "pom".equals(artifact.getType())) {
                artifact.selectVersion(pomArtifact.getVersion());
                artifact.setFile(pomFile.toFile());
                artifact.setResolved(true);
            }

            if (localProject) {
                return build(pomFile, ModelSource.fromPath(pomFile));
            } else {
                return build(
                        null,
                        ModelSource.fromPath(
                                pomFile,
                                artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion()));
            }
        }

        List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive) throws ProjectBuildingException {
            List<ProjectBuildingResult> results = doBuild(pomFiles, recursive);
            if (results.stream()
                    .flatMap(r -> r.getProblems().stream())
                    .anyMatch(p -> p.getSeverity() != org.apache.maven.model.building.ModelProblem.Severity.WARNING)) {
                org.apache.maven.model.building.ModelProblem cycle = results.stream()
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
                return build(projectIndex, interimResults);
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
            List<Callable<InterimResult>> tasks = pomFiles.stream()
                    .map(pomFile -> ((Callable<InterimResult>)
                            () -> build(projectIndex, pomFile, concat(aggregatorFiles, pomFile), root, recursive)))
                    .collect(Collectors.toList());
            try {
                List<Future<InterimResult>> futures = executor.invokeAll(tasks);
                List<InterimResult> list = new ArrayList<>();
                for (Future<InterimResult> future : futures) {
                    InterimResult interimResult = future.get();
                    list.add(interimResult);
                }
                return list;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

            ModelBuildingListener listener = new DefaultModelBuildingListener(project, projectBuildingHelper, request);

            ModelBuilderRequest modelBuildingRequest = getModelBuildingRequest()
                    .source(ModelSource.fromPath(pomFile.toPath()))
                    .requestType(ModelBuilderRequest.RequestType.BUILD_POM)
                    .twoPhaseBuilding(true)
                    .locationTracking(true)
                    .listener(listener)
                    .recursive(recursive)
                    .build();

            ModelBuilderResult result;
            try {
                result = modelBuilderSession.build(modelBuildingRequest);
            } catch (ModelBuilderException e) {
                result = e.getResult();
                if (result == null || result.getFileModel() == null) {
                    return new InterimResult(
                            modelBuildingRequest,
                            new DefaultProjectBuildingResult(e.getModelId(), pomFile, convert(e.getProblems())));
                }
                // validation error, continue project building and delay failing to help IDEs
                // result.getProblems().addAll(e.getProblems()) ?
            }

            InterimResult interimResult = new InterimResult(pomFile, modelBuildingRequest, result, project, isRoot);
            interimResult.subprojects = result.getChildren().stream()
                    .map(r -> build(projectIndex, r.getFileModel().getPomFile().toFile(), aggregatorFiles, false, true))
                    .toList();
            /*
            Model model = result.getActivatedFileModel();

            if (recursive && model != null) {
                File basedir = pomFile.getParentFile();
                List<String> subprojects = model.getSubprojects();
                if (subprojects.isEmpty()) {
                    subprojects = model.getModules();
                }
                List<File> subprojectFiles = new ArrayList<>();
                for (String subproject : subprojects) {
                    if (subproject == null || subproject.isEmpty()) {
                        continue;
                    }

                    subproject = subproject.replace('\\', File.separatorChar).replace('/', File.separatorChar);

                    Path subprojectPath = modelProcessor.locateExistingPom(new File(basedir, subproject).toPath());
                    File subprojectFile = subprojectPath != null ? subprojectPath.toFile() : null;

                    if (subprojectFile == null) {
                        ModelProblem problem = new org.apache.maven.internal.impl.model.DefaultModelProblem(
                                "Child subproject " + subprojectFile + " of " + pomFile + " does not exist",
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
                            subprojectFile = subprojectFile.getCanonicalFile();
                        } catch (IOException e) {
                            subprojectFile = subprojectFile.getAbsoluteFile();
                        }
                    } else {
                        subprojectFile = new File(subprojectFile.toURI().normalize());
                    }

                    if (aggregatorFiles.contains(subprojectFile)) {
                        StringBuilder buffer = new StringBuilder(256);
                        for (File aggregatorFile : aggregatorFiles) {
                            buffer.append(aggregatorFile).append(" -> ");
                        }
                        buffer.append(subprojectFile);

                        ModelProblem problem = new org.apache.maven.internal.impl.model.DefaultModelProblem(
                                "Child subproject " + subprojectFile + " of " + pomFile + " forms aggregation cycle "
                                        + buffer,
                                ModelProblem.Severity.ERROR,
                                ModelProblem.Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        result.getProblems().add(problem);

                        continue;
                    }

                    subprojectFiles.add(subprojectFile);
                }

                if (!subprojectFiles.isEmpty()) {
                    interimResult.subprojects = build(projectIndex, subprojectFiles, aggregatorFiles, false, recursive);
                }
            }
             */

            projectIndex.put(pomFile, project);

            return interimResult;
        }

        private List<ProjectBuildingResult> build(
                Map<File, MavenProject> projectIndex, List<InterimResult> interimResults) {
            return interimResults.stream()
                    .map(ir -> doBuild(projectIndex, ir))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        private List<ProjectBuildingResult> doBuild(Map<File, MavenProject> projectIndex, InterimResult interimResult) {
            if (interimResult.projectBuildingResult != null) {
                return Collections.singletonList(interimResult.projectBuildingResult);
            }
            MavenProject project = interimResult.project;
            try {
                ModelBuilderResult result = interimResult.result;

                // 2nd pass of initialization: resolve and build parent if necessary
                List<org.apache.maven.model.building.ModelProblem> problems = convert(result.getProblems());
                try {
                    initProject(project, projectIndex, result);
                } catch (InvalidArtifactRTException iarte) {
                    problems.add(new DefaultModelProblem(
                            null,
                            org.apache.maven.model.building.ModelProblem.Severity.ERROR,
                            null,
                            new org.apache.maven.model.Model(result.getEffectiveModel()),
                            -1,
                            -1,
                            iarte));
                }

                List<ProjectBuildingResult> results = build(projectIndex, interimResult.subprojects);

                project.setExecutionRoot(interimResult.root);
                project.setCollectedProjects(
                        results.stream().map(ProjectBuildingResult::getProject).collect(Collectors.toList()));
                DependencyResolutionResult resolutionResult = null;
                if (request.isResolveDependencies()) {
                    resolutionResult = resolveDependencies(project);
                }

                results.add(new DefaultProjectBuildingResult(project, problems, resolutionResult));

                return results;
            } catch (ModelBuilderException e) {
                DefaultProjectBuildingResult result;
                if (project == null || interimResult.result.getEffectiveModel() == null) {
                    result = new DefaultProjectBuildingResult(
                            e.getModelId(), interimResult.pomFile, convert(e.getProblems()));
                } else {
                    project.setModel(new org.apache.maven.model.Model(interimResult.result.getEffectiveModel()));
                    result = new DefaultProjectBuildingResult(project, convert(e.getProblems()), null);
                }
                return Collections.singletonList(result);
            }
        }

        private List<org.apache.maven.model.building.ModelProblem> convert(List<ModelProblem> problems) {
            if (problems == null) {
                return null;
            }
            return problems.stream()
                    .map(p -> (org.apache.maven.model.building.ModelProblem) new DefaultModelProblem(
                            p.getMessage(),
                            org.apache.maven.model.building.ModelProblem.Severity.valueOf(
                                    p.getSeverity().name()),
                            org.apache.maven.model.building.ModelProblem.Version.valueOf(
                                    p.getVersion().name()),
                            p.getSource(),
                            p.getLineNumber(),
                            p.getColumnNumber(),
                            p.getModelId(),
                            p.getException()))
                    .toList();
        }

        @SuppressWarnings({"checkstyle:methodlength", "deprecation"})
        private void initProject(MavenProject project, Map<File, MavenProject> projects, ModelBuilderResult result) {
            project.setModel(new org.apache.maven.model.Model(result.getEffectiveModel()));
            project.setOriginalModel(new org.apache.maven.model.Model(result.getFileModel()));

            initParent(project, projects, result);

            Artifact projectArtifact = repositorySystem.createArtifact(
                    project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging());
            project.setArtifact(projectArtifact);

            // only set those on 2nd phase, ignore on 1st pass
            if (project.getFile() != null) {
                Build build = project.getBuild().getDelegate();
                project.addScriptSourceRoot(build.getScriptSourceDirectory());
                project.addCompileSourceRoot(build.getSourceDirectory());
                project.addTestCompileSourceRoot(build.getTestSourceDirectory());
            }

            project.setActiveProfiles(Stream.concat(
                            result.getActivePomProfiles(result.getModelIds().get(0)).stream(),
                            result.getActiveExternalProfiles().stream())
                    .map(org.apache.maven.model.Profile::new)
                    .toList());

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
            for (Plugin plugin : project.getModel().getDelegate().getBuild().getPlugins()) {
                Artifact artifact = repositorySystem.createPluginArtifact(new org.apache.maven.model.Plugin(plugin));

                if (artifact != null) {
                    pluginArtifacts.add(artifact);
                }
            }
            project.setPluginArtifacts(pluginArtifacts);

            // reportArtifacts
            Set<Artifact> reportArtifacts = new HashSet<>();
            for (ReportPlugin report :
                    project.getModel().getDelegate().getReporting().getPlugins()) {
                Plugin pp = Plugin.newBuilder()
                        .groupId(report.getGroupId())
                        .artifactId(report.getArtifactId())
                        .version(report.getVersion())
                        .build();

                Artifact artifact = repositorySystem.createPluginArtifact(new org.apache.maven.model.Plugin(pp));

                if (artifact != null) {
                    reportArtifacts.add(artifact);
                }
            }
            project.setReportArtifacts(reportArtifacts);

            // extensionArtifacts
            Set<Artifact> extensionArtifacts = new HashSet<>();
            List<Extension> extensions =
                    project.getModel().getDelegate().getBuild().getExtensions();
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
            final DependencyManagement dependencyManagement =
                    project.getModel().getDelegate().getDependencyManagement();
            if (dependencyManagement != null
                    && dependencyManagement.getDependencies() != null
                    && !dependencyManagement.getDependencies().isEmpty()) {
                map = new LazyMap<>(() -> {
                    Map<String, Artifact> tmp = new HashMap<>();
                    for (Dependency d : dependencyManagement.getDependencies()) {
                        Artifact artifact =
                                repositorySystem.createDependencyArtifact(new org.apache.maven.model.Dependency(d));
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
                    DeploymentRepository r = project.getModel()
                            .getDelegate()
                            .getDistributionManagement()
                            .getRepository();
                    if (r.getId() != null
                            && !r.getId().isEmpty()
                            && r.getUrl() != null
                            && !r.getUrl().isEmpty()) {
                        ArtifactRepository repo = MavenRepositorySystem.buildArtifactRepository(
                                new org.apache.maven.model.DeploymentRepository(r));
                        repositorySystem.injectProxy(request.getRepositorySession(), List.of(repo));
                        repositorySystem.injectAuthentication(request.getRepositorySession(), List.of(repo));
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
                    DeploymentRepository r = project.getModel()
                            .getDelegate()
                            .getDistributionManagement()
                            .getSnapshotRepository();
                    if (r.getId() != null
                            && !r.getId().isEmpty()
                            && r.getUrl() != null
                            && !r.getUrl().isEmpty()) {
                        ArtifactRepository repo = MavenRepositorySystem.buildArtifactRepository(
                                new org.apache.maven.model.DeploymentRepository(r));
                        repositorySystem.injectProxy(request.getRepositorySession(), List.of(repo));
                        repositorySystem.injectAuthentication(request.getRepositorySession(), List.of(repo));
                        project.setSnapshotArtifactRepository(repo);
                    }
                } catch (InvalidRepositoryException e) {
                    throw new IllegalStateException(
                            "Failed to create snapshot distribution repository for " + project.getId(), e);
                }
            }
        }

        private void initParent(MavenProject project, Map<File, MavenProject> projects, ModelBuilderResult result) {
            Model parentModel = result.getModelIds().size() > 1
                            && !result.getModelIds().get(1).isEmpty()
                    ? result.getRawModel(result.getModelIds().get(1)).orElse(null)
                    : null;

            if (parentModel != null) {
                final String parentGroupId = inheritedGroupId(result, 1);
                final String parentVersion = inheritedVersion(result, 1);

                project.setParentArtifact(repositorySystem.createProjectArtifact(
                        parentGroupId, parentModel.getArtifactId(), parentVersion));

                // org.apache.maven.its.mng4834:parent:0.1
                String parentModelId = result.getModelIds().get(1);
                Path parentPomFile =
                        result.getRawModel(parentModelId).map(Model::getPomFile).orElse(null);
                MavenProject parent = parentPomFile != null ? projects.get(parentPomFile.toFile()) : null;
                if (parent == null) {
                    //
                    // At this point the DefaultModelBuildingListener has fired and it populates the
                    // remote repositories with those found in the pom.xml, along with the existing externally
                    // defined repositories.
                    //
                    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
                    if (parentPomFile != null) {
                        project.setParentFile(parentPomFile.toFile());
                        try {
                            parent = build(parentPomFile, ModelSource.fromPath(parentPomFile))
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

        private ModelBuilderRequest.ModelBuilderRequestBuilder getModelBuildingRequest() {
            ModelBuilderRequest.ModelBuilderRequestBuilder modelBuildingRequest = ModelBuilderRequest.builder();

            InternalSession internalSession = InternalSession.from(session);
            modelBuildingRequest.session(internalSession);
            modelBuildingRequest.requestType(ModelBuilderRequest.RequestType.BUILD_POM);
            modelBuildingRequest.profiles(
                    request.getProfiles() != null
                            ? request.getProfiles().stream()
                                    .map(org.apache.maven.model.Profile::getDelegate)
                                    .toList()
                            : null);
            modelBuildingRequest.activeProfileIds(request.getActiveProfileIds());
            modelBuildingRequest.inactiveProfileIds(request.getInactiveProfileIds());
            modelBuildingRequest.systemProperties(toMap(request.getSystemProperties()));
            modelBuildingRequest.userProperties(toMap(request.getUserProperties()));
            // bv4: modelBuildingRequest.setBuildStartTime(request.getBuildStartTime());
            modelBuildingRequest.repositoryMerging(ModelBuilderRequest.RepositoryMerging.valueOf(
                    request.getRepositoryMerging().name()));
            modelBuildingRequest.repositories(request.getRemoteRepositories().stream()
                    .map(r -> internalSession.getRemoteRepository(RepositoryUtils.toRepo(r)))
                    .toList());
            /* TODO: bv4
            InternalMavenSession session =
                    (InternalMavenSession) this.session.getData().get(InternalMavenSession.class);
            if (session != null) {
                try {
                    modelBuildingRequest.setRootDirectory(session.getRootDirectory());
                } catch (IllegalStateException e) {
                    // can happen if root directory cannot be found, just ignore
                }
            }
            */

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
                        artifact.setFile(
                                lrm.getRepository().getBasePath().resolve(path).toFile());
                    }
                }
            }
            project.setResolvedArtifacts(artifacts);
            project.setArtifacts(artifacts);

            return resolutionResult;
        }
    }

    private List<String> getProfileIds(List<Profile> profiles) {
        return profiles.stream().map(Profile::getId).collect(Collectors.toList());
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

        String xml = buffer.toString();

        return new StubModelSource(xml, artifact);
    }

    private static String inheritedGroupId(final ModelBuilderResult result, final int modelIndex) {
        String groupId = null;
        final String modelId = result.getModelIds().get(modelIndex);

        if (!modelId.isEmpty()) {
            final Model model = result.getRawModel(modelId).orElseThrow();
            groupId = model.getGroupId() != null ? model.getGroupId() : inheritedGroupId(result, modelIndex + 1);
        }

        return groupId;
    }

    private static String inheritedVersion(final ModelBuilderResult result, final int modelIndex) {
        String version = null;
        final String modelId = result.getModelIds().get(modelIndex);

        if (!modelId.isEmpty()) {
            version = result.getRawModel(modelId).map(Model::getVersion).orElse(null);
            if (version == null) {
                version = inheritedVersion(result, modelIndex + 1);
            }
        }

        return version;
    }

    private static Map<String, String> toMap(Properties properties) {
        if (properties != null && !properties.isEmpty()) {
            return properties.entrySet().stream()
                    .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));

        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
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

    protected abstract class ModelResolverWrapper implements ModelResolver {

        protected abstract org.apache.maven.model.resolution.ModelResolver getResolver(
                List<RemoteRepository> repositories);

        @Override
        public ModelSource resolveModel(
                Session session,
                List<org.apache.maven.api.RemoteRepository> repositories,
                String groupId,
                String artifactId,
                String version,
                Consumer<String> resolved)
                throws ModelResolverException {
            try {
                InternalSession internalSession = InternalSession.from(session);
                org.apache.maven.model.resolution.ModelResolver resolver = getResolver(internalSession.toRepositories(
                        repositories != null ? repositories : internalSession.getRemoteRepositories()));
                org.apache.maven.model.Parent p = new org.apache.maven.model.Parent(Parent.newBuilder()
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(version)
                        .build());
                org.apache.maven.model.building.ModelSource modelSource = resolver.resolveModel(p);
                if (!p.getVersion().equals(version)) {
                    resolved.accept(p.getVersion());
                }
                return toSource(modelSource);
            } catch (UnresolvableModelException e) {
                throw new ModelResolverException(e.getMessage(), e.getGroupId(), e.getArtifactId(), e.getVersion(), e);
            }
        }

        @Override
        public ModelSource resolveModel(
                Session session,
                List<org.apache.maven.api.RemoteRepository> repositories,
                Parent parent,
                AtomicReference<Parent> modified)
                throws ModelResolverException {
            try {
                org.apache.maven.model.Parent p = new org.apache.maven.model.Parent(parent);
                InternalSession internalSession = InternalSession.from(session);
                org.apache.maven.model.resolution.ModelResolver resolver = getResolver(internalSession.toRepositories(
                        repositories != null ? repositories : internalSession.getRemoteRepositories()));
                ModelSource source = toSource(resolver.resolveModel(p));
                if (p.getDelegate() != parent) {
                    modified.set(p.getDelegate());
                }
                return source;
            } catch (UnresolvableModelException e) {
                throw new ModelResolverException(e.getMessage(), e.getGroupId(), e.getArtifactId(), e.getVersion(), e);
            }
        }

        @Override
        public ModelSource resolveModel(
                Session session,
                List<org.apache.maven.api.RemoteRepository> repositories,
                Dependency dependency,
                AtomicReference<Dependency> modified)
                throws ModelResolverException {
            try {
                org.apache.maven.model.Dependency d = new org.apache.maven.model.Dependency(dependency);
                InternalSession internalSession = InternalSession.from(session);
                org.apache.maven.model.resolution.ModelResolver resolver = getResolver(internalSession.toRepositories(
                        repositories != null ? repositories : internalSession.getRemoteRepositories()));
                ModelSource source = toSource(resolver.resolveModel(d));
                if (d.getDelegate() != dependency) {
                    modified.set(d.getDelegate());
                }
                return source;
            } catch (UnresolvableModelException e) {
                throw new ModelResolverException(e.getMessage(), e.getGroupId(), e.getArtifactId(), e.getVersion(), e);
            }
        }
    }

    /**
     * Processes events from the model builder while building the effective model for a {@link MavenProject} instance.
     *
     */
    public static class DefaultModelBuildingListener implements ModelBuildingListener {

        private final MavenProject project;

        private final ProjectBuildingHelper projectBuildingHelper;

        private final ProjectBuildingRequest projectBuildingRequest;

        private List<ArtifactRepository> remoteRepositories;

        private List<ArtifactRepository> pluginRepositories;

        public DefaultModelBuildingListener(
                MavenProject project,
                ProjectBuildingHelper projectBuildingHelper,
                ProjectBuildingRequest projectBuildingRequest) {
            this.project = Objects.requireNonNull(project, "project cannot be null");
            this.projectBuildingHelper =
                    Objects.requireNonNull(projectBuildingHelper, "projectBuildingHelper cannot be null");
            this.projectBuildingRequest =
                    Objects.requireNonNull(projectBuildingRequest, "projectBuildingRequest cannot be null");
            this.remoteRepositories = projectBuildingRequest.getRemoteRepositories();
            this.pluginRepositories = projectBuildingRequest.getPluginArtifactRepositories();
        }

        /**
         * Gets the project whose model is being built.
         *
         * @return The project, never {@code null}.
         */
        public MavenProject getProject() {
            return project;
        }

        @Override
        public void buildExtensionsAssembled(ModelBuildingEvent event) {
            org.apache.maven.model.Model model = new org.apache.maven.model.Model(event.model());

            try {
                pluginRepositories = projectBuildingHelper.createArtifactRepositories(
                        model.getPluginRepositories(), pluginRepositories, projectBuildingRequest);
            } catch (Exception e) {
                event.problems()
                        .add(
                                BuilderProblem.Severity.ERROR,
                                ModelProblem.Version.BASE,
                                "Invalid plugin repository: " + e.getMessage(),
                                e);
            }
            project.setPluginArtifactRepositories(pluginRepositories);

            if (event.request().getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
                try {
                    ProjectRealmCache.CacheRecord record =
                            projectBuildingHelper.createProjectRealm(project, model, projectBuildingRequest);

                    project.setClassRealm(record.getRealm());
                    project.setExtensionDependencyFilter(record.getExtensionArtifactFilter());
                } catch (PluginResolutionException | PluginManagerException | PluginVersionResolutionException e) {
                    event.problems()
                            .add(
                                    BuilderProblem.Severity.ERROR,
                                    ModelProblem.Version.BASE,
                                    "Unresolvable build extension: " + e.getMessage(),
                                    e);
                }

                projectBuildingHelper.selectProjectRealm(project);
            }

            // build the regular repos after extensions are loaded to allow for custom layouts
            try {
                remoteRepositories = projectBuildingHelper.createArtifactRepositories(
                        model.getRepositories(), remoteRepositories, projectBuildingRequest);
            } catch (Exception e) {
                event.problems()
                        .add(
                                BuilderProblem.Severity.ERROR,
                                ModelProblem.Version.BASE,
                                "Invalid artifact repository: " + e.getMessage(),
                                e);
            }
            project.setRemoteArtifactRepositories(remoteRepositories);

            if (model.getDelegate() != event.model()) {
                event.update().accept(model.getDelegate());
            }
        }
    }
}
