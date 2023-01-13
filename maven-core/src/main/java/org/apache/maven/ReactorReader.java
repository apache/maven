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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a workspace reader that knows how to search the Maven reactor for artifacts, either as packaged
 * jar if it has been built, or only compile output directory if packaging hasn't happened yet.
 *
 * @author Jason van Zyl
 */
@Named(ReactorReader.HINT)
@SessionScoped
class ReactorReader implements MavenWorkspaceReader {
    public static final String HINT = "reactor";

    private static final Collection<String> COMPILE_PHASE_TYPES =
            Arrays.asList("jar", "ejb-client", "war", "rar", "ejb3", "par", "sar", "wsr", "har", "app-client");

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorReader.class);

    private final MavenSession session;
    private final WorkspaceRepository repository;
    private Map<String, Map<String, Map<String, MavenProject>>> projects;

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
            File file = find(project, artifact);
            if (file == null && project != project.getExecutionProject()) {
                file = find(project.getExecutionProject(), artifact);
            }
            return file;
        }

        return null;
    }

    public List<String> findVersions(Artifact artifact) {
        return getProjects()
                .getOrDefault(artifact.getGroupId(), Collections.emptyMap())
                .getOrDefault(artifact.getArtifactId(), Collections.emptyMap())
                .values()
                .stream()
                .filter(p -> Objects.nonNull(find(p, artifact)))
                .map(MavenProject::getVersion)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public Model findModel(Artifact artifact) {
        MavenProject project = getProject(artifact);
        return project == null ? null : project.getModel();
    }

    //
    // Implementation
    //

    private File find(MavenProject project, Artifact artifact) {
        if ("pom".equals(artifact.getExtension())) {
            return project.getFile();
        }

        File packagedArtifactFile = find(artifact);
        if (packagedArtifactFile != null
                && packagedArtifactFile.exists()
                && isPackagedArtifactUpToDate(project, packagedArtifactFile, artifact)) {
            return packagedArtifactFile;
        }

        Artifact projectArtifact = findMatchingArtifact(project, artifact);
        packagedArtifactFile = determinePreviouslyPackagedArtifactFile(project, projectArtifact);

        if (hasArtifactFileFromPackagePhase(projectArtifact)) {
            return projectArtifact.getFile();
        }
        // Check whether an earlier Maven run might have produced an artifact that is still on disk.
        else if (packagedArtifactFile != null
                && packagedArtifactFile.exists()
                && isPackagedArtifactUpToDate(project, packagedArtifactFile, artifact)) {
            return packagedArtifactFile;
        } else if (!hasBeenPackagedDuringThisSession(project)) {
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

    private File determinePreviouslyPackagedArtifactFile(MavenProject project, Artifact artifact) {
        if (artifact == null) {
            return null;
        }
        String fileName = String.format("%s.%s", project.getBuild().getFinalName(), artifact.getExtension());
        return new File(project.getBuild().getDirectory(), fileName);
    }

    private boolean hasArtifactFileFromPackagePhase(Artifact projectArtifact) {
        return projectArtifact != null
                && projectArtifact.getFile() != null
                && projectArtifact.getFile().exists();
    }

    private boolean isPackagedArtifactUpToDate(MavenProject project, File packagedArtifactFile, Artifact artifact) {
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

            Iterator<Path> iterator = outputFiles.iterator();
            while (iterator.hasNext()) {
                Path outputFile = iterator.next();

                if (Files.isDirectory(outputFile)) {
                    continue;
                }

                long outputFileLastModified =
                        Files.getLastModifiedTime(outputFile).toMillis();
                if (outputFileLastModified > artifactLastModified) {
                    File alternative = determineBuildOutputDirectoryForArtifact(project, artifact);
                    if (alternative != null) {
                        LOGGER.warn(
                                "File '{}' is more recent than the packaged artifact for '{}'; using '{}' instead",
                                relativizeOutputFile(outputFile),
                                project.getArtifactId(),
                                relativizeOutputFile(alternative.toPath()));
                    } else {
                        LOGGER.warn(
                                "File '{}' is more recent than the packaged artifact for '{}'; "
                                        + "cannot use the build output directory for this type of artifact",
                                relativizeOutputFile(outputFile),
                                project.getArtifactId());
                    }
                    // TODO: remove the following log
                    LOGGER.warn(
                            "{} > {}",
                            Files.getLastModifiedTime(outputFile),
                            Files.getLastModifiedTime(packagedArtifactFile.toPath()));
                    return false;
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
        return project.hasLifecyclePhase("package")
                || project.hasLifecyclePhase("install")
                || project.hasLifecyclePhase("deploy");
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

        Artifact mainArtifact = RepositoryUtils.toArtifact(project.getArtifact());
        if (requestedRepositoryConflictId.equals(ArtifactIdUtils.toVersionlessId(mainArtifact))) {
            return mainArtifact;
        }

        return RepositoryUtils.toArtifacts(project.getAttachedArtifacts()).stream()
                .filter(isRequestedArtifact(requestedArtifact))
                .findFirst()
                .orElse(null);
    }

    /**
     * We are taking as much as we can from the DefaultArtifact.equals(). The requested artifact has no file, so we want
     * to remove that from the comparison.
     *
     * @param requestArtifact checked against the given artifact.
     * @return true if equals, false otherwise.
     */
    private Predicate<Artifact> isRequestedArtifact(Artifact requestArtifact) {
        return s -> s.getArtifactId().equals(requestArtifact.getArtifactId())
                && s.getGroupId().equals(requestArtifact.getGroupId())
                && s.getVersion().equals(requestArtifact.getVersion())
                && s.getExtension().equals(requestArtifact.getExtension())
                && s.getClassifier().equals(requestArtifact.getClassifier());
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

    private File find(Artifact artifact) {
        Path target = getArtifactPath(artifact);
        return Files.isRegularFile(target) ? target.toFile() : null;
    }

    public void processProject(MavenProject project) {
        List<Artifact> artifacts = new ArrayList<>();

        artifacts.add(RepositoryUtils.toArtifact(new ProjectArtifact(project)));
        if (!"pom".equals(project.getPackaging())) {
            org.apache.maven.artifact.Artifact mavenMainArtifact = project.getArtifact();
            artifacts.add(RepositoryUtils.toArtifact(mavenMainArtifact));
        }
        for (org.apache.maven.artifact.Artifact attached : project.getAttachedArtifacts()) {
            artifacts.add(RepositoryUtils.toArtifact(attached));
        }

        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null && artifact.getFile().isFile()) {
                Path target = getArtifactPath(artifact);
                try {
                    LOGGER.debug("Copying {} to project local repository", artifact);
                    Files.createDirectories(target.getParent());
                    Files.copy(
                            artifact.getFile().toPath(),
                            target,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException e) {
                    LOGGER.warn("Error while copying artifact to project local repository", e);
                }
            }
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
                .resolve(artifactId
                        + "-" + version
                        + (classifier != null && !classifier.isEmpty() ? "-" + classifier : "")
                        + (extension != null && !extension.isEmpty() ? "." + extension : ""));
    }

    private Path getProjectLocalRepo() {
        Path root = session.getRequest().getMultiModuleProjectDirectory().toPath();
        return root.resolve("target").resolve("project-local-repo");
    }

    private MavenProject getProject(Artifact artifact) {
        return getProjects()
                .getOrDefault(artifact.getGroupId(), Collections.emptyMap())
                .getOrDefault(artifact.getArtifactId(), Collections.emptyMap())
                .getOrDefault(artifact.getBaseVersion(), null);
    }

    private Map<String, Map<String, Map<String, MavenProject>>> getProjects() {
        // compute the projects mapping
        if (projects == null) {
            List<MavenProject> allProjects = session.getAllProjects();
            if (allProjects != null) {
                Map<String, Map<String, Map<String, MavenProject>>> map = new HashMap<>();
                allProjects.forEach(project -> map.computeIfAbsent(project.getGroupId(), k -> new HashMap<>())
                        .computeIfAbsent(project.getArtifactId(), k -> new HashMap<>())
                        .put(project.getVersion(), project));
                this.projects = map;
            } else {
                return Collections.emptyMap();
            }
        }
        return projects;
    }

    @Named
    @Singleton
    @SuppressWarnings("unused")
    static class ReactorReaderSpy implements EventSpy {

        final PlexusContainer container;

        @Inject
        ReactorReaderSpy(PlexusContainer container) {
            this.container = container;
        }

        @Override
        public void init(Context context) throws Exception {}

        @Override
        public void onEvent(Object event) throws Exception {
            if (event instanceof ExecutionEvent) {
                ExecutionEvent ee = (ExecutionEvent) event;
                if (ee.getType() == ExecutionEvent.Type.ForkedProjectSucceeded
                        || ee.getType() == ExecutionEvent.Type.ProjectSucceeded) {
                    container.lookup(ReactorReader.class).processProject(ee.getProject());
                }
            }
        }

        @Override
        public void close() throws Exception {}
    }
}
