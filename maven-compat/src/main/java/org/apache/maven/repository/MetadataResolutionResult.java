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
package org.apache.maven.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.CyclicDependencyException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;

/**
 *
 *
 * @author Oleg Gusakov
 *
 */
public class MetadataResolutionResult {
    private Artifact originatingArtifact;

    private List<Artifact> missingArtifacts;

    // Exceptions

    private List<Exception> exceptions;

    private List<Exception> versionRangeViolations;

    private List<ArtifactResolutionException> metadataResolutionExceptions;

    private List<CyclicDependencyException> circularDependencyExceptions;

    private List<ArtifactResolutionException> errorArtifactExceptions;

    // file system errors

    private List<ArtifactRepository> repositories;

    private Set<Artifact> requestedArtifacts;

    private Set<Artifact> artifacts;

    private MetadataGraph dirtyTree;

    private MetadataGraph resolvedTree;

    private MetadataGraph resolvedGraph;

    public Artifact getOriginatingArtifact() {
        return originatingArtifact;
    }

    public MetadataResolutionResult listOriginatingArtifact(final Artifact originatingArtifact) {
        this.originatingArtifact = originatingArtifact;

        return this;
    }

    public void addArtifact(Artifact artifact) {
        if (artifacts == null) {
            artifacts = new LinkedHashSet<>();
        }

        artifacts.add(artifact);
    }

    public Set<Artifact> getArtifacts() {
        return artifacts;
    }

    public void addRequestedArtifact(Artifact artifact) {
        if (requestedArtifacts == null) {
            requestedArtifacts = new LinkedHashSet<>();
        }

        requestedArtifacts.add(artifact);
    }

    public Set<Artifact> getRequestedArtifacts() {
        return requestedArtifacts;
    }

    public boolean hasMissingArtifacts() {
        return missingArtifacts != null && !missingArtifacts.isEmpty();
    }

    public List<Artifact> getMissingArtifacts() {
        return missingArtifacts == null
                ? Collections.<Artifact>emptyList()
                : Collections.unmodifiableList(missingArtifacts);
    }

    public MetadataResolutionResult addMissingArtifact(Artifact artifact) {
        missingArtifacts = initList(missingArtifacts);

        missingArtifacts.add(artifact);

        return this;
    }

    public MetadataResolutionResult setUnresolvedArtifacts(final List<Artifact> unresolvedArtifacts) {
        this.missingArtifacts = unresolvedArtifacts;

        return this;
    }

    // ------------------------------------------------------------------------
    // Exceptions
    // ------------------------------------------------------------------------

    public boolean hasExceptions() {
        return exceptions != null && !exceptions.isEmpty();
    }

    public List<Exception> getExceptions() {
        return exceptions == null ? Collections.<Exception>emptyList() : Collections.unmodifiableList(exceptions);
    }

    // ------------------------------------------------------------------------
    // Version Range Violations
    // ------------------------------------------------------------------------

    public boolean hasVersionRangeViolations() {
        return versionRangeViolations != null;
    }

    /**
     * TODO this needs to accept a {@link OverConstrainedVersionException} as returned by
     *       {@link #getVersionRangeViolation(int)} but it's not used like that in
     *       {@link org.apache.maven.repository.legacy.resolver.DefaultLegacyArtifactCollector}
     */
    public MetadataResolutionResult addVersionRangeViolation(Exception e) {
        versionRangeViolations = initList(versionRangeViolations);

        versionRangeViolations.add(e);

        exceptions = initList(exceptions);

        exceptions.add(e);

        return this;
    }

    public OverConstrainedVersionException getVersionRangeViolation(int i) {
        return (OverConstrainedVersionException) versionRangeViolations.get(i);
    }

    public List<Exception> getVersionRangeViolations() {
        return versionRangeViolations == null
                ? Collections.<Exception>emptyList()
                : Collections.unmodifiableList(versionRangeViolations);
    }

    // ------------------------------------------------------------------------
    // Metadata Resolution Exceptions: ArtifactResolutionExceptions
    // ------------------------------------------------------------------------

    public boolean hasMetadataResolutionExceptions() {
        return metadataResolutionExceptions != null;
    }

    public MetadataResolutionResult addMetadataResolutionException(ArtifactResolutionException e) {
        metadataResolutionExceptions = initList(metadataResolutionExceptions);

        metadataResolutionExceptions.add(e);

        exceptions = initList(exceptions);

        exceptions.add(e);

        return this;
    }

    public ArtifactResolutionException getMetadataResolutionException(int i) {
        return metadataResolutionExceptions.get(i);
    }

    public List<ArtifactResolutionException> getMetadataResolutionExceptions() {
        return metadataResolutionExceptions == null
                ? Collections.<ArtifactResolutionException>emptyList()
                : Collections.unmodifiableList(metadataResolutionExceptions);
    }

    // ------------------------------------------------------------------------
    // ErrorArtifactExceptions: ArtifactResolutionExceptions
    // ------------------------------------------------------------------------

    public boolean hasErrorArtifactExceptions() {
        return errorArtifactExceptions != null;
    }

    public MetadataResolutionResult addError(Exception e) {
        exceptions = initList(exceptions);

        exceptions.add(e);

        return this;
    }

    public List<ArtifactResolutionException> getErrorArtifactExceptions() {
        if (errorArtifactExceptions == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(errorArtifactExceptions);
    }

    // ------------------------------------------------------------------------
    // Circular Dependency Exceptions
    // ------------------------------------------------------------------------

    public boolean hasCircularDependencyExceptions() {
        return circularDependencyExceptions != null;
    }

    public MetadataResolutionResult addCircularDependencyException(CyclicDependencyException e) {
        circularDependencyExceptions = initList(circularDependencyExceptions);

        circularDependencyExceptions.add(e);

        exceptions = initList(exceptions);

        exceptions.add(e);

        return this;
    }

    public CyclicDependencyException getCircularDependencyException(int i) {
        return circularDependencyExceptions.get(i);
    }

    public List<CyclicDependencyException> getCircularDependencyExceptions() {
        if (circularDependencyExceptions == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(circularDependencyExceptions);
    }

    // ------------------------------------------------------------------------
    // Repositories
    // ------------------------------------------------------------------------

    public List<ArtifactRepository> getRepositories() {
        if (repositories == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(repositories);
    }

    public MetadataResolutionResult setRepositories(final List<ArtifactRepository> repositories) {
        this.repositories = repositories;

        return this;
    }

    //
    // Internal
    //

    private <T> List<T> initList(final List<T> l) {
        if (l == null) {
            return new ArrayList<>();
        }
        return l;
    }

    public String toString() {
        if (artifacts == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(256);
        int i = 1;
        sb.append("---------\n");
        sb.append(artifacts.size()).append('\n');
        for (Artifact a : artifacts) {
            sb.append(i).append(' ').append(a).append('\n');
            i++;
        }
        sb.append("---------\n");
        return sb.toString();
    }

    public MetadataGraph getResolvedTree() {
        return resolvedTree;
    }

    public void setResolvedTree(MetadataGraph resolvedTree) {
        this.resolvedTree = resolvedTree;
    }
}
