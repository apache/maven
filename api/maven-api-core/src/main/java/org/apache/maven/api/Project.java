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
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;

/**
 * Interface representing a Maven project which can be created using the
 * {@link org.apache.maven.api.services.ProjectBuilder ProjectBuilder} service.
 * Such objects are immutable and plugin that wish to modify such objects
 * need to do so using the {@link org.apache.maven.api.services.ProjectManager
 * ProjectManager} service.
 * <p>
 * Projects are created using the {@code ProjectBuilder} from a POM file
 * (usually named {@code pom.xml}) on the file system.
 * The {@link #pomPath()} will point to the POM file and the
 * {@link #basedir()} to the directory parent containing the
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
     * {@return the project groupId}.
     */
    @Nonnull
    String groupId();

    /**
     * {@return the project groupId}.
     *
     * @deprecated Use {@link #groupId()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default String getGroupId() {
        return groupId();
    }

    /**
     * {@return the project artifactId}.
     */
    @Nonnull
    String artifactId();

    /**
     * {@return the project artifactId}.
     *
     * @deprecated Use {@link #artifactId()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default String getArtifactId() {
        return artifactId();
    }

    /**
     * {@return the project version}.
     */
    @Nonnull
    String version();

    /**
     * {@return the project version}.
     *
     * @deprecated Use {@link #version()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default String getVersion() {
        return version();
    }

    /**
     * {@return the project packaging}.
     * <p>
     * Note: unlike in legacy code, logical checks against string representing packaging (returned by this method)
     * are NOT recommended (code like {@code "pom".equals(project.getPackaging)} must be avoided). Use method
     * {@link #artifacts()} to gain access to POM or build artifact.
     *
     * @see #artifacts()
     */
    @Nonnull
    Packaging packaging();

    /**
     * {@return the project packaging}.
     * <p>
     * Note: unlike in legacy code, logical checks against string representing packaging (returned by this method)
     * are NOT recommended (code like {@code "pom".equals(project.getPackaging)} must be avoided). Use method
     * {@link #getArtifacts()} to gain access to POM or build artifact.
     *
     * @see #getArtifacts()
     * @deprecated Use {@link #packaging()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Packaging getPackaging() {
        return packaging();
    }

    /**
     * {@return the project language}. It is by default determined by {@link #packaging()}.
     *
     * @see #packaging()
     */
    @Nonnull
    default Language language() {
        return packaging().language();
    }

    /**
     * {@return the project language}. It is by default determined by {@link #getPackaging()}.
     *
     * @see #getPackaging()
     * @deprecated Use {@link #language()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Language getLanguage() {
        return language();
    }

    /**
     * {@return the project POM artifact}, which is the artifact of the POM of this project. Every project have a POM
     * artifact, even if the existence of backing POM file is NOT a requirement (i.e. for some transient projects).
     *
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default ProducedArtifact pomArtifact() {
        return artifacts().get(0);
    }

    /**
     * {@return the project POM artifact}, which is the artifact of the POM of this project. Every project have a POM
     * artifact, even if the existence of backing POM file is NOT a requirement (i.e. for some transient projects).
     *
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     * @deprecated Use {@link #pomArtifact()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default ProducedArtifact getPomArtifact() {
        return pomArtifact();
    }

    /**
     * {@return the project main artifact}, which is the artifact produced by this project build, if applicable.
     * This artifact MAY be absent if the project is actually not producing any main artifact (i.e. "pom" packaging).
     *
     * @see #packaging()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default Optional<ProducedArtifact> mainArtifact() {
        List<ProducedArtifact> artifacts = artifacts();
        return artifacts.size() == 2 ? Optional.of(artifacts.get(1)) : Optional.empty();
    }

    /**
     * {@return the project main artifact}, which is the artifact produced by this project build, if applicable.
     * This artifact MAY be absent if the project is actually not producing any main artifact (i.e. "pom" packaging).
     *
     * @see #getPackaging()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     * @deprecated Use {@link #mainArtifact()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Optional<ProducedArtifact> getMainArtifact() {
        return mainArtifact();
    }

    /**
     * {@return the project artifacts as immutable list}. Elements are the project POM artifact and the artifact
     * produced by this project build, if applicable. Hence, the returned list may have one or two elements
     * (never less than 1, never more than 2), depending on project packaging.
     * <p>
     * The list's first element is ALWAYS the project POM artifact. Presence of second element in the list depends
     * solely on the project packaging.
     *
     * @see #packaging()
     * @see #pomArtifact()
     * @see #mainArtifact()
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    List<ProducedArtifact> artifacts();

    /**
     * {@return the project artifacts as immutable list}. Elements are the project POM artifact and the artifact
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
     * @deprecated Use {@link #artifacts()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<ProducedArtifact> getArtifacts() {
        return artifacts();
    }

    /**
     * {@return the project model}.
     */
    @Nonnull
    Model model();

    /**
     * {@return the project model}.
     *
     * @deprecated Use {@link #model()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Model getModel() {
        return model();
    }

    /**
     * Shorthand method.
     *
     * @return the build element of the project model
     */
    @Nonnull
    default Build build() {
        Build build = model().getBuild();
        return build != null ? build : Build.newInstance();
    }

    /**
     * Shorthand method.
     *
     * @return the build element of the project model
     * @deprecated Use {@link #build()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Build getBuild() {
        return build();
    }

    /**
     * Returns the path to the pom file for this project.
     * A project is usually read from a file named {@code pom.xml},
     * which contains the {@linkplain #model() model} in an XML form.
     * When a custom {@code org.apache.maven.api.spi.ModelParser} is used,
     * the path may point to a non XML file.
     * <p>
     * The POM path is also used to define the {@linkplain #basedir() base directory}
     * of the project.
     *
     * @return the path of the pom
     * @see #basedir()
     */
    @Nonnull
    Path pomPath();

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
     * @deprecated Use {@link #pomPath()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Path getPomPath() {
        return pomPath();
    }

    /**
     * Returns the project base directory, i.e. the directory containing the project.
     * A project is usually read from the file system and this will point to
     * the directory containing the POM file.
     *
     * @return the path of the directory containing the project
     */
    @Nonnull
    Path basedir();

    /**
     * Returns the project base directory, i.e. the directory containing the project.
     * A project is usually read from the file system and this will point to
     * the directory containing the POM file.
     *
     * @return the path of the directory containing the project
     * @deprecated Use {@link #basedir()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Path getBasedir() {
        return basedir();
    }

    /**
     * {@return the absolute path to the directory where files generated by the build are placed}
     * <p>
     * <strong>Purpose:</strong> This method provides the base output directory for a given scope,
     * which serves as the destination for compiled classes, processed resources, and other generated files.
     * The returned path is always absolute.
     * </p>
     * <p>
     * <strong>Scope-based Directory Resolution:</strong>
     * </p>
     * <table class="striped">
     *   <caption>Output Directory by Scope</caption>
     *   <thead>
     *     <tr>
     *       <th>Scope Parameter</th>
     *       <th>Build Configuration</th>
     *       <th>Typical Path</th>
     *       <th>Contents</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td>{@link ProjectScope#MAIN}</td>
     *       <td>{@code build.getOutputDirectory()}</td>
     *       <td>{@code target/classes}</td>
     *       <td>Compiled application classes and processed main resources</td>
     *     </tr>
     *     <tr>
     *       <td>{@link ProjectScope#TEST}</td>
     *       <td>{@code build.getTestOutputDirectory()}</td>
     *       <td>{@code target/test-classes}</td>
     *       <td>Compiled test classes and processed test resources</td>
     *     </tr>
     *     <tr>
     *       <td>{@code null} or other</td>
     *       <td>{@code build.getDirectory()}</td>
     *       <td>{@code target}</td>
     *       <td>Parent directory for all build outputs</td>
     *     </tr>
     *   </tbody>
     * </table>
     * <p>
     * <strong>Role in {@link SourceRoot} Path Resolution:</strong>
     * </p>
     * <p>
     * This method is the foundation for {@link SourceRoot#targetPath(Project)} path resolution.
     * When a {@link SourceRoot} has a relative {@code targetPath}, it is resolved against the
     * output directory returned by this method for the source root's scope. This ensures that:
     * </p>
     * <ul>
     *   <li>Main resources with {@code targetPath="META-INF"} are copied to {@code target/classes/META-INF}</li>
     *   <li>Test resources with {@code targetPath="test-data"} are copied to {@code target/test-classes/test-data}</li>
     *   <li>Resources without an explicit {@code targetPath} are copied to the root of the output directory</li>
     * </ul>
     * <p>
     * <strong>Maven 3 Compatibility:</strong>
     * </p>
     * <p>
     * This behavior maintains the Maven 3.x semantic where resource {@code targetPath} elements
     * are resolved relative to the appropriate output directory ({@code project.build.outputDirectory}
     * or {@code project.build.testOutputDirectory}), <strong>not</strong> the project base directory.
     * </p>
     * <p>
     * In Maven 3, when a resource configuration specifies:
     * </p>
     * <pre>{@code
     * <resource>
     *   <directory>src/main/resources</directory>
     *   <targetPath>META-INF/resources</targetPath>
     * </resource>
     * }</pre>
     * <p>
     * The maven-resources-plugin resolves {@code targetPath} as:
     * {@code project.build.outputDirectory + "/" + targetPath}, which results in
     * {@code target/classes/META-INF/resources}. This method provides the same base directory
     * ({@code target/classes}) for Maven 4 API consumers.
     * </p>
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * Project project = ...; // project at /home/user/myproject
     *
     * // Get main output directory
     * Path mainOutput = project.outputDirectory(ProjectScope.MAIN);
     * // Result: /home/user/myproject/target/classes
     *
     * // Get test output directory
     * Path testOutput = project.outputDirectory(ProjectScope.TEST);
     * // Result: /home/user/myproject/target/test-classes
     *
     * // Get build directory
     * Path buildDir = project.outputDirectory(null);
     * // Result: /home/user/myproject/target
     * }</pre>
     *
     * @param scope the scope of the generated files for which to get the directory, or {@code null} for the build directory
     * @return the absolute path to the output directory for the given scope
     *
     * @see SourceRoot#targetPath(Project)
     * @see SourceRoot#targetPath()
     * @see Build#getOutputDirectory()
     * @see Build#getTestOutputDirectory()
     * @see Build#getDirectory()
     */
    @Nonnull
    default Path outputDirectory(@Nullable ProjectScope scope) {
        String dir;
        Build build = build();
        if (scope == ProjectScope.MAIN) {
            dir = build.getOutputDirectory();
        } else if (scope == ProjectScope.TEST) {
            dir = build.getTestOutputDirectory();
        } else {
            dir = build.getDirectory();
        }
        return basedir().resolve(dir);
    }

    /**
     * {@return the absolute path to the directory where files generated by the build are placed}
     *
     * @param scope the scope of the generated files for which to get the directory, or {@code null} for the build directory
     * @return the absolute path to the output directory for the given scope
     * @deprecated Use {@link #outputDirectory(ProjectScope)} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Path getOutputDirectory(@Nullable ProjectScope scope) {
        return outputDirectory(scope);
    }

    /**
     * {@return the project direct dependencies (directly specified or inherited)}.
     */
    @Nonnull
    List<DependencyCoordinates> dependencies();

    /**
     * {@return the project direct dependencies (directly specified or inherited)}.
     *
     * @deprecated Use {@link #dependencies()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<DependencyCoordinates> getDependencies() {
        return dependencies();
    }

    /**
     * {@return the project managed dependencies (directly specified or inherited)}.
     */
    @Nonnull
    List<DependencyCoordinates> managedDependencies();

    /**
     * {@return the project managed dependencies (directly specified or inherited)}.
     *
     * @deprecated Use {@link #managedDependencies()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<DependencyCoordinates> getManagedDependencies() {
        return managedDependencies();
    }

    /**
     * {@return the project ID, usable as key}.
     */
    @Nonnull
    default String getId() {
        return model().getId();
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
     * meaning that the {@link #rootDirectory()} and {@link #basedir()}
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
     * @return the root directory of the project
     * @throws IllegalStateException if the root directory could not be found
     * @see Session#getRootDirectory()
     */
    @Nonnull
    Path rootDirectory();

    /**
     * Gets the root directory of the project, which is the parent directory
     * containing the {@code .mvn} directory or flagged with {@code root="true"}.
     *
     * @return the root directory of the project
     * @throws IllegalStateException if the root directory could not be found
     * @see Session#getRootDirectory()
     * @deprecated Use {@link #rootDirectory()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Path getRootDirectory() {
        return rootDirectory();
    }

    /**
     * Returns project parent project, if any.
     * <p>
     * Note that the model may have a parent defined, but an empty parent
     * project may be returned if the parent comes from a remote repository,
     * as a {@code Project} must refer to a buildable project.
     *
     * @return an optional containing the parent project
     * @see Model#getParent()
     */
    @Nonnull
    Optional<Project> parent();

    /**
     * Returns project parent project, if any.
     * <p>
     * Note that the model may have a parent defined, but an empty parent
     * project may be returned if the parent comes from a remote repository,
     * as a {@code Project} must refer to a buildable project.
     *
     * @return an optional containing the parent project
     * @see Model#getParent()
     * @deprecated Use {@link #parent()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Optional<Project> getParent() {
        return parent();
    }

    /**
     * Returns all profiles defined in this project.
     * <p>
     * This method returns only the profiles defined directly in the current project's POM
     * and does not include profiles from parent projects.
     *
     * @return a non-null, possibly empty list of profiles defined in this project
     * @see Profile
     * @see #effectiveProfiles()
     */
    @Nonnull
    List<Profile> declaredProfiles();

    /**
     * Returns all profiles defined in this project.
     * <p>
     * This method returns only the profiles defined directly in the current project's POM
     * and does not include profiles from parent projects.
     *
     * @return a non-null, possibly empty list of profiles defined in this project
     * @see Profile
     * @see #getEffectiveProfiles()
     * @deprecated Use {@link #declaredProfiles()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<Profile> getDeclaredProfiles() {
        return declaredProfiles();
    }

    /**
     * Returns all profiles defined in this project and all of its parent projects.
     * <p>
     * This method traverses the parent hierarchy and includes profiles defined in parent POMs.
     * The returned list contains profiles from the current project and all of its ancestors in
     * the project inheritance chain.
     *
     * @return a non-null, possibly empty list of all profiles from this project and its parents
     * @see Profile
     * @see #declaredProfiles()
     */
    @Nonnull
    List<Profile> effectiveProfiles();

    /**
     * Returns all profiles defined in this project and all of its parent projects.
     * <p>
     * This method traverses the parent hierarchy and includes profiles defined in parent POMs.
     * The returned list contains profiles from the current project and all of its ancestors in
     * the project inheritance chain.
     *
     * @return a non-null, possibly empty list of all profiles from this project and its parents
     * @see Profile
     * @see #getDeclaredProfiles()
     * @deprecated Use {@link #effectiveProfiles()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<Profile> getEffectiveProfiles() {
        return effectiveProfiles();
    }

    /**
     * Returns all active profiles for the current project build.
     * <p>
     * Active profiles are those that have been explicitly activated through one of the following means:
     * <ul>
     *   <li>Command line activation using the -P flag</li>
     *   <li>Maven settings activation in settings.xml via &lt;activeProfiles&gt;</li>
     *   <li>Automatic activation via &lt;activation&gt; conditions</li>
     *   <li>The default active profile (marked with &lt;activeByDefault&gt;true&lt;/activeByDefault&gt;)</li>
     * </ul>
     * <p>
     * The active profiles control various aspects of the build configuration including but not
     * limited to dependencies, plugins, properties, and build resources.
     *
     * @return a non-null, possibly empty list of active profiles for this project
     * @see Profile
     * @see #effectiveActiveProfiles()
     */
    @Nonnull
    List<Profile> declaredActiveProfiles();

    /**
     * Returns all active profiles for the current project build.
     * <p>
     * Active profiles are those that have been explicitly activated through one of the following means:
     * <ul>
     *   <li>Command line activation using the -P flag</li>
     *   <li>Maven settings activation in settings.xml via &lt;activeProfiles&gt;</li>
     *   <li>Automatic activation via &lt;activation&gt; conditions</li>
     *   <li>The default active profile (marked with &lt;activeByDefault&gt;true&lt;/activeByDefault&gt;)</li>
     * </ul>
     * <p>
     * The active profiles control various aspects of the build configuration including but not
     * limited to dependencies, plugins, properties, and build resources.
     *
     * @return a non-null, possibly empty list of active profiles for this project
     * @see Profile
     * @see #getEffectiveActiveProfiles()
     * @deprecated Use {@link #declaredActiveProfiles()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<Profile> getDeclaredActiveProfiles() {
        return declaredActiveProfiles();
    }

    /**
     * Returns all active profiles for this project and all of its parent projects.
     * <p>
     * This method traverses the parent hierarchy and collects all active profiles from
     * the current project and its ancestors. Active profiles are those that meet the
     * activation criteria through explicit activation or automatic conditions.
     * <p>
     * The combined set of active profiles from the entire project hierarchy affects
     * the effective build configuration.
     *
     * @return a non-null, possibly empty list of all active profiles from this project and its parents
     * @see Profile
     * @see #declaredActiveProfiles()
     */
    @Nonnull
    List<Profile> effectiveActiveProfiles();

    /**
     * Returns all active profiles for this project and all of its parent projects.
     * <p>
     * This method traverses the parent hierarchy and collects all active profiles from
     * the current project and its ancestors. Active profiles are those that meet the
     * activation criteria through explicit activation or automatic conditions.
     * <p>
     * The combined set of active profiles from the entire project hierarchy affects
     * the effective build configuration.
     *
     * @return a non-null, possibly empty list of all active profiles from this project and its parents
     * @see Profile
     * @see #getDeclaredActiveProfiles()
     * @deprecated Use {@link #effectiveActiveProfiles()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default List<Profile> getEffectiveActiveProfiles() {
        return effectiveActiveProfiles();
    }
}
