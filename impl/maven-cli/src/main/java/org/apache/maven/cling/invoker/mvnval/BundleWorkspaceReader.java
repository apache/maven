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
package org.apache.maven.cling.invoker.mvnval;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Model;
import org.apache.maven.impl.resolver.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * A workspace reader backed by a set of POM files given on the command line.
 * <p>
 * When validating several POMs at once — for instance a deployment bundle whose
 * members reference each other as parents or BOM imports — a parent lookup for a
 * member would otherwise go to the remote repository.  This reader answers those
 * lookups from the bundle itself, so the verdict does not depend on whether the
 * parent has been published yet and the run does not download what it already has
 * on disk.
 * <p>
 * The index is keyed by {@code groupId:artifactId} (GA) only: a bundle is
 * expected to carry at most one version of any given module, and a version
 * mismatch between a child's {@code <parent>} declaration and the parent POM
 * found in the bundle is itself a problem for the validator to report rather
 * than a reason to fall through to the repository.
 * <p>
 * Only POM artifacts are answered here.  Jar lookups and version-range queries
 * always fall through to the local and remote repositories.
 */
class BundleWorkspaceReader implements MavenWorkspaceReader {

    private static final WorkspaceRepository REPOSITORY = new WorkspaceRepository("bundle", null);

    /** GA → POM file path, built once before any validation starts. */
    private final Map<String, File> index;

    /** GA → pre-parsed file model, for {@link MavenWorkspaceReader#findModel}. */
    private final Map<String, Model> models;

    BundleWorkspaceReader(Map<String, File> index, Map<String, Model> models) {
        this.index = Map.copyOf(index);
        this.models = Map.copyOf(models);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return REPOSITORY;
    }

    /**
     * Returns the POM file for the artifact when it is in the bundle, or {@code null} to fall
     * through to the local and remote repositories.
     * <p>
     * Only answers for {@code pom} extension artifacts, never for jars or other types: the bundle
     * contains source POMs, not packaged artifacts.
     */
    @Override
    public File findArtifact(Artifact artifact) {
        if (!"pom".equals(artifact.getExtension())) {
            return null;
        }
        return index.get(ga(artifact.getGroupId(), artifact.getArtifactId()));
    }

    /**
     * Returns the single version in the bundle for this GA, or an empty list to fall through.
     * <p>
     * Version-range resolution is not a concern for POM validation: a child's {@code <parent>}
     * names an exact version and the resolver asks for that version directly via
     * {@link #findArtifact}.  {@code findVersions} is consulted for range queries and LATEST /
     * RELEASE, which do not appear in well-formed parent declarations.
     */
    @Override
    public List<String> findVersions(Artifact artifact) {
        return List.of();
    }

    /**
     * Returns the pre-parsed file model for the artifact when it is in the bundle, or
     * {@code null} to trigger normal model building.
     * <p>
     * Consulted by {@link org.apache.maven.impl.resolver.DefaultArtifactDescriptorReader} to skip
     * re-parsing a POM that the workspace already has in memory.
     */
    @Override
    public Model findModel(Artifact artifact) {
        if (!"pom".equals(artifact.getExtension())) {
            return null;
        }
        return models.get(ga(artifact.getGroupId(), artifact.getArtifactId()));
    }

    private static String ga(String groupId, String artifactId) {
        return groupId + ":" + artifactId;
    }
}
