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
package org.apache.maven.artifact.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;

/**
 * ResolutionNode
 */
public class ResolutionNode {
    private Artifact artifact;

    private List<ResolutionNode> children;

    private final List<Object> parents;

    private final int depth;

    private final ResolutionNode parent;

    private final List<ArtifactRepository> remoteRepositories;

    private boolean active = true;

    private List<Artifact> trail;

    public ResolutionNode(Artifact artifact, List<ArtifactRepository> remoteRepositories) {
        this.artifact = artifact;
        this.remoteRepositories = remoteRepositories;
        depth = 0;
        parents = Collections.emptyList();
        parent = null;
    }

    public ResolutionNode(Artifact artifact, List<ArtifactRepository> remoteRepositories, ResolutionNode parent) {
        this.artifact = artifact;
        this.remoteRepositories = remoteRepositories;
        depth = parent.depth + 1;
        parents = new ArrayList<>();
        parents.addAll(parent.parents);
        parents.add(parent.getKey());
        this.parent = parent;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public Object getKey() {
        return artifact.getDependencyConflictId();
    }

    public void addDependencies(
            Set<Artifact> artifacts, List<ArtifactRepository> remoteRepositories, ArtifactFilter filter)
            throws CyclicDependencyException, OverConstrainedVersionException {
        if (artifacts != null && !artifacts.isEmpty()) {
            children = new ArrayList<>(artifacts.size());

            for (Artifact a : artifacts) {
                if (parents.contains(a.getDependencyConflictId())) {
                    a.setDependencyTrail(getDependencyTrail());

                    throw new CyclicDependencyException("A dependency has introduced a cycle", a);
                }

                children.add(new ResolutionNode(a, remoteRepositories, this));
            }
            children = Collections.unmodifiableList(children);
        } else {
            children = Collections.emptyList();
        }
        trail = null;
    }

    /**
     * @return {@link List} &lt; {@link String} &gt; with artifact ids
     * @throws OverConstrainedVersionException
     */
    public List<String> getDependencyTrail() throws OverConstrainedVersionException {
        List<Artifact> trial = getTrail();

        List<String> ret = new ArrayList<>(trial.size());

        for (Artifact artifact : trial) {
            ret.add(artifact.getId());
        }

        return ret;
    }

    private List<Artifact> getTrail() throws OverConstrainedVersionException {
        if (trail == null) {
            List<Artifact> ids = new LinkedList<>();
            ResolutionNode node = this;
            while (node != null) {
                Artifact artifact = node.getArtifact();
                if (artifact.getVersion() == null) {
                    // set the recommended version
                    ArtifactVersion selected = artifact.getSelectedVersion();
                    // MNG-2123: null is a valid response to getSelectedVersion, don't
                    // assume it won't ever be.
                    if (selected != null) {
                        artifact.selectVersion(selected.toString());
                    } else {
                        throw new OverConstrainedVersionException(
                                "Unable to get a selected Version for " + artifact.getArtifactId(), artifact);
                    }
                }

                ids.add(0, artifact);
                node = node.parent;
            }
            trail = ids;
        }
        return trail;
    }

    public boolean isResolved() {
        return children != null;
    }

    /**
     * Test whether the node is direct or transitive dependency.
     */
    public boolean isChildOfRootNode() {
        return parent != null && parent.parent == null;
    }

    public Iterator<ResolutionNode> getChildrenIterator() {
        return children.iterator();
    }

    public int getDepth() {
        return depth;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public boolean isActive() {
        return active;
    }

    public void enable() {
        active = true;

        // TODO if it was null, we really need to go find them now... or is this taken care of by the ordering?
        if (children != null) {
            for (ResolutionNode node : children) {
                node.enable();
            }
        }
    }

    public void disable() {
        active = false;
        if (children != null) {
            for (ResolutionNode node : children) {
                node.disable();
            }
        }
    }

    public boolean filterTrail(ArtifactFilter filter) throws OverConstrainedVersionException {
        boolean success = true;
        if (filter != null) {
            for (Artifact artifact : getTrail()) {
                if (!filter.include(artifact)) {
                    success = false;
                }
            }
        }
        return success;
    }

    @Override
    public String toString() {
        return artifact.toString() + " (" + depth + "; " + (active ? "enabled" : "disabled") + ")";
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
}
