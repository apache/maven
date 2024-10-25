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
package org.apache.maven.internal.aether;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectStepData;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import static java.util.Objects.requireNonNull;

/**
 * A class building reverse tree using {@link CollectStepData} trace data provided in {@link RepositoryEvent}
 * events fired during collection.
 *
 * @since 3.9.0
 */
class ReverseTreeRepositoryListener extends AbstractRepositoryListener {
    @Override
    public void artifactResolved(RepositoryEvent event) {
        requireNonNull(event, "event cannot be null");

        if (!isLocalRepositoryArtifactOrMissing(event.getSession(), event.getArtifact())) {
            return;
        }

        RequestTrace trace = event.getTrace();

        CollectStepData collectStepTrace = null;
        ArtifactRequest artifactRequest = null;
        ArtifactDescriptorRequest artifactDescriptorRequest = null;
        Plugin plugin = null;

        while (trace != null) {
            Object data = trace.getData();
            if (data instanceof CollectStepData) {
                collectStepTrace = (CollectStepData) data;
            } else if (data instanceof ArtifactDescriptorRequest) {
                artifactDescriptorRequest = (ArtifactDescriptorRequest) data;
            } else if (data instanceof ArtifactRequest) {
                artifactRequest = (ArtifactRequest) data;
            } else if (data instanceof Plugin) {
                plugin = (Plugin) data;
            }
            trace = trace.getParent();
        }

        Path trackingDir;
        boolean missing = event.getFile() == null;
        if (missing) {
            // missing artifact - let's track the path anyway
            File dir = event.getSession().getLocalRepository().getBasedir();
            dir = new File(
                    dir, event.getSession().getLocalRepositoryManager().getPathForLocalArtifact(event.getArtifact()));
            trackingDir = dir.getParentFile().toPath().resolve(".tracking");
        } else {
            trackingDir = event.getFile().getParentFile().toPath().resolve(".tracking");
        }

        String baseName;
        String ext = missing ? ".miss" : ".dep";
        Path trackingFile = null;

        String indent = "";
        ArrayList<String> trackingData = new ArrayList<>();

        if (collectStepTrace == null && plugin != null) {
            ext = ".plugin";
            baseName = plugin.getGroupId() + "_" + plugin.getArtifactId() + "_" + plugin.getVersion();
            trackingFile = trackingDir.resolve(baseName + ext);
            if (Files.exists(trackingFile)) {
                return;
            }

            if (event.getArtifact() != null) {
                trackingData.add(indent + event.getArtifact());
                indent += "  ";
            }
            trackingData.add(indent + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
            indent += "  ";

            InputLocation location = plugin.getLocation("");
            if (location != null && location.getSource() != null) {
                trackingData.add(indent + location.getSource().getModelId() + " (implicit)");
                indent += "  ";
            }
        } else if (collectStepTrace != null) {
            if (collectStepTrace.getPath().get(0).getArtifact() == null) {
                return;
            }
            baseName = ArtifactIdUtils.toId(collectStepTrace.getPath().get(0).getArtifact())
                    .replace(":", "_");
            trackingFile = trackingDir.resolve(baseName + ext);
            if (Files.exists(trackingFile)) {
                return;
            }

            Artifact resolvedArtifact = event.getArtifact();
            Artifact nodeArtifact = collectStepTrace.getNode().getArtifact();

            if (isInScope(resolvedArtifact, nodeArtifact) || "pom".equals(resolvedArtifact.getExtension())) {
                Dependency node = collectStepTrace.getNode();
                trackingData.add(resolvedArtifact.toString());
                indent += "  ";
                trackingData.add(indent + node + " (" + collectStepTrace.getContext() + ")");
                ListIterator<DependencyNode> iter = collectStepTrace
                        .getPath()
                        .listIterator(collectStepTrace.getPath().size());
                while (iter.hasPrevious()) {
                    DependencyNode curr = iter.previous();
                    indent += "  ";
                    trackingData.add(indent + curr + " (" + collectStepTrace.getContext() + ")");
                }
            }
        }

        if (trackingFile == null) {
            return;
        }
        try {
            Files.createDirectories(trackingDir);

            trackingData.add("");
            if (!missing) {
                if (event.getRepository() != null) {
                    trackingData.add("Repository: " + event.getRepository());
                }
            } else {
                List<RemoteRepository> repositories = new ArrayList<>();
                if (artifactRequest != null && artifactRequest.getRepositories() != null) {
                    repositories.addAll(artifactRequest.getRepositories());
                } else if (artifactDescriptorRequest != null && artifactDescriptorRequest.getRepositories() != null) {
                    repositories.addAll(artifactDescriptorRequest.getRepositories());
                }
                if (!repositories.isEmpty()) {
                    trackingData.add("Configured repositories:");
                    for (RemoteRepository r : repositories) {
                        trackingData.add(" - " + r.getId() + " : " + r.getUrl());
                    }
                } else {
                    trackingData.add("No repositories configured");
                }
            }

            Files.write(trackingFile, trackingData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns {@code true} if passed in artifact is originating from local repository. In other words, we want
     * to process and store tracking information ONLY into local repository, not to any other place. This method
     * filters out currently built artifacts, as events are fired for them as well, but their resolved artifact
     * file would point to checked out source-tree, not the local repository.
     * <p>
     * Visible for testing.
     */
    static boolean isLocalRepositoryArtifactOrMissing(RepositorySystemSession session, Artifact artifact) {
        return artifact.getFile() == null
                || artifact.getFile()
                        .getPath()
                        .startsWith(session.getLocalRepository().getBasedir().getPath());
    }

    /**
     * Unravels trace tree (going upwards from current node), looking for {@link CollectStepData} trace data.
     * This method may return {@code null} if no collect step data found in passed trace data or it's parents.
     * <p>
     * Visible for testing.
     */
    static CollectStepData lookupCollectStepData(RequestTrace trace) {
        CollectStepData collectStepTrace = null;
        while (trace != null) {
            if (trace.getData() instanceof CollectStepData) {
                collectStepTrace = (CollectStepData) trace.getData();
                break;
            }
            trace = trace.getParent();
        }
        return collectStepTrace;
    }

    /**
     * The event "artifact resolved" if fired WHENEVER an artifact is resolved, BUT it happens also when an artifact
     * descriptor (model, the POM) is being built, and parent (and parent of parent...) is being asked for. Hence, this
     * method "filters" out in WHICH artifact are we interested in, but it intentionally neglects extension as
     * ArtifactDescriptorReader modifies extension to "pom" during collect. So all we have to rely on is GAV only.
     */
    static boolean isInScope(Artifact artifact, Artifact nodeArtifact) {
        return Objects.equals(artifact.getGroupId(), nodeArtifact.getGroupId())
                && Objects.equals(artifact.getArtifactId(), nodeArtifact.getArtifactId())
                && Objects.equals(artifact.getVersion(), nodeArtifact.getVersion());
    }
}
