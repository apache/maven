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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorReader.class);

    private final MavenSession session;
    private final WorkspaceRepository repository;

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
        List<MavenProject> projects = session.getAllProjects();
        if (projects != null) {
            for (MavenProject p : projects) {
                if (Objects.equals(artifact.getGroupId(), p.getGroupId())
                        && Objects.equals(artifact.getArtifactId(), p.getArtifactId())
                        && Objects.equals(artifact.getBaseVersion(), p.getVersion())
                        && Objects.equals(artifact.getExtension(), "pom")
                        && p.getFile() != null) {
                    return p.getFile();
                }
            }
        }
        File file = find(artifact);
        return file;
    }

    public List<String> findVersions(Artifact artifact) {
        List<String> versions = new ArrayList<>();
        String artifactId = artifact.getArtifactId();
        String groupId = artifact.getGroupId();
        Path repo = getProjectLocalRepo().resolve(groupId).resolve(artifactId);
        String classifier = artifact.getClassifier();
        String extension = artifact.getExtension();
        Pattern pattern = Pattern.compile("\\Q" + artifactId + "\\E-(.*)"
                + (classifier != null ? "-\\Q" + classifier + "\\E" : "")
                + (extension != null ? "." + extension : ""));
        try (Stream<Path> paths = Files.list(repo)) {
            paths.forEach(p -> {
                String filename = p.getFileName().toString();
                Matcher matcher = pattern.matcher(filename);
                if (matcher.matches()) {
                    versions.add(matcher.group(1));
                }
            });
        } catch (IOException e) {
            // ignore
        }
        return Collections.unmodifiableList(versions);
    }

    @Override
    public Model findModel(Artifact artifact) {
        List<MavenProject> projects = session.getAllProjects();
        if (projects != null) {
            for (MavenProject p : projects) {
                if (Objects.equals(artifact.getGroupId(), p.getGroupId())
                        && Objects.equals(artifact.getArtifactId(), p.getArtifactId())
                        && Objects.equals(artifact.getBaseVersion(), p.getVersion())
                        && Objects.equals(artifact.getExtension(), "pom")
                        && p.getFile() != null) {
                    return p.getModel();
                }
            }
        }
        return null;
    }

    //
    // Implementation
    //

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
                    Files.copy(artifact.getFile().toPath(), target, StandardCopyOption.REPLACE_EXISTING);
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
        Path repo = root.resolve("target").resolve("project-local-repo");
        return repo;
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
