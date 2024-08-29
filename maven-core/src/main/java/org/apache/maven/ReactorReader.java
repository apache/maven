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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.resolver.MavenWorkspaceReader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a workspace reader that knows how to search the Maven reactor for artifacts, either as packaged
 * jar if it has been built, or only compile output directory if packaging hasn't happened yet.
 *
 */
@Named(ReactorReader.HINT)
@SessionScoped
class ReactorReader implements MavenWorkspaceReader {
    public static final String HINT = "reactor";

    public static final String PROJECT_LOCAL_REPO = "project-local-repo";

    private static final Collection<String> COMPILE_PHASE_TYPES = new HashSet<>(
            Arrays.asList("jar", "ejb-client", "war", "rar", "ejb3", "par", "sar", "wsr", "har", "app-client"));

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorReader.class);

    private final MavenSession session;
    private final WorkspaceRepository repository;
    // groupId -> (artifactId -> (version -> project)))
    private Map<String, Map<String, Map<String, MavenProject>>> projects;
    private Map<String, Map<String, Map<String, MavenProject>>> allProjects;
    private Path projectLocalRepository;
    // projectId -> Deque<lifecycle>
    private final Map<String, Deque<String>> lifecycles = new ConcurrentHashMap<>();

    @Inject
    ReactorReader(MavenSession session) {
        this.session = session;
        this.repository = new WorkspaceRepository("reactor", null);
    }

    //
    // Public API
    //

    public WorkspaceRepository getRepository() {
        return repository;
    }

    public File findArtifact(Artifact artifact) {
        MavenProject project = getProject(artifact);

        if (project != null) {
            File file = findArtifact(project, artifact, true);
            if (file == null && project != project.getExecutionProject()) {
                file = findArtifact(project.getExecutionProject(), artifact, true);
            }
            return file;
        }

        // No project, but most certainly a dependency which has been built previously
        File packagedArtifactFile = findInProjectLocalRepository(artifact);
        if (packagedArtifactFile != null && packagedArtifactFile.exists()) {
            return packagedArtifactFile;
        }

        return null;
    }

    public List<String> findVersions(Artifact artifact) {
        List<String> versions = getProjects()
                .getOrDefault(artifact.getGroupId(), Collections.emptyMap())
                .getOrDefault(artifact.getArtifactId(), Collections.emptyMap())
                .values()
                .stream()
                .map(MavenProject::getVersion)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        if (!versions.isEmpty()) {
            return versions;
        }
        return getAllProjects()
                .getOrDefault(artifact.getGroupId(), Collections.emptyMap())
                .getOrDefault(artifact.getArtifactId(), Collections.emptyMap())
                .values()
                .stream()
                .filter(p -> Objects.nonNull(findArtifact(p, artifact, false)))
                .map(MavenProject::getVersion)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public Model findModel(Artifact artifact) {
        MavenProject project = getProject(artifact);
        return project == null ? null : project.getModel().getDelegate();
    }

    //
    // Implementation
    //

    private File findArtifact(MavenProject project, Artifact artifact, boolean checkUptodate) {
        // POMs are always returned from the file system
        if ("pom".equals(artifact.getExtension())) {
            return project.getFile();
        }

        // Get the matching artifact from the project
        Artifact projectArtifact = findMatchingArtifact(project, artifact);
        if (projectArtifact != null) {
            // If the artifact has been associated to a file, use it
            File packagedArtifactFile = projectArtifact.getFile();
            if (packagedArtifactFile != null && packagedArtifactFile.exists()) {
                return packagedArtifactFile;
            }
        }

        // Check in the project local repository
        File packagedArtifactFile = findInProjectLocalRepository(artifact);
        if (packagedArtifactFile != null
                && packagedArtifactFile.exists()
                && (!checkUptodate || isPackagedArtifactUpToDate(project, packagedArtifactFile))) {
            return packagedArtifactFile;
        }

        if (!hasBeenPackagedDuringThisSession(project)) {
            // fallback to loose class files only if artifacts haven't been packaged yet
            // and only for plain old jars. Not war files, not ear files, not anything else.
            return determineBuildOutputDirectoryForArtifact(project, artifact);
        }

        // The fall-through indicates that the artifact cannot be found;
        // for instance if package produced nothing or classifier problems.
        return null;
    }

    private File determineBuildOutputDirectoryForArtifact(final MavenProject project, final Artifact artifact) {
        if (isTestArtifact(artifact)) {
            if (project.hasLifecyclePhase("test-compile")) {
                return new File(project.getBuild().getTestOutputDirectory());
            }
        } else {
            String type = artifact.getProperty("type", "");
            File outputDirectory = new File(project.getBuild().getOutputDirectory());

            // Check if the project is being built during this session, and if we can expect any output.
            // There is no need to check if the build has created any outputs, see MNG-2222.
            boolean projectCompiledDuringThisSession =
                    project.hasLifecyclePhase("compile") && COMPILE_PHASE_TYPES.contains(type);

            // Check if the project is part of the session (not filtered by -pl, -rf, etc). If so, we check
            // if a possible earlier Maven invocation produced some output for that project which we can use.
            boolean projectHasOutputFromPreviousSession =
                    !session.getProjects().contains(project) && outputDirectory.exists();

            if (projectHasOutputFromPreviousSession || projectCompiledDuringThisSession) {
                return outputDirectory;
            }
        }

        // The fall-through indicates that the artifact cannot be found;
        // for instance if package produced nothing or classifier problems.
        return null;
    }

    private boolean isPackagedArtifactUpToDate(MavenProject project, File packagedArtifactFile) {
        Path outputDirectory = Paths.get(project.getBuild().getOutputDirectory());
        if (!outputDirectory.toFile().exists()) {
            return true;
        }

        try (Stream<Path> outputFiles = Files.walk(outputDirectory)) {
            // Not using File#lastModified() to avoid a Linux JDK8 milliseconds precision bug: JDK-8177809.
            long artifactLastModified =
                    Files.getLastModifiedTime(packagedArtifactFile.toPath()).toMillis();

            if (session.getProjectBuildingRequest().getBuildStartTime() != null) {
                long buildStartTime =
                        session.getProjectBuildingRequest().getBuildStartTime().getTime();
                if (artifactLastModified > buildStartTime) {
                    return true;
                }
            }

            for (Path outputFile : (Iterable<Path>) outputFiles::iterator) {
                if (Files.isDirectory(outputFile)) {
                    continue;
                }

                long outputFileLastModified =
                        Files.getLastModifiedTime(outputFile).toMillis();
                if (outputFileLastModified > artifactLastModified) {
                    LOGGER.warn(
                            "File '{}' is more recent than the packaged artifact for '{}', "
                                    + "please run a full `mvn package` build",
                            relativizeOutputFile(outputFile),
                            project.getArtifactId());
                    return true;
                }
            }

            return true;
        } catch (IOException e) {
            LOGGER.warn(
                    "An I/O error occurred while checking if the packaged artifact is up-to-date "
                            + "against the build output directory. "
                            + "Continuing with the assumption that it is up-to-date.",
                    e);
            return true;
        }
    }

    private boolean hasBeenPackagedDuringThisSession(MavenProject project) {
        boolean packaged = false;
        for (String phase : getLifecycles(project)) {
            switch (phase) {
                case "clean":
                    packaged = false;
                    break;
                case "package":
                case "install":
                case "deploy":
                    packaged = true;
                    break;
                default:
                    break;
            }
        }
        return packaged;
    }

    private Path relativizeOutputFile(final Path outputFile) {
        Path projectBaseDirectory =
                Paths.get(session.getRequest().getMultiModuleProjectDirectory().toURI());
        return projectBaseDirectory.relativize(outputFile);
    }

    /**
     * Tries to resolve the specified artifact from the artifacts of the given project.
     *
     * @param project The project to try to resolve the artifact from, must not be <code>null</code>.
     * @param requestedArtifact The artifact to resolve, must not be <code>null</code>.
     * @return The matching artifact from the project or <code>null</code> if not found. Note that this
     */
    private Artifact findMatchingArtifact(MavenProject project, Artifact requestedArtifact) {
        String requestedRepositoryConflictId = ArtifactIdUtils.toVersionlessId(requestedArtifact);
        return getProjectArtifacts(project)
                .filter(artifact ->
                        Objects.equals(requestedRepositoryConflictId, ArtifactIdUtils.toVersionlessId(artifact)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines whether the specified artifact refers to test classes.
     *
     * @param artifact The artifact to check, must not be {@code null}.
     * @return {@code true} if the artifact refers to test classes, {@code false} otherwise.
     */
    private static boolean isTestArtifact(Artifact artifact) {
        return ("test-jar".equals(artifact.getProperty("type", "")))
                || ("jar".equals(artifact.getExtension()) && "tests".equals(artifact.getClassifier()));
    }

    private File findInProjectLocalRepository(Artifact artifact) {
        Path target = getArtifactPath(artifact);
        return Files.isRegularFile(target) ? target.toFile() : null;
    }

    /**
     * We are interested in project success events, in which case we call
     * the {@link #installIntoProjectLocalRepository(MavenProject)} method.
     * The mojo started event is also captured to determine the lifecycle
     * phases the project has been through.
     *
     * @param event the execution event
     */
    private void processEvent(ExecutionEvent event) {
        MavenProject project = event.getProject();
        switch (event.getType()) {
            case MojoStarted:
                String phase = event.getMojoExecution().getLifecyclePhase();
                if (phase != null) {
                    Deque<String> phases = getLifecycles(project);
                    if (!Objects.equals(phase, phases.peekLast())) {
                        phases.addLast(phase);
                        if ("clean".equals(phase)) {
                            cleanProjectLocalRepository(project);
                        }
                    }
                }
                break;
            case ProjectSucceeded:
            case ForkedProjectSucceeded:
                installIntoProjectLocalRepository(project);
                break;
            default:
                break;
        }
    }

    private Deque<String> getLifecycles(MavenProject project) {
        return lifecycles.computeIfAbsent(project.getId(), k -> new ArrayDeque<>());
    }

    /**
     * Copy packaged and attached artifacts from this project to the
     * project local repository.
     * This allows a subsequent build to resume while still being able
     * to locate attached artifacts.
     *
     * @param project the project to copy artifacts from
     */
    private void installIntoProjectLocalRepository(MavenProject project) {
        if ("pom".equals(project.getPackaging())
                        && !"clean".equals(getLifecycles(project).peekLast())
                || hasBeenPackagedDuringThisSession(project)) {
            getProjectArtifacts(project).filter(this::isRegularFile).forEach(this::installIntoProjectLocalRepository);
        }
    }

    private void cleanProjectLocalRepository(MavenProject project) {
        try {
            Path artifactPath = getProjectLocalRepo()
                    .resolve(project.getGroupId())
                    .resolve(project.getArtifactId())
                    .resolve(project.getVersion());
            if (Files.isDirectory(artifactPath)) {
                try (Stream<Path> paths = Files.list(artifactPath)) {
                    for (Path path : (Iterable<Path>) paths::iterator) {
                        Files.delete(path);
                    }
                }
                try {
                    Files.delete(artifactPath);
                    Files.delete(artifactPath.getParent());
                    Files.delete(artifactPath.getParent().getParent());
                } catch (DirectoryNotEmptyException e) {
                    // ignore
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while cleaning project local repository", e);
        }
    }

    /**
     * Retrieve a stream of the project's artifacts
     */
    private Stream<Artifact> getProjectArtifacts(MavenProject project) {
        Stream<org.apache.maven.artifact.Artifact> artifacts = Stream.concat(
                Stream.concat(
                        // pom artifact
                        Stream.of(new ProjectArtifact(project)),
                        // main project artifact if not a pom
                        "pom".equals(project.getPackaging()) ? Stream.empty() : Stream.of(project.getArtifact())),
                // attached artifacts
                project.getAttachedArtifacts().stream());
        return artifacts.map(RepositoryUtils::toArtifact);
    }

    private boolean isRegularFile(Artifact artifact) {
        return artifact.getFile() != null && artifact.getFile().isFile();
    }

    private void installIntoProjectLocalRepository(Artifact artifact) {
        Path target = getArtifactPath(artifact);
        try {
            LOGGER.info("Copying {} to project local repository", artifact);
            Files.createDirectories(target.getParent());
            Files.copy(
                    artifact.getPath(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            LOGGER.error("Error while copying artifact to project local repository", e);
        }
    }

    private Path getArtifactPath(Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getBaseVersion();
        String classifier = artifact.getClassifier();
        String extension = artifact.getExtension();
        Path repo = getProjectLocalRepo();
        return repo.resolve(groupId)
                .resolve(artifactId)
                .resolve(version)
                .resolve(artifactId
                        + "-" + version
                        + (classifier != null && !classifier.isEmpty() ? "-" + classifier : "")
                        + "." + extension);
    }

    private Path getProjectLocalRepo() {
        if (projectLocalRepository == null) {
            Path root = session.getRequest().getMultiModuleProjectDirectory().toPath();
            List<MavenProject> projects = session.getProjects();
            if (projects != null) {
                projectLocalRepository = projects.stream()
                        .filter(project -> Objects.equals(root.toFile(), project.getBasedir()))
                        .findFirst()
                        .map(project -> project.getBuild().getDirectory())
                        .map(Paths::get)
                        .orElseGet(() -> root.resolve("target"))
                        .resolve(PROJECT_LOCAL_REPO);
            } else {
                return root.resolve("target").resolve(PROJECT_LOCAL_REPO);
            }
        }
        return projectLocalRepository;
    }

    private MavenProject getProject(Artifact artifact) {
        return getAllProjects()
                .getOrDefault(artifact.getGroupId(), Collections.emptyMap())
                .getOrDefault(artifact.getArtifactId(), Collections.emptyMap())
                .getOrDefault(artifact.getBaseVersion(), null);
    }

    // groupId -> (artifactId -> (version -> project)))
    private Map<String, Map<String, Map<String, MavenProject>>> getAllProjects() {
        // compute the projects mapping
        if (allProjects == null) {
            List<MavenProject> allProjects = session.getAllProjects();
            if (allProjects != null) {
                Map<String, Map<String, Map<String, MavenProject>>> map = new HashMap<>();
                allProjects.forEach(project -> map.computeIfAbsent(project.getGroupId(), k -> new HashMap<>())
                        .computeIfAbsent(project.getArtifactId(), k -> new HashMap<>())
                        .put(project.getVersion(), project));
                this.allProjects = map;
            } else {
                return Collections.emptyMap();
            }
        }
        return allProjects;
    }

    private Map<String, Map<String, Map<String, MavenProject>>> getProjects() {
        // compute the projects mapping
        if (projects == null) {
            List<MavenProject> projects = session.getProjects();
            if (projects != null) {
                Map<String, Map<String, Map<String, MavenProject>>> map = new HashMap<>();
                projects.forEach(project -> map.computeIfAbsent(project.getGroupId(), k -> new HashMap<>())
                        .computeIfAbsent(project.getArtifactId(), k -> new HashMap<>())
                        .put(project.getVersion(), project));
                this.projects = map;
            } else {
                return Collections.emptyMap();
            }
        }
        return projects;
    }

    /**
     * Singleton class used to receive events by implementing the EventSpy.
     * It simply forwards all {@code ExecutionEvent}s to the {@code ReactorReader}.
     */
    @Named
    @Singleton
    @SuppressWarnings("unused")
    static class ReactorReaderSpy implements EventSpy {

        private final Lookup lookup;

        @Inject
        ReactorReaderSpy(Lookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public void init(Context context) throws Exception {}

        @Override
        public void onEvent(Object event) throws Exception {
            if (event instanceof ExecutionEvent) {
                ReactorReader reactorReader = lookup.lookup(ReactorReader.class);
                reactorReader.processEvent((ExecutionEvent) event);
            }
        }

        @Override
        public void close() throws Exception {}
    }
}
