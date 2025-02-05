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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.impl.DefaultSourceRoot;
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

    private Model model;

    private MavenProject parent;

    private File file;

    private File basedir;

    private Path rootDirectory;

    private Set<Artifact> resolvedArtifacts;

    private ArtifactFilter artifactFilter;

    private Set<Artifact> artifacts;

    private Artifact parentArtifact;

    private Set<Artifact> pluginArtifacts;

    @Deprecated
    private List<ArtifactRepository> remoteArtifactRepositories;

    @Deprecated
    private List<ArtifactRepository> pluginArtifactRepositories;

    private List<RemoteRepository> remoteProjectRepositories;

    private List<RemoteRepository> remotePluginRepositories;

    private List<Artifact> attachedArtifacts = new ArrayList<>();

    private MavenProject executionProject;

    private List<MavenProject> collectedProjects;

    /**
     * A tuple of {@link SourceRoot} properties for which we decide that no duplicated value should exist in a project.
     * The set of properties that we choose to put in this record may be modified in any future Maven version.
     * The intent is to detect some configuration errors.
     */
    private record SourceKey(ProjectScope scope, Language language, Path directory) {
        /**
         * Converts this key into a source root.
         * Used for adding a new source when no other information is available.
         *
         * @return the source root for the properties of this key.
         */
        SourceRoot createSource() {
            return new DefaultSourceRoot(scope, language, directory);
        }

        /**
         * {@return an error message to report when a conflict is detected}.
         *
         * @param baseDir value of {@link #getBaseDirectory()}, in order to make the message shorter
         */
        String conflictMessage(Path baseDir) {
            return "Directory " + baseDir.relativize(directory)
                    + " is specified twice for the scope \"" + scope.id()
                    + "\" and language \"" + language.id() + "\".";
        }
    }

    /**
     * All sources of this project. The map includes main and test codes for all languages.
     * However, we put some restrictions on what information can be repeated.
     * Those restrictions are expressed in {@link SourceKey}.
     */
    private HashMap<SourceKey, SourceRoot> sources = new LinkedHashMap<>(); // Need access to the `clone()` method.

    @Deprecated
    private ArtifactRepository releaseArtifactRepository;

    @Deprecated
    private ArtifactRepository snapshotArtifactRepository;

    private List<Profile> activeProfiles = new ArrayList<>();

    private Map<String, List<String>> injectedProfileIds = new LinkedHashMap<>();

    @Deprecated
    private Set<Artifact> dependencyArtifacts;

    private Artifact artifact;

    // calculated.
    private Map<String, Artifact> artifactMap;

    private Model originalModel;

    private Map<String, Artifact> pluginArtifactMap;

    @Deprecated
    private Set<Artifact> reportArtifacts;

    @Deprecated
    private Map<String, Artifact> reportArtifactMap;

    @Deprecated
    private Set<Artifact> extensionArtifacts;

    @Deprecated
    private Map<String, Artifact> extensionArtifactMap;

    private Map<String, Artifact> managedVersionMap;

    private Map<String, MavenProject> projectReferences = new HashMap<>();

    private boolean executionRoot;

    private File parentFile;

    private Map<String, Object> context;

    private ClassRealm classRealm;

    private DependencyFilter extensionDependencyFilter;

    private final Set<String> lifecyclePhases = Collections.synchronizedSet(new LinkedHashSet<>());

    public MavenProject() {
        Model model = new Model();

        model.setGroupId(EMPTY_PROJECT_GROUP_ID);
        model.setArtifactId(EMPTY_PROJECT_ARTIFACT_ID);
        model.setVersion(EMPTY_PROJECT_VERSION);

        setModel(model);
    }

    public MavenProject(org.apache.maven.api.model.Model model) {
        this(new Model(model));
    }

    public MavenProject(Model model) {
        setModel(model);
    }

    public MavenProject(MavenProject project) {
        deepCopy(project);
    }

    public File getParentFile() {
        return parentFile;
    }

    public void setParentFile(File parentFile) {
        this.parentFile = parentFile;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    // TODO I would like to get rid of this. jvz.
    public Model getModel() {
        return model;
    }

    /**
     * Returns the project corresponding to a declared parent.
     *
     * @return the parent, or null if no parent is declared or there was an error building it
     */
    public MavenProject getParent() {
        return parent;
    }

    public void setParent(MavenProject parent) {
        this.parent = parent;
    }

    public boolean hasParent() {
        return getParent() != null;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.basedir = file != null ? file.getParentFile() : null;
    }

    /**
     * Sets project {@code file} without changing project {@code basedir}.
     *
     * @since 3.2.4
     */
    public void setPomFile(File file) {
        this.file = file;
    }

    /**
     * @deprecated Replaced by {@link #getBaseDirectory()} for migrating from {@code File} to {@code Path}.
     */
    @Deprecated(since = "4.0.0")
    public File getBasedir() {
        return basedir;
    }

    /**
     * {@return the base directory of this project}.
     * All source files are relative to this directory, unless they were specified as absolute paths.
     *
     * @since 4.0.0
     */
    public Path getBaseDirectory() {
        return getBasedir().toPath();
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

    /**
     * Adds the given source. If a source already exists for the given scope, language and directory,
     * then this method either does nothing if all other properties are equal, or thrown
     * {@linkplain IllegalArgumentException} otherwise.
     *
     * @param source the source to add
     * @throws IllegalArgumentException if a source exists for the given language, scope and directory
     *         but with different values for the other properties.
     *
     * @see #getSourceRoots()
     *
     * @since 4.0.0
     */
    public void addSourceRoot(SourceRoot source) {
        var key = new SourceKey(source.scope(), source.language(), source.directory());
        SourceRoot current = sources.putIfAbsent(key, source);
        if (current != null && !current.equals(source)) {
            throw new IllegalArgumentException(key.conflictMessage(getBaseDirectory()));
        }
    }

    /**
     * Resolves and adds the given directory as a source with the given scope and language.
     * First, this method resolves the given root against the {@linkplain #getBaseDirectory() base directory},
     * then normalizes the path. If a source already exists for the same scope, language and normalized directory,
     * this method does nothing. Otherwise, the normalized directory is added as a new {@link SourceRoot} element.
     *
     * @param scope scope (main or test) of the directory to add
     * @param language language of the files contained in the directory to add
     * @param directory the directory to add if not already present in the source
     *
     * @see #getEnabledSourceRoots(ProjectScope, Language)
     *
     * @since 4.0.0
     */
    public void addSourceRoot(ProjectScope scope, Language language, Path directory) {
        directory = getBaseDirectory().resolve(directory).normalize();
        var key = new SourceKey(scope, language, directory);
        sources.computeIfAbsent(key, SourceKey::createSource);
    }

    /**
     * Resolves and adds the given directory as a source with the given scope and language.
     * If the given directory is null, blank or already in the sources, then this method does nothing.
     * Otherwise, the directory is converted to a path, resolved, normalized and finally added as a new
     * {@link SourceRoot} element if no source exists for these scope, language and normalized directory.
     *
     * @param scope scope (main or test) of the directory to add
     * @param language language of the files contained in the directory to add
     * @param directory the directory to add if not already present in the source, or null
     *
     * @since 4.0.0
     */
    public void addSourceRoot(ProjectScope scope, Language language, String directory) {
        if (directory != null) {
            directory = directory.trim();
            if (!directory.isBlank()) {
                Path path = getBaseDirectory().resolve(directory).normalize();
                var key = new SourceKey(scope, language, path);
                sources.computeIfAbsent(key, SourceKey::createSource);
            }
        }
    }

    /**
     * @deprecated Replaced by {@code addSourceRoot(ProjectScope.MAIN, Language.JAVA_FAMILY, path)}.
     */
    @Deprecated(since = "4.0.0")
    public void addCompileSourceRoot(String path) {
        addSourceRoot(ProjectScope.MAIN, Language.JAVA_FAMILY, path);
    }

    /**
     * @deprecated Replaced by {@code addSourceRoot(ProjectScope.TEST, Language.JAVA_FAMILY, path)}.
     */
    @Deprecated(since = "4.0.0")
    public void addTestCompileSourceRoot(String path) {
        addSourceRoot(ProjectScope.TEST, Language.JAVA_FAMILY, path);
    }

    /**
     * {@return all source root directories, including the disabled ones, for all languages and scopes}.
     * The iteration order is the order in which the sources are declared in the POM file.
     * The returned collection is unmodifiable.
     *
     * @see #addSourceRoot(SourceRoot)
     */
    public Collection<SourceRoot> getSourceRoots() {
        return Collections.unmodifiableCollection(sources.values());
    }

    /**
     * {@return all enabled sources that provide files in the given language for the given scope}.
     * If the given scope is {@code null}, then this method returns the enabled sources for all scopes.
     * If the given language is {@code null}, then this method returns the enabled sources for all languages.
     * The iteration order is the order in which the sources are declared in the POM file.
     *
     * @param scope the scope of the sources to return, or {@code null} for all scopes
     * @param language the language of the sources to return, or {@code null} for all languages
     *
     * @see #addSourceRoot(ProjectScope, Language, Path)
     *
     * @since 4.0.0
     */
    public Stream<SourceRoot> getEnabledSourceRoots(ProjectScope scope, Language language) {
        Stream<SourceRoot> s = sources.values().stream().filter(SourceRoot::enabled);
        if (scope != null) {
            s = s.filter((source) -> scope.equals(source.scope()));
        }
        if (language != null) {
            s = s.filter((source) -> language.equals(source.language()));
        }
        return s;
    }

    /**
     * Returns a list of paths for the given scope.
     *
     * @deprecated Used only for the implementation of deprecated methods.
     */
    @Deprecated
    private List<String> getSourceRootDirs(ProjectScope scope, Language language) {
        return getEnabledSourceRoots(scope, language)
                .map((source) -> source.directory().toString())
                .toList();
    }

    /**
     * @deprecated Replaced by {@code getEnabledSourceRoots(ProjectScope.MAIN, Language.JAVA_FAMILY)}.
     */
    @Deprecated(since = "4.0.0")
    public List<String> getCompileSourceRoots() {
        return getSourceRootDirs(ProjectScope.MAIN, Language.JAVA_FAMILY);
    }

    /**
     * @deprecated Replaced by {@code getEnabledSourceRoots(ProjectScope.TEST, Language.JAVA_FAMILY)}.
     */
    @Deprecated(since = "4.0.0")
    public List<String> getTestCompileSourceRoots() {
        return getSourceRootDirs(ProjectScope.TEST, Language.JAVA_FAMILY);
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
     * Returns a filtered list of class path elements. This method is invoked when the caller
     * requested that all dependencies are placed on the class path, with no module path element.
     *
     * @param scopeFilter a filter returning {@code true} for the artifact scopes to accept
     * @param includeTestDir whether to include the test directory in the classpath elements
     * @return paths of all artifacts placed on the classpath
     * @throws DependencyResolutionRequiredException if an artifact file is used, but has not been resolved
     */
    private List<String> getClasspathElements(final Predicate<String> scopeFilter, final boolean includeTestDir)
            throws DependencyResolutionRequiredException {
        final List<String> list = new ArrayList<>(getArtifacts().size() + 2);
        if (includeTestDir) {
            String d = getBuild().getTestOutputDirectory();
            if (d != null) {
                list.add(d);
            }
        }
        String d = getBuild().getOutputDirectory();
        if (d != null) {
            list.add(d);
        }
        for (Artifact a : getArtifacts()) {
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
     * Returns the elements placed on the classpath for compilation.
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
     * Returns the elements placed on the classpath for tests.
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
     * Returns the elements placed on the classpath for runtime.
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
        String groupId = getModel().getGroupId();

        if ((groupId == null) && (getModel().getParent() != null)) {
            groupId = getModel().getParent().getGroupId();
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
        if (getModel().getName() != null) {
            return getModel().getName();
        } else {
            return getArtifactId();
        }
    }

    public void setVersion(String version) {
        getModel().setVersion(version);
    }

    public String getVersion() {
        String version = getModel().getVersion();

        if ((version == null) && (getModel().getParent() != null)) {
            version = getModel().getParent().getVersion();
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

    /**
     * @deprecated Replaced by {@code getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)}.
     */
    @Deprecated(since = "4.0.0")
    public List<Resource> getResources() {
        return new AbstractList<>() {
            @Override
            public Resource get(int index) {
                return toResource(getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)
                        .toList()
                        .get(index));
            }

            @Override
            public int size() {
                return (int) getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES)
                        .count();
            }

            @Override
            public boolean add(Resource resource) {
                addResource(resource);
                return true;
            }
        };
    }

    /**
     * @deprecated Replaced by {@code getEnabledSourceRoots(ProjectScope.TEST, Language.RESOURCES)}.
     */
    @Deprecated(since = "4.0.0")
    public List<Resource> getTestResources() {
        return new AbstractList<>() {
            @Override
            public Resource get(int index) {
                return toResource(getEnabledSourceRoots(ProjectScope.TEST, Language.RESOURCES)
                        .toList()
                        .get(index));
            }

            @Override
            public int size() {
                return (int) getEnabledSourceRoots(ProjectScope.TEST, Language.RESOURCES)
                        .count();
            }

            @Override
            public boolean add(Resource resource) {
                addTestResource(resource);
                return true;
            }
        };
    }

    private Resource toResource(SourceRoot sourceRoot) {
        return new Resource(org.apache.maven.api.model.Resource.newBuilder()
                .directory(sourceRoot.directory().toString())
                .includes(sourceRoot.includes().stream().map(Object::toString).toList())
                .excludes(sourceRoot.excludes().stream().map(Object::toString).toList())
                .filtering(Boolean.toString(sourceRoot.stringFiltering()))
                .build());
    }

    /**
     * @deprecated {@link Resource} is replaced by {@link SourceRoot}.
     */
    @Deprecated(since = "4.0.0")
    public void addResource(Resource resource) {
        addSourceRoot(new DefaultSourceRoot(getBaseDirectory(), ProjectScope.MAIN, resource.getDelegate()));
    }

    /**
     * @deprecated {@link Resource} is replaced by {@link SourceRoot}.
     */
    @Deprecated(since = "4.0.0")
    public void addTestResource(Resource testResource) {
        addSourceRoot(new DefaultSourceRoot(getBaseDirectory(), ProjectScope.TEST, testResource.getDelegate()));
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

    public void setArtifacts(Set<Artifact> artifacts) {
        this.artifacts = artifacts;

        // flush the calculated artifactMap
        artifactMap = null;
    }

    /**
     * All dependencies that this project has, including transitive ones. Contents are lazily populated, so depending on
     * what phases have run dependencies in some scopes won't be included. e.g. if only compile phase has run,
     * dependencies with scope test won't be included.
     *
     * @return {@link Set} &lt; {@link Artifact} &gt;
     * @see #getDependencyArtifacts() to get only direct dependencies
     */
    public Set<Artifact> getArtifacts() {
        if (artifacts == null) {
            if (artifactFilter == null || resolvedArtifacts == null) {
                artifacts = new LinkedHashSet<>();
            } else {
                artifacts = new LinkedHashSet<>(resolvedArtifacts.size() * 2);
                for (Artifact artifact : resolvedArtifacts) {
                    if (artifactFilter.include(artifact)) {
                        artifacts.add(artifact);
                    }
                }
            }
        }
        return artifacts;
    }

    public Map<String, Artifact> getArtifactMap() {
        if (artifactMap == null) {
            artifactMap = ArtifactUtils.artifactMapByVersionlessId(getArtifacts());
        }
        return artifactMap;
    }

    public void setPluginArtifacts(Set<Artifact> pluginArtifacts) {
        this.pluginArtifacts = pluginArtifacts;

        this.pluginArtifactMap = null;
    }

    public Set<Artifact> getPluginArtifacts() {
        return pluginArtifacts;
    }

    public Map<String, Artifact> getPluginArtifactMap() {
        if (pluginArtifactMap == null) {
            pluginArtifactMap = ArtifactUtils.artifactMapByVersionlessId(getPluginArtifacts());
        }

        return pluginArtifactMap;
    }

    public void setParentArtifact(Artifact parentArtifact) {
        this.parentArtifact = parentArtifact;
    }

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
        if (getModel().getBuild() == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(getModel().getBuild().getPlugins());
    }

    public List<String> getModules() {
        if (!getModel().getDelegate().getSubprojects().isEmpty()) {
            return getModel().getDelegate().getSubprojects();
        }
        return getModel().getModules();
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
        Build build = getModel().getBuild();

        if (build == null) {
            build = new Build();

            getModel().setBuild(build);
        }

        return build;
    }

    @Deprecated
    public void setRemoteArtifactRepositories(List<ArtifactRepository> remoteArtifactRepositories) {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
        this.remoteProjectRepositories = RepositoryUtils.toRepos(getRemoteArtifactRepositories());
    }

    @Deprecated
    public List<ArtifactRepository> getRemoteArtifactRepositories() {
        if (remoteArtifactRepositories == null) {
            remoteArtifactRepositories = new ArrayList<>();
        }

        return remoteArtifactRepositories;
    }

    @Deprecated
    public void setPluginArtifactRepositories(List<ArtifactRepository> pluginArtifactRepositories) {
        this.pluginArtifactRepositories = pluginArtifactRepositories;
        this.remotePluginRepositories = RepositoryUtils.toRepos(getPluginArtifactRepositories());
    }

    /**
     * @return a list of ArtifactRepository objects constructed from the Repository objects returned by
     *         getPluginRepositories.
     */
    @Deprecated
    public List<ArtifactRepository> getPluginArtifactRepositories() {
        if (pluginArtifactRepositories == null) {
            pluginArtifactRepositories = new ArrayList<>();
        }

        return pluginArtifactRepositories;
    }

    @Deprecated
    public ArtifactRepository getDistributionManagementArtifactRepository() {
        return getArtifact().isSnapshot() && (getSnapshotArtifactRepository() != null)
                ? getSnapshotArtifactRepository()
                : getReleaseArtifactRepository();
    }

    public List<Repository> getPluginRepositories() {
        return getModel().getPluginRepositories();
    }

    public List<RemoteRepository> getRemoteProjectRepositories() {
        return remoteProjectRepositories;
    }

    public List<RemoteRepository> getRemotePluginRepositories() {
        return remotePluginRepositories;
    }

    public void setActiveProfiles(List<Profile> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    public List<Profile> getActiveProfiles() {
        return activeProfiles;
    }

    public void setInjectedProfileIds(String source, List<String> injectedProfileIds) {
        if (injectedProfileIds != null) {
            this.injectedProfileIds.put(source, new ArrayList<>(injectedProfileIds));
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
     * @return The identifiers of all injected profiles, indexed by the source from which the profiles originated, never
     *         {@code null}.
     */
    public Map<String, List<String>> getInjectedProfileIds() {
        return this.injectedProfileIds;
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
    public void addAttachedArtifact(Artifact artifact) throws DuplicateArtifactAttachmentException {
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
    public List<Artifact> getAttachedArtifacts() {
        if (attachedArtifacts == null) {
            attachedArtifacts = new ArrayList<>();
        }
        return Collections.unmodifiableList(attachedArtifacts);
    }

    public Xpp3Dom getGoalConfiguration(
            String pluginGroupId, String pluginArtifactId, String executionId, String goalId) {
        Xpp3Dom dom = null;

        if (getBuildPlugins() != null) {
            for (Plugin plugin : getBuildPlugins()) {
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

    public MavenProject getExecutionProject() {
        return (executionProject == null ? this : executionProject);
    }

    public void setExecutionProject(MavenProject executionProject) {
        this.executionProject = executionProject;
    }

    public List<MavenProject> getCollectedProjects() {
        return collectedProjects;
    }

    public void setCollectedProjects(List<MavenProject> collectedProjects) {
        this.collectedProjects = collectedProjects;
    }

    /**
     * Direct dependencies that this project has.
     *
     * @return {@link Set} &lt; {@link Artifact} &gt;
     * @see #getArtifacts() to get all transitive dependencies
     */
    @Deprecated
    public Set<Artifact> getDependencyArtifacts() {
        return dependencyArtifacts;
    }

    @Deprecated
    public void setDependencyArtifacts(Set<Artifact> dependencyArtifacts) {
        this.dependencyArtifacts = dependencyArtifacts;
    }

    @Deprecated
    public void setReleaseArtifactRepository(ArtifactRepository releaseArtifactRepository) {
        this.releaseArtifactRepository = releaseArtifactRepository;
    }

    @Deprecated
    public void setSnapshotArtifactRepository(ArtifactRepository snapshotArtifactRepository) {
        this.snapshotArtifactRepository = snapshotArtifactRepository;
    }

    public void setOriginalModel(Model originalModel) {
        this.originalModel = originalModel;
    }

    public Model getOriginalModel() {
        return originalModel;
    }

    public void setManagedVersionMap(Map<String, Artifact> map) {
        managedVersionMap = map;
    }

    public Map<String, Artifact> getManagedVersionMap() {
        return managedVersionMap;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else {
            if (other instanceof MavenProject that) {
                return Objects.equals(getArtifactId(), that.getArtifactId())
                        && Objects.equals(getGroupId(), that.getGroupId())
                        && Objects.equals(getVersion(), that.getVersion());
            } else {
                return false;
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroupId(), getArtifactId(), getVersion());
    }

    public List<Extension> getBuildExtensions() {
        Build build = getBuild();
        if ((build == null) || (build.getExtensions() == null)) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(build.getExtensions());
        }
    }

    public void addProjectReference(MavenProject project) {
        projectReferences.put(
                getProjectReferenceId(project.getGroupId(), project.getArtifactId(), project.getVersion()), project);
    }

    public Properties getProperties() {
        return getModel().getProperties();
    }

    public List<String> getFilters() {
        return getBuild().getFilters();
    }

    public Map<String, MavenProject> getProjectReferences() {
        return projectReferences;
    }

    public boolean isExecutionRoot() {
        return executionRoot;
    }

    public void setExecutionRoot(boolean executionRoot) {
        this.executionRoot = executionRoot;
    }

    public String getDefaultGoal() {
        return getBuild() != null ? getBuild().getDefaultGoal() : null;
    }

    public Plugin getPlugin(String pluginKey) {
        return getBuild().getPluginsAsMap().get(pluginKey);
    }

    /**
     * Default toString
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("MavenProject: ");
        sb.append(getGroupId());
        sb.append(':');
        sb.append(getArtifactId());
        sb.append(':');
        sb.append(getVersion());
        if (getFile() != null) {
            sb.append(" @ ");
            sb.append(getFile().getPath());
        }

        return sb.toString();
    }

    /**
     * @since 2.0.9
     */
    @Override
    public MavenProject clone() {
        MavenProject clone;
        try {
            clone = (MavenProject) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }

        clone.deepCopy(this);

        return clone;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    protected void setAttachedArtifacts(List<Artifact> attachedArtifacts) {
        this.attachedArtifacts = attachedArtifacts;
    }

    /**
     * @deprecated Used only for the implementation of deprecated methods.
     */
    @Deprecated
    private void setSourceRootDirs(ProjectScope scope, Language language, List<String> roots) {
        sources.values().removeIf((source) -> scope.equals(source.scope()) && language.equals(source.language()));
        Path directory = getBaseDirectory();
        for (String root : roots) {
            addSourceRoot(new DefaultSourceRoot(scope, language, directory.resolve(root)));
        }
    }

    /**
     * @deprecated Replaced by {@link #addSourceRoot(ProjectScope, Language, String)}.
     */
    @Deprecated(since = "4.0.0")
    protected void setCompileSourceRoots(List<String> compileSourceRoots) {
        setSourceRootDirs(ProjectScope.MAIN, Language.JAVA_FAMILY, compileSourceRoots);
    }

    /**
     * @deprecated Replaced by {@link #addSourceRoot(ProjectScope, Language, String)}.
     */
    @Deprecated(since = "4.0.0")
    protected void setTestCompileSourceRoots(List<String> testCompileSourceRoots) {
        setSourceRootDirs(ProjectScope.TEST, Language.JAVA_FAMILY, testCompileSourceRoots);
    }

    protected ArtifactRepository getReleaseArtifactRepository() {
        return releaseArtifactRepository;
    }

    protected ArtifactRepository getSnapshotArtifactRepository() {
        return snapshotArtifactRepository;
    }

    private void deepCopy(MavenProject project) {
        // disown the parent

        // copy fields
        file = project.file;
        basedir = project.basedir;

        // don't need a deep copy, they don't get modified or added/removed to/from - but make them unmodifiable to be
        // sure!
        if (project.getDependencyArtifacts() != null) {
            setDependencyArtifacts(Collections.unmodifiableSet(project.getDependencyArtifacts()));
        }

        if (project.getArtifacts() != null) {
            setArtifacts(Collections.unmodifiableSet(project.getArtifacts()));
        }

        if (project.getParentFile() != null) {
            parentFile = new File(project.getParentFile().getAbsolutePath());
        }

        if (project.getPluginArtifacts() != null) {
            setPluginArtifacts(Collections.unmodifiableSet(project.getPluginArtifacts()));
        }

        if (project.getReportArtifacts() != null) {
            setReportArtifacts(Collections.unmodifiableSet(project.getReportArtifacts()));
        }

        if (project.getExtensionArtifacts() != null) {
            setExtensionArtifacts(Collections.unmodifiableSet(project.getExtensionArtifacts()));
        }

        setParentArtifact((project.getParentArtifact()));

        if (project.getRemoteArtifactRepositories() != null) {
            setRemoteArtifactRepositories(Collections.unmodifiableList(project.getRemoteArtifactRepositories()));
        }

        if (project.getPluginArtifactRepositories() != null) {
            setPluginArtifactRepositories(Collections.unmodifiableList(project.getPluginArtifactRepositories()));
        }

        if (project.getActiveProfiles() != null) {
            setActiveProfiles((Collections.unmodifiableList(project.getActiveProfiles())));
        }

        if (project.getAttachedArtifacts() != null) {
            // clone properties modifiable by plugins in a forked lifecycle
            setAttachedArtifacts(new ArrayList<>(project.getAttachedArtifacts()));
        }

        // This property is not handled like others as we don't use public API.
        // The whole implementation of this `deepCopy` method may need revision,
        // but it would be the topic for a separated commit.
        sources = (HashMap<SourceKey, SourceRoot>) project.sources.clone();

        if (project.getModel() != null) {
            setModel(project.getModel().clone());
        }

        if (project.getOriginalModel() != null) {
            setOriginalModel(project.getOriginalModel());
        }

        setExecutionRoot(project.isExecutionRoot());

        if (project.getArtifact() != null) {
            setArtifact(ArtifactUtils.copyArtifact(project.getArtifact()));
        }

        if (project.getManagedVersionMap() != null) {
            setManagedVersionMap(project.getManagedVersionMap());
        }

        lifecyclePhases.addAll(project.lifecyclePhases);
    }

    private static String getProjectReferenceId(String groupId, String artifactId, String version) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(groupId).append(':').append(artifactId).append(':').append(version);
        return buffer.toString();
    }

    /**
     * Sets the value of the context value of this project identified by the given key. If the supplied value is
     * <code>null</code>, the context value is removed from this project. Context values are intended to allow core
     * extensions to associate derived state with project instances.
     */
    public void setContextValue(String key, Object value) {
        if (context == null) {
            context = new HashMap<>();
        }
        if (value != null) {
            context.put(key, value);
        } else {
            context.remove(key);
        }
    }

    /**
     * Returns context value of this project associated with the given key or null if this project has no such value.
     */
    public Object getContextValue(String key) {
        if (context == null) {
            return null;
        }
        return context.get(key);
    }

    /**
     * Sets the project's class realm. <strong>Warning:</strong> This is an internal utility method that is only public
     * for technical reasons, it is not part of the public API. In particular, this method can be changed or deleted
     * without prior notice and must not be used by plugins.
     *
     * @param classRealm The class realm hosting the build extensions of this project, may be {@code null}.
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
     * @param artifacts The set of artifacts, may be {@code null}.
     */
    public void setResolvedArtifacts(Set<Artifact> artifacts) {
        this.resolvedArtifacts = (artifacts != null) ? artifacts : Collections.<Artifact>emptySet();
        this.artifacts = null;
        this.artifactMap = null;
    }

    /**
     * Sets the scope filter to select the artifacts being exposed to the currently executed mojo.
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param artifactFilter The artifact filter, may be {@code null} to exclude all artifacts.
     */
    public void setArtifactFilter(ArtifactFilter artifactFilter) {
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

    @Deprecated
    protected void setScriptSourceRoots(List<String> scriptSourceRoots) {
        setSourceRootDirs(ProjectScope.MAIN, Language.SCRIPT, scriptSourceRoots);
    }

    @Deprecated
    public void addScriptSourceRoot(String path) {
        addSourceRoot(ProjectScope.MAIN, Language.SCRIPT, path);
    }

    @Deprecated
    public List<String> getScriptSourceRoots() {
        return getSourceRootDirs(ProjectScope.MAIN, Language.SCRIPT);
    }

    @Deprecated
    public List<Artifact> getCompileArtifacts() {
        List<Artifact> list = new ArrayList<>(getArtifacts().size());

        for (Artifact a : getArtifacts()) {
            // TODO classpath check doesn't belong here - that's the other method
            if (a.getArtifactHandler().isAddedToClasspath()) {
                // TODO let the scope handler deal with this
                if (isCompilePathElement(a.getScope())) {
                    list.add(a);
                }
            }
        }
        return list;
    }

    @Deprecated
    public List<Dependency> getCompileDependencies() {
        Set<Artifact> artifacts = getArtifacts();

        if ((artifacts == null) || artifacts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>(artifacts.size());

        for (Artifact a : getArtifacts()) {
            // TODO let the scope handler deal with this
            if (isCompilePathElement(a.getScope())) {
                Dependency dependency = new Dependency();

                dependency.setArtifactId(a.getArtifactId());
                dependency.setGroupId(a.getGroupId());
                dependency.setVersion(a.getVersion());
                dependency.setScope(a.getScope());
                dependency.setType(a.getType());
                dependency.setClassifier(a.getClassifier());

                list.add(dependency);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Deprecated
    public List<Artifact> getTestArtifacts() {
        List<Artifact> list = new ArrayList<>(getArtifacts().size());

        for (Artifact a : getArtifacts()) {
            // TODO classpath check doesn't belong here - that's the other method
            if (a.getArtifactHandler().isAddedToClasspath()) {
                list.add(a);
            }
        }
        return list;
    }

    @Deprecated
    public List<Dependency> getTestDependencies() {
        Set<Artifact> artifacts = getArtifacts();

        if ((artifacts == null) || artifacts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>(artifacts.size());

        for (Artifact a : getArtifacts()) {
            Dependency dependency = new Dependency();

            dependency.setArtifactId(a.getArtifactId());
            dependency.setGroupId(a.getGroupId());
            dependency.setVersion(a.getVersion());
            dependency.setScope(a.getScope());
            dependency.setType(a.getType());
            dependency.setClassifier(a.getClassifier());

            list.add(dependency);
        }
        return Collections.unmodifiableList(list);
    }

    @Deprecated // used by the Maven ITs
    public List<Dependency> getRuntimeDependencies() {
        Set<Artifact> artifacts = getArtifacts();

        if ((artifacts == null) || artifacts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>(artifacts.size());

        for (Artifact a : getArtifacts()) {
            // TODO let the scope handler deal with this
            if (isRuntimePathElement(a.getScope())) {
                Dependency dependency = new Dependency();

                dependency.setArtifactId(a.getArtifactId());
                dependency.setGroupId(a.getGroupId());
                dependency.setVersion(a.getVersion());
                dependency.setScope(a.getScope());
                dependency.setType(a.getType());
                dependency.setClassifier(a.getClassifier());

                list.add(dependency);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Deprecated
    public List<Artifact> getRuntimeArtifacts() {
        List<Artifact> list = new ArrayList<>(getArtifacts().size());

        for (Artifact a : getArtifacts()) {
            // TODO classpath check doesn't belong here - that's the other method
            if (a.getArtifactHandler().isAddedToClasspath() && isRuntimePathElement(a.getScope())) {
                list.add(a);
            }
        }
        return list;
    }

    @Deprecated
    public List<String> getSystemClasspathElements() throws DependencyResolutionRequiredException {
        List<String> list = new ArrayList<>(getArtifacts().size());

        String d = getBuild().getOutputDirectory();
        if (d != null) {
            list.add(d);
        }

        for (Artifact a : getArtifacts()) {
            if (a.getArtifactHandler().isAddedToClasspath()) {
                // TODO let the scope handler deal with this
                if (Artifact.SCOPE_SYSTEM.equals(a.getScope())) {
                    File f = a.getFile();
                    if (f != null) {
                        list.add(f.getPath());
                    }
                }
            }
        }
        return list;
    }

    @Deprecated
    public List<Artifact> getSystemArtifacts() {
        List<Artifact> list = new ArrayList<>(getArtifacts().size());

        for (Artifact a : getArtifacts()) {
            // TODO classpath check doesn't belong here - that's the other method
            if (a.getArtifactHandler().isAddedToClasspath()) {
                // TODO let the scope handler deal with this
                if (Artifact.SCOPE_SYSTEM.equals(a.getScope())) {
                    list.add(a);
                }
            }
        }
        return list;
    }

    @Deprecated
    public List<Dependency> getSystemDependencies() {
        Set<Artifact> artifacts = getArtifacts();

        if ((artifacts == null) || artifacts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>(artifacts.size());

        for (Artifact a : getArtifacts()) {
            // TODO let the scope handler deal with this
            if (Artifact.SCOPE_SYSTEM.equals(a.getScope())) {
                Dependency dependency = new Dependency();

                dependency.setArtifactId(a.getArtifactId());
                dependency.setGroupId(a.getGroupId());
                dependency.setVersion(a.getVersion());
                dependency.setScope(a.getScope());
                dependency.setType(a.getType());
                dependency.setClassifier(a.getClassifier());

                list.add(dependency);
            }
        }
        return Collections.unmodifiableList(list);
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
    public void setReportArtifacts(Set<Artifact> reportArtifacts) {
        this.reportArtifacts = reportArtifacts;

        reportArtifactMap = null;
    }

    @Deprecated
    public Set<Artifact> getReportArtifacts() {
        return reportArtifacts;
    }

    @Deprecated
    public Map<String, Artifact> getReportArtifactMap() {
        if (reportArtifactMap == null) {
            reportArtifactMap = ArtifactUtils.artifactMapByVersionlessId(getReportArtifacts());
        }

        return reportArtifactMap;
    }

    @Deprecated
    public void setExtensionArtifacts(Set<Artifact> extensionArtifacts) {
        this.extensionArtifacts = extensionArtifacts;
        extensionArtifactMap = null;
    }

    @Deprecated
    public Set<Artifact> getExtensionArtifacts() {
        return extensionArtifacts;
    }

    @Deprecated
    public Map<String, Artifact> getExtensionArtifactMap() {
        if (extensionArtifactMap == null) {
            extensionArtifactMap = ArtifactUtils.artifactMapByVersionlessId(getExtensionArtifacts());
        }

        return extensionArtifactMap;
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
    // used by maven-dependency-tree
    @Deprecated
    public void setProjectBuildingRequest(ProjectBuildingRequest projectBuildingRequest) {
        this.projectBuilderConfiguration = projectBuildingRequest;
    }

    /**
     * @since 4.0.0
     * @return the rootDirectory for this project
     * @throws IllegalStateException if the rootDirectory cannot be found
     */
    public Path getRootDirectory() {
        if (rootDirectory == null) {
            throw new IllegalStateException(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
        }
        return rootDirectory;
    }

    public void setRootDirectory(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }
}
