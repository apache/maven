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
package org.apache.maven.project;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.lifecycle.internal.DefaultProjectArtifactFactory;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The concern of the project is provide runtime values based on the model.
 * <p>
 * The values in the model remain untouched but during the process of building a project notions like inheritance and
 * interpolation can be added. This allows to have an entity which is useful in a runtime while preserving the model so
 * that it can be marshalled and unmarshalled without being tainted by runtime requirements.
 * </p>
 * <p>
 * With changes during 3.2.2 release MavenProject is closer to being immutable after construction with the removal of
 * all components from this class, and the upfront construction taken care of entirely by the {@link ProjectBuilder}.
 * There is still the issue of having to run the lifecycle in order to find all the compile source roots and resource
 * directories but I hope to take care of this during the Maven 4.0 release (jvz).
 * </p>
 */
public class MavenProject implements Cloneable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProject.class);

    public static final String EMPTY_PROJECT_GROUP_ID = "unknown";

    public static final String EMPTY_PROJECT_ARTIFACT_ID = "empty-project";

    public static final String EMPTY_PROJECT_VERSION = "0";

    @Nonnull
    private Model model;

    @Nullable
    private MavenProject parent;

    /**
     * The path to the {@code pom.xml} file. By default, this file is located in {@link #basedir}.
     */
    @Nullable
    private File file;

    /**
     * The project base directory.
     * By default, this is the parent of the {@linkplain #getFile() <abbr>POM</abbr> file}.
     */
    @Nullable
    private File basedir;

    // TODO: what is the difference with {@code basedir}?
    @Nullable
    private Path rootDirectory;

    /**
     * The artifacts specified by {@code setResolvedArtifacts(Set)}. This is for Maven internal usage only.
     * This collection should be handled as if it was <em>immutable</em>,
     * because it may be shared by cloned {@code MavenProject} instances.
     */
    @Nullable
    private Set<Artifact> resolvedArtifacts;

    /**
     * The filter to apply on {@link #resolvedArtifacts} when {@link #artifacts} is lazily populated.
     * May be {@code null} to <em>exclude</em> all resolved artifacts.
     */
    @Nullable
    private ArtifactFilter artifactFilter;

    /**
     * All dependencies that this project has, including transitive ones.
     * May be lazily computed from {@link #resolvedArtifacts}.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nullable // Computed when first requested.
    private Set<Artifact> artifacts;

    @Nullable
    private Artifact parentArtifact;

    /**
     * The plugin dependencies that this project has.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nonnull
    private Set<Artifact> pluginArtifacts;

    /**
     * The artifact repositories of this project.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nonnull
    private List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * The plugin repositories of this project.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nonnull
    private List<ArtifactRepository> pluginArtifactRepositories;

    /**
     * Cached value computed from {@link #remoteArtifactRepositories} when first requested.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nullable // Computed when first requested.
    private List<RemoteRepository> remoteProjectRepositories;

    /**
     * Cached value computed from {@link #pluginArtifactRepositories} when first requested.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nullable // Computed when first requested.
    private List<RemoteRepository> remotePluginRepositories;

    /**
     * List of the attached artifacts to this project.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private List<Artifact> attachedArtifacts;

    @Nullable
    private MavenProject executionProject;

    /**
     * The collected projects.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nonnull
    private List<MavenProject> collectedProjects;

    /**
     * Root directories of source codes to compile.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private List<String> compileSourceRoots;

    /**
     * Root directories of test codes to compile.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private List<String> testCompileSourceRoots;

    /**
     * Root directories of scriot codes.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private List<String> scriptSourceRoots;

    @Nullable
    private ArtifactRepository releaseArtifactRepository;

    @Nullable
    private ArtifactRepository snapshotArtifactRepository;

    /**
     * The active profiles.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nonnull
    private List<Profile> activeProfiles;

    /**
     * The identifiers of all profiles that contributed to this project's effective model.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private Map<String, List<String>> injectedProfileIds;

    /**
     * Direct dependencies that this project has.
     * This is an <em>immutable</em> collection potentially shared by cloned {@code MavenProject} instances.
     */
    @Nullable // For compatibility with previous behavior.
    @Deprecated
    private Set<Artifact> dependencyArtifacts;

    @Nullable
    private Artifact artifact;

    @Nullable // calculated when first requested.
    private Map<String, Artifact> artifactMap;

    @Nullable
    private Model originalModel;

    @Nullable // Computed when first requested.
    private Map<String, Artifact> pluginArtifactMap;

    @Nonnull
    private Set<Artifact> reportArtifacts;

    @Nullable
    private Map<String, Artifact> reportArtifactMap;

    @Nonnull
    private Set<Artifact> extensionArtifacts;

    @Nullable // Computed when first requested.
    private Map<String, Artifact> extensionArtifactMap;

    @Nonnull
    private Map<String, Artifact> managedVersionMap;

    /**
     * Projects by identifiers.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private Map<String, MavenProject> projectReferences;

    private boolean executionRoot;

    @Nullable
    private File parentFile;

    /**
     * Key-value pairs providing context.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private Map<String, Object> context;

    @Nullable
    private ClassRealm classRealm;

    @Nullable
    private DependencyFilter extensionDependencyFilter;

    /**
     * The life cycle phases.
     * This is a <em>modifiable</em> collection which needs to be copied by {@link #deepCopy()}.
     */
    @Nonnull
    private Set<String> lifecyclePhases;

    /**
     * Creates an initially empty project.
     */
    public MavenProject() {
        this(new Model());
        model.setGroupId(EMPTY_PROJECT_GROUP_ID);
        model.setArtifactId(EMPTY_PROJECT_ARTIFACT_ID);
        model.setVersion(EMPTY_PROJECT_VERSION);
    }

    public MavenProject(org.apache.maven.api.model.Model model) {
        this(new Model(model));
    }

    /**
     * Creates a project derived from the given model.
     *
     * @param model the model to wrap in a maven project
     */
    public MavenProject(@Nonnull Model model) {
        // Do not invoke `setModel(Model)` as escaping `this` is deprecated.
        this.model = Objects.requireNonNull(model);

        // Immutable collections.
        pluginArtifacts = Set.of();
        remoteArtifactRepositories = List.of();
        pluginArtifactRepositories = List.of();
        collectedProjects = List.of();
        activeProfiles = List.of();
        reportArtifacts = Set.of();
        extensionArtifacts = Set.of();
        managedVersionMap = Map.of();

        // Mutable collections.
        attachedArtifacts = new ArrayList<>();
        compileSourceRoots = new ArrayList<>();
        testCompileSourceRoots = new ArrayList<>();
        scriptSourceRoots = new ArrayList<>();
        injectedProfileIds = new LinkedHashMap<>();
        projectReferences = new HashMap<>();
        context = new HashMap<>();
        lifecyclePhases = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    /**
     * Creates a copy of the given project.
     *
     * @param project the project to copy
     * @see #clone()
     */
    public MavenProject(@Nonnull MavenProject project) {
        // Do not invoke setter methods. See "this-escaped" compiler warning.
        model = project.getModel();
        parent = project.getParent();
        file = project.getFile();
        basedir = project.getBasedir();
        rootDirectory = project.getRootDirectory();
        artifactFilter = project.artifactFilter; // This internal property has no getter.
        parentArtifact = project.getParentArtifact();
        executionProject = project.executionProject; // Intentionally avoid the getter.
        releaseArtifactRepository = project.getReleaseArtifactRepository();
        snapshotArtifactRepository = project.getSnapshotArtifactRepository();
        artifact = project.getArtifact();
        originalModel = project.getOriginalModel();
        executionRoot = project.isExecutionRoot();
        parentFile = project.getParentFile();
        classRealm = project.getClassRealm();
        extensionDependencyFilter = project.getExtensionDependencyFilter();

        // Immutable collections.
        resolvedArtifacts = project.resolvedArtifacts; // This internal property has no getter.
        artifacts = project.getArtifacts();
        pluginArtifacts = project.getPluginArtifacts();
        remoteArtifactRepositories = project.getRemoteArtifactRepositories();
        pluginArtifactRepositories = project.getPluginArtifactRepositories();
        remoteProjectRepositories = project.getRemoteProjectRepositories();
        remotePluginRepositories = project.getRemotePluginRepositories();
        collectedProjects = project.getCollectedProjects();
        activeProfiles = project.getActiveProfiles();
        dependencyArtifacts = project.getDependencyArtifacts();
        artifactMap = project.getArtifactMap();
        pluginArtifactMap = project.getPluginArtifactMap();
        reportArtifacts = project.getReportArtifacts();
        reportArtifactMap = project.getReportArtifactMap();
        extensionArtifacts = project.getExtensionArtifacts();
        extensionArtifactMap = project.getExtensionArtifactMap();
        managedVersionMap = project.getManagedVersionMap();

        // Mutable collections. Will be copied by `deepCopy()`.
        attachedArtifacts = project.getAttachedArtifacts();
        compileSourceRoots = project.getCompileSourceRoots();
        testCompileSourceRoots = project.getTestCompileSourceRoots();
        scriptSourceRoots = project.getScriptSourceRoots();
        injectedProfileIds = project.getInjectedProfileIds(); // Mutable, but will be copied by `deepCopy()`.
        projectReferences = project.getProjectReferences();
        context = project.context;
        lifecyclePhases = project.lifecyclePhases;
        deepCopy();
    }

    /**
     * Copies in-place the modifiable values of this {@code MavenProject} instance.
     * This method should be invoked after a clone or after the copy constructor.
     * This method should not invoke any user-overrideable method (e.g., no setter).
     */
    private void deepCopy() {
        model = model.clone();
        if (originalModel != null) {
            originalModel = originalModel.clone();
        }
        if (parentFile != null) {
            parentFile = parentFile.getAbsoluteFile();
        }
        attachedArtifacts = new ArrayList<>(attachedArtifacts);
        compileSourceRoots = new ArrayList<>(compileSourceRoots);
        testCompileSourceRoots = new ArrayList<>(testCompileSourceRoots);
        scriptSourceRoots = new ArrayList<>(scriptSourceRoots);
        injectedProfileIds = new LinkedHashMap<>(injectedProfileIds);
        projectReferences = new LinkedHashMap<>(projectReferences);
        context = new LinkedHashMap<>(context);
        lifecyclePhases = Collections.synchronizedSet(new LinkedHashSet<>(lifecyclePhases));
    }

    // TODO: where is the difference with {@code basedir} and {@code rootDirectory}?
    @Nullable
    public File getParentFile() {
        return parentFile;
    }

    public void setParentFile(@Nullable File parentFile) {
        this.parentFile = parentFile;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    @Nullable
    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(@Nullable Artifact target) {
        artifact = target;
    }

    // TODO I would like to get rid of this. jvz.
    @Nonnull
    public Model getModel() {
        return model;
    }

    /**
     * Returns the project corresponding to a declared parent.
     *
     * @return the parent, or null if no parent is declared or there was an error building it
     */
    @Nullable
    public MavenProject getParent() {
        return parent;
    }

    public void setParent(@Nullable MavenProject parent) {
        this.parent = parent;
    }

    public boolean hasParent() {
        return getParent() != null;
    }

    /**
     * {@return the path to the {@code pom.xml} file}.
     * By default, this file is located in the {@linkplain #getBasedir() base directory}.
     */
    @Nullable
    public File getFile() {
        return file;
    }

    /**
     * Sets the {@code pom.xml} file to the given file, and the base directory to the parent of that file.
     *
     * @param file the new {@code pom.xml} file, as a file located in the new base directory
     */
    public void setFile(@Nullable File file) {
        this.file = file;
        this.basedir = file != null ? file.getParentFile() : null;
    }

    /**
     * Sets project {@code file} without changing project {@code basedir}.
     *
     * @param file the new {@code pom.xml} file
     * @since 3.2.4
     */
    public void setPomFile(@Nullable File file) {
        this.file = file;
    }

    /**
     * {@return the project base directory}.
     * By default, this is the parent of the {@linkplain #getFile() <abbr>POM</abbr> file}.
     */
    @Nullable
    public File getBasedir() {
        return basedir;
    }

    public void setDependencies(List<Dependency> dependencies) {
        getModel().setDependencies(dependencies);
    }

    public List<Dependency> getDependencies() {
        return getModel().getDependencies();
    }

    public DependencyManagement getDependencyManagement() {
        return getModel().getDependencyManagement();
    }

    // ----------------------------------------------------------------------
    // Test and compile source roots.
    // ----------------------------------------------------------------------

    private void addPath(List<String> paths, String path) {
        if (path != null) {
            path = path.trim();
            if (!path.isEmpty()) {
                File f = new File(path);
                if (f.isAbsolute()) {
                    path = f.getAbsolutePath();
                } else if (".".equals(path)) {
                    path = getBasedir().getAbsolutePath();
                } else {
                    path = new File(getBasedir(), path).getAbsolutePath();
                }
                if (!paths.contains(path)) {
                    paths.add(path);
                }
            }
        }
    }

    /**
     * If the given path is non-null, adds it to the source root directories. Otherwise, does nothing.
     *
     * @param path the path to add if non-null
     */
    public void addCompileSourceRoot(@Nullable String path) {
        addPath(compileSourceRoots, path);
    }

    /**
     * If the given path is non-null, adds it to the test root directories. Otherwise, does nothing.
     *
     * @param path the path to add if non-null
     */
    public void addTestCompileSourceRoot(@Nullable String path) {
        addPath(testCompileSourceRoots, path);
    }

    /**
     * {@return the source root directories as an unmodifiable list}.
     */
    @Nonnull
    public List<String> getCompileSourceRoots() {
        return Collections.unmodifiableList(compileSourceRoots);
    }

    /**
     * {@return the test root directories as an unmodifiable list}.
     */
    @Nonnull
    public List<String> getTestCompileSourceRoots() {
        return Collections.unmodifiableList(testCompileSourceRoots);
    }

    // TODO let the scope handler deal with this
    private static boolean isCompilePathElement(final String scope) {
        return Artifact.SCOPE_COMPILE.equals(scope)
                || Artifact.SCOPE_PROVIDED.equals(scope)
                || Artifact.SCOPE_SYSTEM.equals(scope);
    }

    // TODO let the scope handler deal with this
    private static boolean isRuntimePathElement(final String scope) {
        return Artifact.SCOPE_COMPILE.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope);
    }

    // TODO let the scope handler deal with this
    private static boolean isTestPathElement(final String scope) {
        return true;
    }

    /**
     * Returns a filtered list of classpath elements. This method is invoked when the caller
     * requested that all dependencies are placed on the classpath, with no module-path element.
     *
     * @param scopeFilter a filter returning {@code true} for the artifact scopes to accept.
     * @param includeTestDir whether to include the test directory in the classpath elements.
     * @return paths of all artifacts placed on the classpath.
     * @throws DependencyResolutionRequiredException if an artifact file is used, but has not been resolved
     */
    private List<String> getClasspathElements(final Predicate<String> scopeFilter, final boolean includeTestDir)
            throws DependencyResolutionRequiredException {
        @SuppressWarnings("LocalVariableHidesMemberVariable") // Usually the same content as the field.
        Set<Artifact> artifacts = getArtifacts();
        final var list = new ArrayList<String>(artifacts.size() + 2);
        final var build = getBuild();
        if (includeTestDir) {
            String d = build.getTestOutputDirectory();
            if (d != null) {
                list.add(d);
            }
        }
        String d = build.getOutputDirectory();
        if (d != null) {
            list.add(d);
        }
        for (Artifact a : artifacts) {
            final File f = a.getFile();
            if (f != null && scopeFilter.test(a.getScope())) {
                final ArtifactHandler h = a.getArtifactHandler();
                if (h.isAddedToClasspath()) {
                    list.add(f.getPath());
                }
            }
        }
        return list;
    }

    /**
     * {@return the elements placed on the classpath for compilation}.
     * This method can be invoked when the caller does not support module-path.
     *
     * @throws DependencyResolutionRequiredException if an artifact file is used, but has not been resolved
     *
     * @deprecated This method is unreliable because it does not consider other dependency properties.
     * See {@link org.apache.maven.api.JavaPathType} instead for better analysis.
     */
    @Deprecated
    public List<String> getCompileClasspathElements() throws DependencyResolutionRequiredException {
        return getClasspathElements(MavenProject::isCompilePathElement, false);
    }

    /**
     * {@return the elements placed on the classpath for tests}.
     * This method can be invoked when the caller does not support module-path.
     *
     * @throws DependencyResolutionRequiredException if an artifact file is used, but has not been resolved
     *
     * @deprecated This method is unreliable because it does not consider other dependency properties.
     * See {@link org.apache.maven.api.JavaPathType} instead for better analysis.
     */
    @Deprecated
    public List<String> getTestClasspathElements() throws DependencyResolutionRequiredException {
        return getClasspathElements(MavenProject::isTestPathElement, true);
    }

    /**
     * {@return the elements placed on the classpath for runtime}.
     * This method can be invoked when the caller does not support module-path.
     *
     * @throws DependencyResolutionRequiredException if an artifact file is used, but has not been resolved
     *
     * @deprecated This method is unreliable because it does not consider other dependency properties.
     * See {@link org.apache.maven.api.JavaPathType} instead for better analysis.
     */
    @Deprecated
    public List<String> getRuntimeClasspathElements() throws DependencyResolutionRequiredException {
        return getClasspathElements(MavenProject::isRuntimePathElement, false);
    }

    // ----------------------------------------------------------------------
    // Delegate to the model
    // ----------------------------------------------------------------------

    public void setModelVersion(String pomVersion) {
        getModel().setModelVersion(pomVersion);
    }

    public String getModelVersion() {
        return getModel().getModelVersion();
    }

    public String getId() {
        return getModel().getId();
    }

    public void setGroupId(String groupId) {
        getModel().setGroupId(groupId);
    }

    public String getGroupId() {
        @SuppressWarnings("LocalVariableHidesMemberVariable") // Usually the same value as the field.
        Model model = getModel();
        String groupId = model.getGroupId();
        if (groupId == null) {
            Parent mp = model.getParent();
            if (mp != null) {
                groupId = mp.getGroupId();
            }
        }
        return groupId;
    }

    public void setArtifactId(String artifactId) {
        getModel().setArtifactId(artifactId);
    }

    public String getArtifactId() {
        return getModel().getArtifactId();
    }

    public void setName(String name) {
        getModel().setName(name);
    }

    public String getName() {
        // TODO this should not be allowed to be null.
        String name = getModel().getName();
        if (name != null) {
            return name;
        } else {
            return getArtifactId();
        }
    }

    public void setVersion(String version) {
        getModel().setVersion(version);
    }

    public String getVersion() {
        @SuppressWarnings("LocalVariableHidesMemberVariable") // Usually the same value as the field.
        Model model = getModel();
        String version = model.getVersion();
        if (version == null) {
            Parent mp = getModel().getParent();
            if (mp != null) {
                version = mp.getVersion();
            }
        }
        return version;
    }

    public String getPackaging() {
        return getModel().getPackaging();
    }

    public void setPackaging(String packaging) {
        getModel().setPackaging(packaging);
    }

    public void setInceptionYear(String inceptionYear) {
        getModel().setInceptionYear(inceptionYear);
    }

    public String getInceptionYear() {
        return getModel().getInceptionYear();
    }

    public void setUrl(String url) {
        getModel().setUrl(url);
    }

    public String getUrl() {
        return getModel().getUrl();
    }

    public Prerequisites getPrerequisites() {
        return getModel().getPrerequisites();
    }

    public void setIssueManagement(IssueManagement issueManagement) {
        getModel().setIssueManagement(issueManagement);
    }

    public CiManagement getCiManagement() {
        return getModel().getCiManagement();
    }

    public void setCiManagement(CiManagement ciManagement) {
        getModel().setCiManagement(ciManagement);
    }

    public IssueManagement getIssueManagement() {
        return getModel().getIssueManagement();
    }

    public void setDistributionManagement(DistributionManagement distributionManagement) {
        getModel().setDistributionManagement(distributionManagement);
    }

    public DistributionManagement getDistributionManagement() {
        return getModel().getDistributionManagement();
    }

    public void setDescription(String description) {
        getModel().setDescription(description);
    }

    public String getDescription() {
        return getModel().getDescription();
    }

    public void setOrganization(Organization organization) {
        getModel().setOrganization(organization);
    }

    public Organization getOrganization() {
        return getModel().getOrganization();
    }

    public void setScm(Scm scm) {
        getModel().setScm(scm);
    }

    public Scm getScm() {
        return getModel().getScm();
    }

    public void setMailingLists(List<MailingList> mailingLists) {
        getModel().setMailingLists(mailingLists);
    }

    public List<MailingList> getMailingLists() {
        return getModel().getMailingLists();
    }

    public void addMailingList(MailingList mailingList) {
        getModel().addMailingList(mailingList);
    }

    public void setDevelopers(List<Developer> developers) {
        getModel().setDevelopers(developers);
    }

    public List<Developer> getDevelopers() {
        return getModel().getDevelopers();
    }

    public void addDeveloper(Developer developer) {
        getModel().addDeveloper(developer);
    }

    public void setContributors(List<Contributor> contributors) {
        getModel().setContributors(contributors);
    }

    public List<Contributor> getContributors() {
        return getModel().getContributors();
    }

    public void addContributor(Contributor contributor) {
        getModel().addContributor(contributor);
    }

    public void setBuild(Build build) {
        getModel().setBuild(build);
    }

    public Build getBuild() {
        return getModelBuild();
    }

    public List<Resource> getResources() {
        return getBuild().getResources();
    }

    public List<Resource> getTestResources() {
        return getBuild().getTestResources();
    }

    public void addResource(Resource resource) {
        getBuild().addResource(resource);
    }

    public void addTestResource(Resource testResource) {
        getBuild().addTestResource(testResource);
    }

    public void setLicenses(List<License> licenses) {
        getModel().setLicenses(licenses);
    }

    public List<License> getLicenses() {
        return getModel().getLicenses();
    }

    public void addLicense(License license) {
        getModel().addLicense(license);
    }

    private static <K, V> Map<K, V> copyWithSameOrder(Map<K, V> items) {
        switch (items.size()) {
            case 0:
                return Map.of();
            case 1:
                return Map.copyOf(items);
            default:
                items = new LinkedHashMap<>(items);
                if (items.containsKey(null)) {
                    throw new NullPointerException("The given map shall not contain the null key.");
                }
                if (items.containsValue(null)) {
                    throw new NullPointerException("The given map shall not contain the null values.");
                }
                return Collections.unmodifiableMap(items);
        }
    }

    private static <E> Set<E> copyWithSameOrder(Collection<E> items) {
        switch (items.size()) {
            case 0:
                return Set.of();
            case 1:
                return Set.copyOf(items);
            default:
                var copy = new LinkedHashSet<>(items);
                if (copy.contains(null)) {
                    throw new NullPointerException("The given set shall not contain the null element.");
                }
                return Collections.unmodifiableSet(copy);
        }
    }

    /**
     * Sets all dependencies that this project has, including transitive ones.
     * The given set is copied: changes to the given set after this method call has no effect on this object.
     * This copy is for ensuring that {@link #getArtifactMap()} stay consistent with the values given to this method.
     *
     * <h4>Side effects</h4>
     * Invoking this method will also modify the values returned by {@link #getArtifactMap()}.
     *
     * @param artifacts the new project dependencies
     */
    public void setArtifacts(@Nonnull Set<Artifact> artifacts) {
        this.artifacts = copyWithSameOrder(artifacts);
        artifactMap = null; // flush the calculated artifactMap
    }

    /**
     * All dependencies that this project has, including transitive ones. Contents are lazily populated, so depending on
     * what phases have run dependencies in some scopes won't be included. e.g. if only compile phase has run,
     * dependencies with scope test won't be included.
     *
     * @return unmodifiable set of dependencies
     */
    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The returned set is unmodifiable.
    public Set<Artifact> getArtifacts() {
        if (artifacts == null) {
            if (artifactFilter == null || resolvedArtifacts == null) {
                artifacts = Set.of();
            } else {
                artifacts = copyWithSameOrder(resolvedArtifacts.stream()
                        .filter((a) -> artifactFilter.include(a))
                        .toList());
            }
        }
        return artifacts;
    }

    @Nonnull
    public Map<String, Artifact> getArtifactMap() {
        if (artifactMap == null) {
            artifactMap = ArtifactUtils.artifactMapByVersionlessId(getArtifacts());
        }
        return Collections.unmodifiableMap(artifactMap);
    }

    /**
     * Sets all plugins that this project has, including transitive ones.
     * The given set is copied: changes to the given set after this method call has no effect on this object.
     * This copy is for ensuring that {@link #getPluginArtifactMap()} stay consistent with the values given
     * to this method.
     *
     * <h4>Side effects</h4>
     * Invoking this method will also modify the values returned by {@link #getPluginArtifactMap()}.
     *
     * @param pluginArtifacts the new project plugins
     */
    public void setPluginArtifacts(@Nonnull Set<Artifact> pluginArtifacts) {
        this.pluginArtifacts = copyWithSameOrder(pluginArtifacts);
        this.pluginArtifactMap = null;
    }

    /**
     * All plugins that this project has.
     *
     * @return unmodifiable set of plugins
     */
    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The returned set is unmodifiable.
    public Set<Artifact> getPluginArtifacts() {
        return pluginArtifacts;
    }

    @Nonnull
    public Map<String, Artifact> getPluginArtifactMap() {
        if (pluginArtifactMap == null) {
            pluginArtifactMap = ArtifactUtils.artifactMapByVersionlessId(getPluginArtifacts());
        }
        return Collections.unmodifiableMap(pluginArtifactMap);
    }

    public void setParentArtifact(@Nullable Artifact parentArtifact) {
        this.parentArtifact = parentArtifact;
    }

    @Nullable
    public Artifact getParentArtifact() {
        return parentArtifact;
    }

    public List<Repository> getRepositories() {
        return getModel().getRepositories();
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List<Plugin> getBuildPlugins() {
        Build build = getModel().getBuild();
        if (build == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(build.getPlugins());
    }

    public List<String> getModules() {
        @SuppressWarnings("LocalVariableHidesMemberVariable") // Usually the same value as the field.
        Model model = getModel();
        List<String> subprojects = model.getDelegate().getSubprojects();
        if (!subprojects.isEmpty()) {
            return subprojects;
        }
        return model.getModules();
    }

    public PluginManagement getPluginManagement() {
        PluginManagement pluginMgmt = null;
        Build build = getModel().getBuild();
        if (build != null) {
            pluginMgmt = build.getPluginManagement();
        }
        return pluginMgmt;
    }

    private Build getModelBuild() {
        @SuppressWarnings("LocalVariableHidesMemberVariable") // Usually the same value as the field.
        Model model = getModel();
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }
        return build;
    }

    /**
     * Sets the artifact repositories of this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     * This copy is for ensuring that {@link #getRemoteProjectRepositories()} stay consistent with the values
     * given to this method.
     *
     * @param remoteArtifactRepositories the new artifact repositories
     */
    public void setRemoteArtifactRepositories(@Nonnull List<ArtifactRepository> remoteArtifactRepositories) {
        this.remoteArtifactRepositories = List.copyOf(remoteArtifactRepositories);
        this.remoteProjectRepositories = null; // Recompute when first requested.
    }

    /**
     * {@return the artifact repositories of this project}. The returned list is unmodifiable for ensuring
     * that {@link #getRemoteProjectRepositories()} stay consistent with the values returned by this method.
     */
    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The returned list is unmodifiable.
    public List<ArtifactRepository> getRemoteArtifactRepositories() {
        return remoteArtifactRepositories;
    }

    /**
     * Sets the plugin repositories of this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     * This copy is for ensuring that {@link #getRemotePluginRepositories()} stay consistent with the values
     * given to this method.
     *
     * @param pluginArtifactRepositories the new artifact repositories
     */
    public void setPluginArtifactRepositories(@Nonnull List<ArtifactRepository> pluginArtifactRepositories) {
        this.pluginArtifactRepositories = List.copyOf(pluginArtifactRepositories);
        this.remotePluginRepositories = null; // Recompute when first requested.
    }

    /**
     * {@return the plugin repositories of this project}. The returned list is unmodifiable for ensuring
     * that {@link #getRemotePluginRepositories()} stay consistent with the values returned by this method.
     */
    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The returned list is unmodifiable.
    public List<ArtifactRepository> getPluginArtifactRepositories() {
        return pluginArtifactRepositories;
    }

    public ArtifactRepository getDistributionManagementArtifactRepository() {
        if (getArtifact().isSnapshot()) {
            ArtifactRepository sar = getSnapshotArtifactRepository();
            if (sar != null) {
                return sar;
            }
        }
        return getReleaseArtifactRepository();
    }

    public List<Repository> getPluginRepositories() {
        return getModel().getPluginRepositories();
    }

    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The cached list is already unmodifiable.
    public List<RemoteRepository> getRemoteProjectRepositories() {
        if (remoteProjectRepositories == null) {
            remoteProjectRepositories = RepositoryUtils.toRepos(getRemoteArtifactRepositories());
        }
        return remoteProjectRepositories;
    }

    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The cached list is already unmodifiable.
    public List<RemoteRepository> getRemotePluginRepositories() {
        if (remotePluginRepositories == null) {
            remotePluginRepositories = RepositoryUtils.toRepos(getPluginArtifactRepositories());
        }
        return remotePluginRepositories;
    }

    /**
     * Sets the active profiles of this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     *
     * @param activeProfiles the new active profiles of this project
     */
    public void setActiveProfiles(@Nonnull List<Profile> activeProfiles) {
        this.activeProfiles = List.copyOf(activeProfiles);
    }

    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The list is already unmodifiable.
    public List<Profile> getActiveProfiles() {
        return activeProfiles;
    }

    public void setInjectedProfileIds(@Nonnull String source, @Nullable List<String> injectedProfileIds) {
        Objects.requireNonNull(source);
        if (injectedProfileIds != null) {
            this.injectedProfileIds.put(source, List.copyOf(injectedProfileIds));
        } else {
            this.injectedProfileIds.remove(source);
        }
    }

    /**
     * Gets the identifiers of all profiles that contributed to this project's effective model. This includes active
     * profiles from the project's POM and all its parent POMs as well as from external sources like the
     * {@code settings.xml}. The profile identifiers are grouped by the identifier of their source, e.g.
     * {@code <groupId>:<artifactId>:<version>} for a POM profile or {@code external} for profiles from the
     * {@code settings.xml}.
     *
     * @return The identifiers of all injected profiles, indexed by the source from which the profiles originated
     */
    @Nonnull
    public Map<String, List<String>> getInjectedProfileIds() {
        return Collections.unmodifiableMap(injectedProfileIds);
    }

    /**
     * Add or replace an artifact. This method is now deprecated. Use the @{MavenProjectHelper} to attach artifacts to a
     * project. In spite of the 'throws' declaration on this API, this method has never thrown an exception since Maven
     * 3.0.x. Historically, it logged and ignored a second addition of the same g/a/v/c/t. Now it replaces the file for
     * the artifact, so that plugins (e.g. shade) can change the pathname of the file for a particular set of
     * coordinates.
     *
     * @param artifact the artifact to add or replace.
     * @deprecated Please use {@link MavenProjectHelper}
     * @throws DuplicateArtifactAttachmentException will never happen but leave it for backward compatibility
     */
    @Deprecated
    public void addAttachedArtifact(@Nonnull Artifact artifact) throws DuplicateArtifactAttachmentException {
        if (artifact == null) {
            return; // While we document this method as non-null, we observe that some callers provide a null value.
        }
        // if already there we remove it and add again
        int index = attachedArtifacts.indexOf(artifact);
        if (index >= 0) {
            LOGGER.warn("artifact '{}' already attached, replacing previous instance", artifact);
            attachedArtifacts.set(index, artifact);
        } else {
            attachedArtifacts.add(artifact);
        }
    }

    /**
     * Returns a read-only list of the attached artifacts to this project.
     *
     * @return the attached artifacts of this project
     */
    @Nonnull
    public List<Artifact> getAttachedArtifacts() {
        return Collections.unmodifiableList(attachedArtifacts);
    }

    public Xpp3Dom getGoalConfiguration(
            String pluginGroupId, String pluginArtifactId, String executionId, String goalId) {
        Xpp3Dom dom = null;
        List<Plugin> plugins = getBuildPlugins();
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                if (pluginGroupId.equals(plugin.getGroupId()) && pluginArtifactId.equals(plugin.getArtifactId())) {
                    dom = (Xpp3Dom) plugin.getConfiguration();
                    if (executionId != null) {
                        for (PluginExecution execution : plugin.getExecutions()) {
                            if (executionId.equals(execution.getId())) {
                                // NOTE: The PluginConfigurationExpander already merged the plugin-level config in
                                dom = (Xpp3Dom) execution.getConfiguration();
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        if (dom != null) {
            // make a copy so the original in the POM doesn't get messed with
            dom = new Xpp3Dom(dom);
        }
        return dom;
    }

    @Nonnull
    public MavenProject getExecutionProject() {
        return (executionProject == null ? this : executionProject);
    }

    public void setExecutionProject(@Nullable MavenProject executionProject) {
        this.executionProject = executionProject;
    }

    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The list is already unmodifiable.
    public List<MavenProject> getCollectedProjects() {
        return collectedProjects;
    }

    /**
     * Sets the collected project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     *
     * @param collectedProjects the collected projects
     */
    public void setCollectedProjects(@Nonnull List<MavenProject> collectedProjects) {
        this.collectedProjects = List.copyOf(collectedProjects);
    }

    /**
     * {@return the direct dependencies that this project has, or null if none}.
     * <em>Note that this method returns {@code null} instead of an empty set when there is no dependencies.</em>
     * This unusual behavior is kept for compatibility reasons with existing codes, which test for nullity instead
     * of emptiness.
     *
     * @see #getArtifacts() to get all transitive dependencies
     */
    @Deprecated
    @Nullable // For compatibility with previous behavior.
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The set is already unmodifiable.
    public Set<Artifact> getDependencyArtifacts() {
        return dependencyArtifacts;
    }

    @Deprecated
    public void setDependencyArtifacts(@Nonnull Set<Artifact> dependencyArtifacts) {
        this.dependencyArtifacts = copyWithSameOrder(dependencyArtifacts);
    }

    public void setReleaseArtifactRepository(@Nullable ArtifactRepository releaseArtifactRepository) {
        this.releaseArtifactRepository = releaseArtifactRepository;
    }

    public void setSnapshotArtifactRepository(@Nullable ArtifactRepository snapshotArtifactRepository) {
        this.snapshotArtifactRepository = snapshotArtifactRepository;
    }

    public void setOriginalModel(@Nullable Model originalModel) {
        this.originalModel = originalModel;
    }

    @Nullable
    public Model getOriginalModel() {
        return originalModel;
    }

    public void setManagedVersionMap(@Nonnull Map<String, Artifact> map) {
        managedVersionMap = copyWithSameOrder(map);
    }

    @Nonnull
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The map is already unmodifiable.
    public Map<String, Artifact> getManagedVersionMap() {
        return managedVersionMap;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof MavenProject)) {
            return false;
        }
        MavenProject that = (MavenProject) other;
        return Objects.equals(getArtifactId(), that.getArtifactId())
                && Objects.equals(getGroupId(), that.getGroupId())
                && Objects.equals(getVersion(), that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroupId(), getArtifactId(), getVersion());
    }

    public List<Extension> getBuildExtensions() {
        Build build = getBuild();
        if (build != null) {
            List<Extension> extensions = build.getExtensions();
            if (extensions != null) {
                return Collections.unmodifiableList(extensions);
            }
        }
        return Collections.emptyList();
    }

    public void addProjectReference(@Nonnull MavenProject project) {
        projectReferences.put(
                getProjectReferenceId(project.getGroupId(), project.getArtifactId(), project.getVersion()), project);
    }

    public Properties getProperties() {
        return getModel().getProperties();
    }

    public List<String> getFilters() {
        return getBuild().getFilters();
    }

    @Nonnull
    public Map<String, MavenProject> getProjectReferences() {
        return Collections.unmodifiableMap(projectReferences);
    }

    public boolean isExecutionRoot() {
        return executionRoot;
    }

    public void setExecutionRoot(boolean executionRoot) {
        this.executionRoot = executionRoot;
    }

    public String getDefaultGoal() {
        return getBuild().getDefaultGoal();
    }

    public Plugin getPlugin(String pluginKey) {
        return getBuild().getPluginsAsMap().get(pluginKey);
    }

    /**
     * {@return a string representation for debugging purposes}.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128).append("MavenProject: ");
        sb.append(getGroupId()).append(':').append(getArtifactId()).append(':').append(getVersion());
        File f = getFile();
        if (f != null) {
            sb.append(" @ ").append(f.getPath());
        }
        return sb.toString();
    }

    /**
     * {@return a deep copy of this object}.
     *
     * @since 2.0.9
     *
     * @see #MavenProject(MavenProject)
     */
    @Override
    public MavenProject clone() {
        MavenProject clone;
        try {
            clone = (MavenProject) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // Should never happen since this class is cloneable.
        }
        clone.deepCopy();
        return clone;
    }

    public void setModel(@Nonnull Model model) {
        this.model = Objects.requireNonNull(model);
    }

    /**
     * Sets the artifacts attached to this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     *
     * @param attachedArtifacts the new artifacts attached to this project
     */
    protected void setAttachedArtifacts(@Nonnull List<Artifact> attachedArtifacts) {
        this.attachedArtifacts.clear();
        this.attachedArtifacts.addAll(attachedArtifacts);
    }

    /**
     * Sets the source root directories of this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     *
     * @param compileSourceRoots the new source root directories
     */
    protected void setCompileSourceRoots(@Nonnull List<String> compileSourceRoots) {
        this.compileSourceRoots.clear();
        this.compileSourceRoots.addAll(compileSourceRoots);
    }

    /**
     * Sets the test source directories of this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     *
     * @param testCompileSourceRoots the new test source directories
     */
    protected void setTestCompileSourceRoots(@Nonnull List<String> testCompileSourceRoots) {
        this.testCompileSourceRoots.clear();
        this.testCompileSourceRoots.addAll(testCompileSourceRoots);
    }

    @Nullable
    protected ArtifactRepository getReleaseArtifactRepository() {
        return releaseArtifactRepository;
    }

    @Nullable
    protected ArtifactRepository getSnapshotArtifactRepository() {
        return snapshotArtifactRepository;
    }

    private static String getProjectReferenceId(String groupId, String artifactId, String version) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(groupId).append(':').append(artifactId).append(':').append(version);
        return buffer.toString();
    }

    /**
     * Sets the value of the context value of this project identified by the given key. If the supplied value is
     * {@code null}, the context value is removed from this project. Context values are intended to allow core
     * extensions to associate derived state with project instances.
     *
     * @param key the key to associate to a value
     * @param value the value to associate to the given key, or {@code null} for removing the association
     */
    public void setContextValue(@Nonnull String key, @Nullable Object value) {
        Objects.requireNonNull(key);
        if (value != null) {
            context.put(key, value);
        } else {
            context.remove(key);
        }
    }

    /**
     * Returns context value of this project associated with the given key or null if this project has no such value.
     *
     * @param key the key of the value to get
     * @return the associated value, or {@code null} if none
     */
    public Object getContextValue(String key) {
        return context.get(Objects.requireNonNull(key));
    }

    /**
     * Sets the project's class realm. <strong>Warning:</strong> This is an internal utility method that is only public
     * for technical reasons, it is not part of the public API. In particular, this method can be changed or deleted
     * without prior notice and must not be used by plugins.
     *
     * @param classRealm The class realm hosting the build extensions of this project, may be {@code null}.
     * @hidden
     */
    public void setClassRealm(ClassRealm classRealm) {
        this.classRealm = classRealm;
    }

    /**
     * Gets the project's class realm. This class realm hosts the build extensions of the project.
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @return The project's class realm or {@code null}.
     * @hidden
     */
    public ClassRealm getClassRealm() {
        return classRealm;
    }

    /**
     * Sets the artifact filter used to exclude shared extension artifacts from plugin realms. <strong>Warning:</strong>
     * This is an internal utility method that is only public for technical reasons, it is not part of the public API.
     * In particular, this method can be changed or deleted without prior notice and must not be used by plugins.
     *
     * @param extensionDependencyFilter The dependency filter to apply to plugins, may be {@code null}.
     * @hidden
     */
    public void setExtensionDependencyFilter(DependencyFilter extensionDependencyFilter) {
        this.extensionDependencyFilter = extensionDependencyFilter;
    }

    /**
     * Gets the dependency filter used to exclude shared extension artifacts from plugin realms.
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @return The dependency filter or {@code null}.
     * @hidden
     */
    public DependencyFilter getExtensionDependencyFilter() {
        return extensionDependencyFilter;
    }

    /**
     * Sets the transitive dependency artifacts that have been resolved/collected for this project.
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param artifacts the set of artifacts, may be {@code null}
     * @hidden
     */
    public void setResolvedArtifacts(@Nullable Set<Artifact> artifacts) {
        this.resolvedArtifacts = copyWithSameOrder(artifacts);
        this.artifacts = null;
        this.artifactMap = null;
    }

    /**
     * Sets the scope filter to select the artifacts being exposed to the currently executed mojo.
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param artifactFilter the artifact filter, may be {@code null} to exclude all artifacts
     * @hidden
     */
    public void setArtifactFilter(@Nullable ArtifactFilter artifactFilter) {
        this.artifactFilter = artifactFilter;
        this.artifacts = null;
        this.artifactMap = null;
    }

    /**
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param phase The phase to check for, must not be {@code null}.
     * @return {@code true} if the phase has been seen.
     * @hidden
     */
    public boolean hasLifecyclePhase(String phase) {
        return lifecyclePhases.contains(phase);
    }

    /**
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param lifecyclePhase The lifecycle phase to add, must not be {@code null}.
     * @hidden
     */
    public void addLifecyclePhase(String lifecyclePhase) {
        lifecyclePhases.add(lifecyclePhase);
    }

    // ----------------------------------------------------------------------------------------------------------------
    //
    //
    // D E P R E C A T E D
    //
    //
    // ----------------------------------------------------------------------------------------------------------------
    //
    // Everything below will be removed for Maven 4.0.0
    //
    // ----------------------------------------------------------------------------------------------------------------

    private ProjectBuildingRequest projectBuilderConfiguration;

    private Map<String, String> moduleAdjustments;

    @Deprecated // This appears only to be used in test code
    public String getModulePathAdjustment(MavenProject moduleProject) throws IOException {
        // FIXME: This is hacky. What if module directory doesn't match artifactid, and parent
        // is coming from the repository??
        String module = moduleProject.getArtifactId();
        File moduleFile = moduleProject.getFile();
        if (moduleFile != null) {
            File moduleDir = moduleFile.getCanonicalFile().getParentFile();
            module = moduleDir.getName();
        }
        if (moduleAdjustments == null) {
            moduleAdjustments = new HashMap<>();
            List<String> modules = getModules();
            if (modules != null) {
                for (String modulePath : modules) {
                    String moduleName = modulePath;
                    if (moduleName.endsWith("/") || moduleName.endsWith("\\")) {
                        moduleName = moduleName.substring(0, moduleName.length() - 1);
                    }
                    int lastSlash = moduleName.lastIndexOf('/');
                    if (lastSlash < 0) {
                        lastSlash = moduleName.lastIndexOf('\\');
                    }
                    String adjustment = null;
                    if (lastSlash > -1) {
                        moduleName = moduleName.substring(lastSlash + 1);
                        adjustment = modulePath.substring(0, lastSlash);
                    }
                    moduleAdjustments.put(moduleName, adjustment);
                }
            }
        }
        return moduleAdjustments.get(module);
    }

    @Deprecated
    public Set<Artifact> createArtifacts(ArtifactFactory artifactFactory, String inheritedScope, ArtifactFilter filter)
            throws InvalidDependencyVersionException {
        return DefaultProjectArtifactFactory.createArtifacts(
                artifactFactory, getModel().getDependencies(), inheritedScope, filter, this);
    }

    /**
     * Sets the test script directories of this project.
     * The given list is copied: changes to the given list after this method call has no effect on this object.
     *
     * @param scriptSourceRoots the new test script directories
     */
    @Deprecated
    protected void setScriptSourceRoots(@Nonnull List<String> scriptSourceRoots) {
        this.scriptSourceRoots.clear();
        this.scriptSourceRoots.addAll(scriptSourceRoots);
    }

    @Deprecated
    public void addScriptSourceRoot(@Nullable String path) {
        addPath(scriptSourceRoots, path);
    }

    @Nonnull
    @Deprecated
    public List<String> getScriptSourceRoots() {
        return Collections.unmodifiableList(scriptSourceRoots);
    }

    @Nonnull
    @Deprecated
    public List<Artifact> getCompileArtifacts() {
        return getClasspathArtifacts(MavenProject::isCompilePathElement);
    }

    @Nonnull
    @Deprecated
    public List<Dependency> getCompileDependencies() {
        return getDependencies(MavenProject::isCompilePathElement);
    }

    @Nonnull
    @Deprecated
    public List<Artifact> getTestArtifacts() {
        return getClasspathArtifacts(MavenProject::isAddedToClasspath);
    }

    @Nonnull
    @Deprecated
    public List<Dependency> getTestDependencies() {
        return getDependencies(MavenProject::isTestPathElement);
    }

    @Nonnull
    @Deprecated // used by the Maven ITs
    public List<Dependency> getRuntimeDependencies() {
        return getDependencies(MavenProject::isRuntimePathElement);
    }

    @Nonnull
    @Deprecated
    public List<Artifact> getRuntimeArtifacts() {
        return getClasspathArtifacts(MavenProject::isRuntimePathElement);
    }

    @Nonnull
    @Deprecated
    public List<String> getSystemClasspathElements() throws DependencyResolutionRequiredException {
        Stream<String> s1 = Optional.ofNullable(getBuild().getOutputDirectory()).stream();
        Stream<String> s2 = getArtifacts().stream()
                .filter(MavenProject::isAddedToClasspath)
                .filter(MavenProject::isScopeSystem)
                .map((Artifact::getFile))
                .filter(Objects::nonNull)
                .map(File::getPath);
        return Stream.concat(s1, s2).toList();
    }

    @Nonnull
    @Deprecated
    public List<Artifact> getSystemArtifacts() {
        return getClasspathArtifacts(MavenProject::isScopeSystem);
    }

    @Nonnull
    @Deprecated
    public List<Dependency> getSystemDependencies() {
        return getDependencies(MavenProject::isScopeSystem);
    }

    // TODO let the scope handler deal with this
    private static boolean isCompilePathElement(Artifact a) {
        return isCompilePathElement(a.getScope());
    }

    // TODO let the scope handler deal with this
    private static boolean isRuntimePathElement(Artifact a) {
        return isRuntimePathElement(a.getScope());
    }

    // TODO let the scope handler deal with this
    private static boolean isTestPathElement(Artifact a) {
        return isTestPathElement(a.getScope());
    }

    // TODO let the scope handler deal with this
    private static boolean isScopeSystem(Artifact a) {
        return Artifact.SCOPE_SYSTEM.equals(a.getScope());
    }

    // TODO classpath check doesn't belong here - that's the other method
    private static boolean isAddedToClasspath(Artifact a) {
        return a.getArtifactHandler().isAddedToClasspath();
    }

    private List<Artifact> getClasspathArtifacts(final Predicate<Artifact> filter) {
        return getArtifacts().stream()
                .filter(MavenProject::isAddedToClasspath)
                .filter(filter)
                .toList();
    }

    private List<Dependency> getDependencies(final Predicate<Artifact> filter) {
        return getArtifacts().stream()
                .filter(filter)
                .map(MavenProject::toDependency)
                .toList();
    }

    private static Dependency toDependency(Artifact a) {
        Dependency dependency = new Dependency();
        dependency.setArtifactId(a.getArtifactId());
        dependency.setGroupId(a.getGroupId());
        dependency.setVersion(a.getVersion());
        dependency.setScope(a.getScope());
        dependency.setType(a.getType());
        dependency.setClassifier(a.getClassifier());
        return dependency;
    }

    @Deprecated
    public void setReporting(Reporting reporting) {
        getModel().setReporting(reporting);
    }

    @Deprecated
    public Reporting getReporting() {
        return getModel().getReporting();
    }

    @Deprecated
    public void setReportArtifacts(@Nonnull Set<Artifact> reportArtifacts) {
        this.reportArtifacts = copyWithSameOrder(reportArtifacts);
        reportArtifactMap = null;
    }

    @Deprecated
    public Set<Artifact> getReportArtifacts() {
        return Collections.unmodifiableSet(reportArtifacts);
    }

    @Nonnull
    @Deprecated
    public Map<String, Artifact> getReportArtifactMap() {
        if (reportArtifactMap == null) {
            reportArtifactMap = ArtifactUtils.artifactMapByVersionlessId(getReportArtifacts());
        }
        return Collections.unmodifiableMap(reportArtifactMap);
    }

    @Deprecated
    public void setExtensionArtifacts(@Nonnull Set<Artifact> extensionArtifacts) {
        this.extensionArtifacts = copyWithSameOrder(extensionArtifacts);
        extensionArtifactMap = null;
    }

    @Nonnull
    @Deprecated
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // The set is already unmodifiable.
    public Set<Artifact> getExtensionArtifacts() {
        return extensionArtifacts;
    }

    @Nonnull
    @Deprecated
    public Map<String, Artifact> getExtensionArtifactMap() {
        if (extensionArtifactMap == null) {
            extensionArtifactMap = ArtifactUtils.artifactMapByVersionlessId(getExtensionArtifacts());
        }
        return Collections.unmodifiableMap(extensionArtifactMap);
    }

    @Deprecated
    public List<ReportPlugin> getReportPlugins() {
        if (getModel().getReporting() == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(getModel().getReporting().getPlugins());
    }

    @Deprecated
    public Xpp3Dom getReportConfiguration(String pluginGroupId, String pluginArtifactId, String reportSetId) {
        Xpp3Dom dom = null;
        // ----------------------------------------------------------------------
        // I would like to be able to look up the Mojo object using a key but
        // we have a limitation in modello that will be remedied shortly. So
        // for now I have to iterate through and see what we have.
        // ----------------------------------------------------------------------
        if (getReportPlugins() != null) {
            for (ReportPlugin plugin : getReportPlugins()) {
                if (pluginGroupId.equals(plugin.getGroupId()) && pluginArtifactId.equals(plugin.getArtifactId())) {
                    dom = (Xpp3Dom) plugin.getConfiguration();
                    if (reportSetId != null) {
                        ReportSet reportSet = plugin.getReportSetsAsMap().get(reportSetId);
                        if (reportSet != null) {
                            Xpp3Dom executionConfiguration = (Xpp3Dom) reportSet.getConfiguration();
                            if (executionConfiguration != null) {
                                Xpp3Dom newDom = new Xpp3Dom(executionConfiguration);
                                dom = Xpp3Dom.mergeXpp3Dom(newDom, dom);
                            }
                        }
                    }
                    break;
                }
            }
        }
        if (dom != null) {
            // make a copy so the original in the POM doesn't get messed with
            dom = new Xpp3Dom(dom);
        }
        return dom;
    }

    /**
     * @deprecated Use MavenProjectHelper.attachArtifact(..) instead.
     */
    @Deprecated
    public void attachArtifact(String type, String classifier, File file) {}

    /**
     * @deprecated Use {@link org.apache.maven.model.io.ModelWriter}.
     */
    @Deprecated
    public void writeModel(Writer writer) throws IOException {
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write(writer, getModel());
    }

    /**
     * @deprecated Use {@link org.apache.maven.model.io.ModelWriter}.
     */
    @Deprecated
    public void writeOriginalModel(Writer writer) throws IOException {
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write(writer, getOriginalModel());
    }

    @Deprecated
    public Artifact replaceWithActiveArtifact(Artifact pluginArtifact) {
        return pluginArtifact;
    }

    /**
     * Gets the project building request from which this project instance was created. <strong>Warning:</strong> This is
     * a utility method that is meant to assist integrators of Maven, it must not be used by Maven plugins.
     *
     * @return The project building request or {@code null}.
     * @since 2.1
     */
    @Deprecated
    public ProjectBuildingRequest getProjectBuildingRequest() {
        return projectBuilderConfiguration;
    }

    /**
     * Sets the project building request from which this project instance was created. <strong>Warning:</strong> This is
     * a utility method that is meant to assist integrators of Maven, it must not be used by Maven plugins.
     *
     * @param projectBuildingRequest The project building request, may be {@code null}.
     * @since 2.1
     */
    @Deprecated // used by maven-dependency-tree
    public void setProjectBuildingRequest(ProjectBuildingRequest projectBuildingRequest) {
        this.projectBuilderConfiguration = projectBuildingRequest;
    }

    /**
     * TODO: what is the difference with {@code basedir}?
     *
     * @return the root directory for this project
     * @throws IllegalStateException if the root directory cannot be found
     * @since 4.0.0
     */
    @Nonnull
    public Path getRootDirectory() {
        if (rootDirectory == null) {
            throw new IllegalStateException(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
        }
        return rootDirectory;
    }

    @Nullable
    public void setRootDirectory(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }
}
