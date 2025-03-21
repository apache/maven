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
package org.apache.maven.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

public class DefaultNode extends AbstractNode {

    protected final @Nonnull InternalSession session;
    protected final @Nonnull org.eclipse.aether.graph.DependencyNode node;
    protected final boolean verbose;

    public DefaultNode(
            @Nonnull InternalSession session, @Nonnull org.eclipse.aether.graph.DependencyNode node, boolean verbose) {
        this.session = session;
        this.node = node;
        this.verbose = verbose;
    }

    @Override
    DependencyNode getDependencyNode() {
        return node;
    }

    @Override
    public Artifact getArtifact() {
        return node.getArtifact() != null ? session.getArtifact(node.getArtifact()) : null;
    }

    @Override
    public Dependency getDependency() {
        return node.getDependency() != null ? session.getDependency(node.getDependency()) : null;
    }

    @Override
    public List<Node> getChildren() {
        return new MappedList<>(node.getChildren(), n -> session.getNode(n, verbose));
    }

    @Override
    public List<RemoteRepository> getRemoteRepositories() {
        return new MappedList<>(node.getRepositories(), session::getRemoteRepository);
    }

    @Override
    public Optional<RemoteRepository> getRepository() {
        // TODO: v4: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns a detailed string representation of this dependency node.
     * <p>
     * When verbose mode is disabled, returns the basic string representation in the format:
     * {@code groupId:artifactId:version[:scope]}
     * <p>
     * When verbose mode is enabled, additional details are included with the following format:
     * <ul>
     *   <li>For included dependencies: {@code groupId:artifactId:version[:scope] (details)}</li>
     *   <li>For omitted dependencies: {@code (groupId:artifactId:version[:scope] - details)}</li>
     * </ul>
     * Where details may include:
     * <ul>
     *   <li>Version management information (if the version was managed from a different version)</li>
     *   <li>Scope management information (if the scope was managed from a different scope)</li>
     *   <li>Scope updates (if the scope was changed during resolution)</li>
     *   <li>Conflict resolution information (if the dependency was omitted due to conflicts or duplicates)</li>
     * </ul>
     *
     * @return a string representation of this dependency node with optional detailed information
     */
    @Nonnull
    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();
        DependencyNode node = getDependencyNode();
        org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
        org.eclipse.aether.graph.Dependency dependency = node.getDependency();

        if (!verbose) {
            sb.append(artifact);
            if (dependency != null) {
                sb.append(":").append(dependency.getScope());
            }
            return sb.toString();
        }

        List<String> details = new ArrayList<>();
        org.eclipse.aether.graph.DependencyNode winner =
                (org.eclipse.aether.graph.DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        String winnerVersion = winner != null ? winner.getArtifact().getBaseVersion() : null;
        boolean included = (winnerVersion == null);

        String preManagedVersion = DependencyManagerUtils.getPremanagedVersion(node);
        if (preManagedVersion != null) {
            details.add("version managed from " + preManagedVersion);
        }

        String preManagedScope = DependencyManagerUtils.getPremanagedScope(node);
        if (preManagedScope != null) {
            details.add("scope managed from " + preManagedScope);
        }

        String originalScope = (String) node.getData().get(ConflictResolver.NODE_DATA_ORIGINAL_SCOPE);
        if (originalScope != null && !originalScope.equals(dependency.getScope())) {
            details.add("scope updated from " + originalScope);
        }

        if (!included) {
            if (Objects.equals(winnerVersion, artifact.getVersion())) {
                details.add("omitted for duplicate");
            } else {
                details.add("omitted for conflict with " + winnerVersion);
            }
        }

        if (!included) {
            sb.append('(');
        }

        sb.append(artifact);
        if (dependency != null) {
            sb.append(":").append(dependency.getScope());
        }

        if (!details.isEmpty()) {
            sb.append(included ? " (" : " - ");
            appendDetails(sb, details);
            sb.append(')');
        }

        if (!included) {
            sb.append(')');
        }

        return sb.toString();
    }

    private static void appendDetails(StringBuilder sb, List<String> details) {
        boolean first = true;
        for (String detail : details) {
            if (first) {
                first = false;
            } else {
                sb.append("; ");
            }
            sb.append(detail);
        }
    }
}
