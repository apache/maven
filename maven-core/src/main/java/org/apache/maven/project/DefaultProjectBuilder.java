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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.ProjectCycleException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DeploymentRepository;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.Model;
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
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepositoryManager;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ModelBuilder modelBuilder;
    private final ProjectBuildingHelper projectBuildingHelper;
    private final MavenRepositorySystem repositorySystem;
    private final org.eclipse.aether.RepositorySystem repoSystem;
    private final ProjectDependenciesResolver dependencyResolver;
    private final RootLocator rootLocator;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    public DefaultProjectBuilder(
            ModelBuilder modelBuilder,
            ProjectBuildingHelper projectBuildingHelper,
            MavenRepositorySystem repositorySystem,
            RepositorySystem repoSystem,
            ProjectDependenciesResolver dependencyResolver,
            RootLocator rootLocator) {
        this.modelBuilder = modelBuilder;
        this.projectBuildingHelper = projectBuildingHelper;
        this.repositorySystem = repositorySystem;
        this.repoSystem = repoSystem;
        this.dependencyResolver = dependencyResolver;
        this.rootLocator = rootLocator;
    }
    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    @Override
    public ProjectBuildingResult build(File pomFile, ProjectBuildingRequest request) throws ProjectBuildingException {
        try (BuildSession bs = new BuildSession(request)) {
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
        try (BuildSession bs = new BuildSession(request)) {
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
        try (BuildSession bs = new BuildSession(request)) {
            return bs.build(artifact, allowStubModel);
        }
    }

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        try (BuildSession bs = new BuildSession(request)) {
            return bs.build(pomFiles, recursive);
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
        private final ModelBuilder.ModelBuilderSession modelBuilderSession;
        private final Map<File, MavenProject> projectIndex = new ConcurrentHashMap<>(256);

        BuildSession(ProjectBuildingRequest request) {
            this.request = request;
            this.session =
                    RepositoryUtils.overlay(request.getLocalRepository(), request.getRepositorySession(), repoSystem);
            InternalSession.from(session);
            this.modelBuilderSession = modelBuilder.newSession();
        }

        @Override
        public void close() {}

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

                    initProject(project, result);
                }

                DependencyResolutionResult resolutionResult = null;

                if (request.isResolveDependencies()) {
                    projectBuildingHelper.selectProjectRealm(project);
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
            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                return pomFiles.stream()
                        .map(pomFile -> build(pomFile, true, recursive))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            } finally {
                Thread.currentThread().setContextClassLoader(oldContextClassLoader);
            }
        }

        @SuppressWarnings("checkstyle:parameternumber")
        private List<ProjectBuildingResult> build(File pomFile, boolean topLevel, boolean recursive) {
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
                    return List.of(new DefaultProjectBuildingResult(e.getModelId(), pomFile, convert(e.getProblems())));
                }
                // validation error, continue project building and delay failing to help IDEs
                // result.getProblems().addAll(e.getProblems()) ?
            }

            projectIndex.put(pomFile, project);

            try {
                // 2nd pass of initialization: resolve and build parent if necessary
                List<org.apache.maven.model.building.ModelProblem> problems = convert(result.getProblems());
                try {
                    initProject(project, result);
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

                List<ProjectBuildingResult> results = result.getChildren().stream()
                        .map(r -> build(r.getFileModel().getPomFile().toFile(), false, true))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());

                project.setExecutionRoot(topLevel);
                project.setCollectedProjects(
                        results.stream().map(ProjectBuildingResult::getProject).collect(Collectors.toList()));
                DependencyResolutionResult resolutionResult = null;
                if (request.isResolveDependencies()) {
                    resolutionResult = resolveDependencies(project);
                }

                results.add(new DefaultProjectBuildingResult(project, problems, resolutionResult));

                return results;
            } catch (ModelBuilderException e) {
                DefaultProjectBuildingResult eresult;
                if (result.getEffectiveModel() == null) {
                    eresult = new DefaultProjectBuildingResult(e.getModelId(), pomFile, convert(e.getProblems()));
                } else {
                    project.setModel(new org.apache.maven.model.Model(result.getEffectiveModel()));
                    eresult = new DefaultProjectBuildingResult(project, convert(e.getProblems()), null);
                }
                return Collections.singletonList(eresult);
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
        private void initProject(MavenProject project, ModelBuilderResult result) {
            project.setModel(new org.apache.maven.model.Model(result.getEffectiveModel()));
            project.setOriginalModel(new org.apache.maven.model.Model(result.getFileModel()));

            initParent(project, result);

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

        private void initParent(MavenProject project, ModelBuilderResult result) {
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
                MavenProject parent = parentPomFile != null ? projectIndex.get(parentPomFile.toFile()) : null;
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
            modelBuildingRequest.repositoryMerging(ModelBuilderRequest.RepositoryMerging.valueOf(
                    request.getRepositoryMerging().name()));
            modelBuildingRequest.repositories(request.getRemoteRepositories().stream()
                    .map(r -> internalSession.getRemoteRepository(RepositoryUtils.toRepo(r)))
                    .toList());
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
        String xml = "<?xml version='1.0'?>" + "<project>"
                + "<modelVersion>4.0.0</modelVersion>"
                + "<groupId>"
                + artifact.getGroupId() + "</groupId>" + "<artifactId>"
                + artifact.getArtifactId() + "</artifactId>" + "<version>"
                + artifact.getBaseVersion() + "</version>" + "<packaging>"
                + artifact.getType() + "</packaging>" + "</project>";
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
