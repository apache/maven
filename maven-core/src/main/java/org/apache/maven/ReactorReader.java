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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

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

    public static final String PROJECT_LOCAL_REPO = "project-local-repo";

    private static final Collection<String> COMPILE_PHASE_TYPES = new HashSet<>(
            Arrays.asList("jar", "ejb-client", "war", "rar", "ejb3", "par", "sar", "wsr", "har", "app-client"));

    private final MavenSession session;
    private final Map<String, MavenProject> projectsByGAV;
    private final Map<String, List<MavenProject>> projectsByGA;
    private final WorkspaceRepository repository;

    private Function<MavenProject, String> projectIntoKey =
            s -> ArtifactUtils.key(s.getGroupId(), s.getArtifactId(), s.getVersion());

    private Function<MavenProject, String> projectIntoVersionlessKey =
            s -> ArtifactUtils.versionlessKey(s.getGroupId(), s.getArtifactId());

    @Inject
    ReactorReader(MavenSession session) {
        this.session = session;
        this.projectsByGAV = session.getProjects().stream().collect(toMap(projectIntoKey, identity()));

        this.projectsByGA = projectsByGAV.values().stream().collect(groupingBy(projectIntoVersionlessKey));

        repository = new WorkspaceRepository("reactor", new HashSet<>(projectsByGAV.keySet()));
    }

    //
    // Public API
    //

    public WorkspaceRepository getRepository() {
        return repository;
    }

    public File findArtifact(Artifact artifact) {
        String projectKey = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

        MavenProject project = projectsByGAV.get(projectKey);

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
        String key = ArtifactUtils.versionlessKey(artifact.getGroupId(), artifact.getArtifactId());

        return Optional.ofNullable(projectsByGA.get(key)).orElse(Collections.emptyList()).stream()
                .filter(s -> Objects.nonNull(find(s, artifact)))
                .map(MavenProject::getVersion)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public Model findModel(Artifact artifact) {
        String projectKey = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        MavenProject project = projectsByGAV.get(projectKey);
        return project == null ? null : project.getModel();
    }

    //
    // Implementation
    //

    private File find(MavenProject project, Artifact artifact) {
        if ("pom".equals(artifact.getExtension())) {
            return project.getFile();
        }

        Artifact projectArtifact = findMatchingArtifact(project, artifact);

        if (hasArtifactFileFromPackagePhase(projectArtifact)) {
            return projectArtifact.getFile();
        } else if (!hasBeenPackaged(project)) {
            // fallback to loose class files only if artifacts haven't been packaged yet
            // and only for plain old jars. Not war files, not ear files, not anything else.

            if (isTestArtifact(artifact)) {
                if (project.hasLifecyclePhase("test-compile")) {
                    return new File(project.getBuild().getTestOutputDirectory());
                }
            } else {
                String type = artifact.getProperty("type", "");
                if (project.hasLifecyclePhase("compile") && COMPILE_PHASE_TYPES.contains(type)) {
                    return new File(project.getBuild().getOutputDirectory());
                }
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

    private boolean hasBeenPackaged(MavenProject project) {
        return project.hasLifecyclePhase("package")
                || project.hasLifecyclePhase("install")
                || project.hasLifecyclePhase("deploy");
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
}
