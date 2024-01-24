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
package org.apache.maven.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;

/**
 * Interface representing a Maven project which can be created using the
 * {@link org.apache.maven.api.services.ProjectBuilder} service.
 * Such objects are immutable and plugin that wish to modify such objects
 * need to do so using the {@link org.apache.maven.api.services.ProjectManager}
 * service.
 * <p>
 * Projects are created using the {@code ProjectBuilder} from a POM file
 * (usually named {@code pom.xml}) on the file system.
 * The {@link #getPomPath()} will point to the POM file and the
 * {@link #getBasedir()} to the directory parent containing the
 * POM file.
 * </p>
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.ProjectManager
 * @see org.apache.maven.api.services.ProjectBuilder
 */
@Experimental
public interface Project {

    /**
     * Returns the project groupId.
     */
    @Nonnull
    String getGroupId();

    /**
     * Returns the project artifactId.
     */
    @Nonnull
    String getArtifactId();

    /**
     * Returns the project version.
     */
    @Nonnull
    String getVersion();

    /**
     * Returns the project packaging.
     * <p>
     * Note: unlike in legacy code, logical checks against string representing packaging (returned by this method)
     * are NOT recommended (code like {@code "pom".equals(project.getPackaging)} must be avoided). Use method
     * {@link #getArtifacts()} to gain access to POM or build artifact.
     *
     * @see #getArtifacts()
     */
    @Nonnull
    Packaging getPackaging();

    /**
     * Returns the project language. It is by default determined by {@link #getPackaging()}.
     *
     * @see #getPackaging()
     */
    @Nonnull
    default Language getLanguage() {
        return getPackaging().language();
    }

    /**
     * Returns the project POM artifact, which is the artifact of the POM of this project. Every project have a POM
     * artifact, even if the existence of backing POM file is NOT a requirement (i.e. for some transient projects).
     *
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default Artifact getPomArtifact() {
        return getArtifacts().get(0);
    }

    /**
     * Returns the project main artifact, which is the artifact produced by this project build, if applicable.
     * This artifact MAY be absent if the project is actually not producing any main artifact (i.e. "pom" packaging).
     *
     * @see #getPackaging()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default Optional<Artifact> getMainArtifact() {
        List<Artifact> artifacts = getArtifacts();
        return artifacts.size() == 2 ? Optional.of(artifacts.get(1)) : Optional.empty();
    }

    /**
     * Returns the project artifacts as immutable list. Elements are the project POM artifact and the artifact
     * produced by this project build, if applicable. Hence, the returned list may have one or two elements
     * (never less than 1, never more than 2), depending on project packaging.
     * <p>
     * The list's first element is ALWAYS the project POM artifact. Presence of second element in the list depends
     * solely on the project packaging.
     *
     * @see #getPackaging()
     * @see #getPomArtifact()
     * @see #getMainArtifact()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    List<Artifact> getArtifacts();

    /**
     * Returns the project model.
     */
    @Nonnull
    Model getModel();

    /**
     * Shorthand method.
     */
    @Nonnull
    default Build getBuild() {
        Build build = getModel().getBuild();
        return build != null ? build : Build.newInstance();
    }

    /**
     * Returns the path to the pom file for this project.
     * A project is usually read from a file named {@code pom.xml},
     * which contains the {@linkplain #getModel() model} in an XML form.
     * When a custom {@code org.apache.maven.api.spi.ModelParser} is used,
     * the path may point to a non XML file.
     * <p>
     * The POM path is also used to define the {@linkplain #getBasedir() base directory}
     * of the project.
     *
     * @return the path of the pom
     * @see #getBasedir()
     */
    @Nonnull
    Path getPomPath();

    /**
     * Returns the project base directory, i.e. the directory containing the project.
     * A project is usually read from the file system and this will point to
     * the directory containing the POM file.
     *
     * @return the path of the directory containing the project
     */
    @Nonnull
    Path getBasedir();

    /**
     * Returns the project direct dependencies (directly specified or inherited).
     */
    @Nonnull
    List<DependencyCoordinate> getDependencies();

    /**
     * Returns the project managed dependencies (directly specified or inherited).
     */
    @Nonnull
    List<DependencyCoordinate> getManagedDependencies();

    /**
     * Returns the project ID, usable as key.
     */
    @Nonnull
    default String getId() {
        return getModel().getId();
    }

    /**
     * Returns a boolean indicating if the project is the top level project for
     * this reactor build.  The top level project may be different from the
     * {@code rootDirectory}, especially if a subtree of the project is being
     * built, either because Maven has been launched in a subdirectory or using
     * a {@code -f} option.
     *
     * @return {@code true} if the project is the top level project for this build
     */
    boolean isTopProject();

    /**
     * Returns a boolean indicating if the project is a root project,
     * meaning that the {@link #getRootDirectory()} and {@link #getBasedir()}
     * points to the same directory, and that either {@link Model#isRoot()}
     * is {@code true} or that {@code basedir} contains a {@code .mvn} child
     * directory.
     *
     * @return {@code true} if the project is the root project
     * @see Model#isRoot()
     */
    boolean isRootProject();

    /**
     * Gets the root directory of the project, which is the parent directory
     * containing the {@code .mvn} directory or flagged with {@code root="true"}.
     *
     * @throws IllegalStateException if the root directory could not be found
     * @see Session#getRootDirectory()
     */
    @Nonnull
    Path getRootDirectory();

    /**
     * Returns project parent project, if any.
     */
    @Nonnull
    Optional<Project> getParent();
}
