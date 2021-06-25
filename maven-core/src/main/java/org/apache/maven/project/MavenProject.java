package org.apache.maven.project;

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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Objects;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
// remove once createArtifacts() is removed
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
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
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
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
public class MavenProject
    implements Cloneable
{

    private static final Logger LOGGER = LoggerFactory.getLogger( MavenProject.class );

    public static final String EMPTY_PROJECT_GROUP_ID = "unknown";

    public static final String EMPTY_PROJECT_ARTIFACT_ID = "empty-project";

    public static final String EMPTY_PROJECT_VERSION = "0";

    private Model model;

    private MavenProject parent;

    private File file;

    private File basedir;

    private Set<Artifact> resolvedArtifacts;

    private Artifact parentArtifact;

    private Set<Artifact> pluginArtifacts;

    private List<ArtifactRepository> remoteArtifactRepositories;

    private List<ArtifactRepository> pluginArtifactRepositories;

    private List<RemoteRepository> remoteProjectRepositories;

    private List<RemoteRepository> remotePluginRepositories;

    private List<Artifact> attachedArtifacts = new ArrayList<>();

    private MavenProject executionProject;

    private List<MavenProject> collectedProjects;

    private List<String> compileSourceRoots = new ArrayList<>();

    private List<String> testCompileSourceRoots = new ArrayList<>();

    private List<String> scriptSourceRoots = new ArrayList<>();

    private ArtifactRepository releaseArtifactRepository;

    private ArtifactRepository snapshotArtifactRepository;

    private List<Profile> activeProfiles = new ArrayList<>();

    private Map<String, List<String>> injectedProfileIds = new LinkedHashMap<>();

    private Set<Artifact> dependencyArtifacts;

    private Artifact artifact;

    private final ThreadLocal<ArtifactsHolder> threadLocalArtifactsHolder =
        ThreadLocal.withInitial( ArtifactsHolder::new );

    private Model originalModel;

    private Map<String, Artifact> pluginArtifactMap;

    private Set<Artifact> reportArtifacts;

    private Map<String, Artifact> reportArtifactMap;

    private Set<Artifact> extensionArtifacts;

    private Map<String, Artifact> extensionArtifactMap;

    private Map<String, Artifact> managedVersionMap;

    private Map<String, MavenProject> projectReferences = new HashMap<>();

    private boolean executionRoot;

    private File parentFile;

    private Map<String, Object> context;

    private ClassRealm classRealm;

    private DependencyFilter extensionDependencyFilter;

    private final Set<String> lifecyclePhases = Collections.synchronizedSet( new LinkedHashSet<>() );

    public MavenProject()
    {
        Model model = new Model();

        model.setGroupId( EMPTY_PROJECT_GROUP_ID );
        model.setArtifactId( EMPTY_PROJECT_ARTIFACT_ID );
        model.setVersion( EMPTY_PROJECT_VERSION );

        setModel( model );
    }

    public MavenProject( Model model )
    {
        setModel( model );
    }

    public MavenProject( MavenProject project )
    {
        deepCopy( project );
    }

    public File getParentFile()
    {
        return parentFile;
    }

    public void setParentFile( File parentFile )
    {
        this.parentFile = parentFile;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    // TODO I would like to get rid of this. jvz.
    public Model getModel()
    {
        return model;
    }

    /**
     * Returns the project corresponding to a declared parent.
     *
     * @return the parent, or null if no parent is declared or there was an error building it
     */
    public MavenProject getParent()
    {
        return parent;
    }

    public void setParent( MavenProject parent )
    {
        this.parent = parent;
    }

    public boolean hasParent()
    {
        return getParent() != null;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
        this.basedir = file != null ? file.getParentFile() : null;
    }

    /**
     * Sets project {@code file} without changing project {@code basedir}.
     *
     * @since 3.2.4
     */
    public void setPomFile( File file )
    {
        this.file = file;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setDependencies( List<Dependency> dependencies )
    {
        getModel().setDependencies( dependencies );
    }

    public List<Dependency> getDependencies()
    {
        return getModel().getDependencies();
    }

    public DependencyManagement getDependencyManagement()
    {
        return getModel().getDependencyManagement();
    }

    // ----------------------------------------------------------------------
    // Test and compile sourceroots.
    // ----------------------------------------------------------------------

    private void addPath( List<String> paths, String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() > 0 )
            {
                File file = new File( path );
                if ( file.isAbsolute() )
                {
                    path = file.getAbsolutePath();
                }
                else if ( ".".equals( path ) )
                {
                    path = getBasedir().getAbsolutePath();
                }
                else
                {
                    path = new File( getBasedir(), path ).getAbsolutePath();
                }

                if ( !paths.contains( path ) )
                {
                    paths.add( path );
                }
            }
        }
    }

    public void addCompileSourceRoot( String path )
    {
        addPath( getCompileSourceRoots(), path );
    }

    public void addTestCompileSourceRoot( String path )
    {
        addPath( getTestCompileSourceRoots(), path );
    }

    public List<String> getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    public List<String> getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    public List<String> getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<>( getArtifacts().size() + 1 );

        String d = getBuild().getOutputDirectory();
        if ( d != null )
        {
            list.add( d );
        }

        for ( Artifact a : getArtifacts() )
        {
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    addArtifactPath( a, list );
                }
            }
        }

        return list;
    }

    // TODO this checking for file == null happens because the resolver has been confused about the root
    // artifact or not. things like the stupid dummy artifact coming from surefire.
    public List<String> getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<>( getArtifacts().size() + 2 );

        String d = getBuild().getTestOutputDirectory();
        if ( d != null )
        {
            list.add( d );
        }

        d = getBuild().getOutputDirectory();
        if ( d != null )
        {
            list.add( d );
        }

        for ( Artifact a : getArtifacts() )
        {
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                addArtifactPath( a, list );
            }
        }

        return list;
    }

    public List<String> getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<>( getArtifacts().size() + 1 );

        String d = getBuild().getOutputDirectory();
        if ( d != null )
        {
            list.add( d );
        }

        for ( Artifact a : getArtifacts() )
        {
            if ( a.getArtifactHandler().isAddedToClasspath()
            // TODO let the scope handler deal with this
                && ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) ) )
            {
                addArtifactPath( a, list );
            }
        }
        return list;
    }

    // ----------------------------------------------------------------------
    // Delegate to the model
    // ----------------------------------------------------------------------

    public void setModelVersion( String pomVersion )
    {
        getModel().setModelVersion( pomVersion );
    }

    public String getModelVersion()
    {
        return getModel().getModelVersion();
    }

    public String getId()
    {
        return getModel().getId();
    }

    public void setGroupId( String groupId )
    {
        getModel().setGroupId( groupId );
    }

    public String getGroupId()
    {
        String groupId = getModel().getGroupId();

        if ( ( groupId == null ) && ( getModel().getParent() != null ) )
        {
            groupId = getModel().getParent().getGroupId();
        }

        return groupId;
    }

    public void setArtifactId( String artifactId )
    {
        getModel().setArtifactId( artifactId );
    }

    public String getArtifactId()
    {
        return getModel().getArtifactId();
    }

    public void setName( String name )
    {
        getModel().setName( name );
    }

    public String getName()
    {
        // TODO this should not be allowed to be null.
        if ( getModel().getName() != null )
        {
            return getModel().getName();
        }
        else
        {
            return getArtifactId();
        }
    }

    public void setVersion( String version )
    {
        getModel().setVersion( version );
    }

    public String getVersion()
    {
        String version = getModel().getVersion();

        if ( ( version == null ) && ( getModel().getParent() != null ) )
        {
            version = getModel().getParent().getVersion();
        }

        return version;
    }

    public String getPackaging()
    {
        return getModel().getPackaging();
    }

    public void setPackaging( String packaging )
    {
        getModel().setPackaging( packaging );
    }

    public void setInceptionYear( String inceptionYear )
    {
        getModel().setInceptionYear( inceptionYear );
    }

    public String getInceptionYear()
    {
        return getModel().getInceptionYear();
    }

    public void setUrl( String url )
    {
        getModel().setUrl( url );
    }

    public String getUrl()
    {
        return getModel().getUrl();
    }

    public Prerequisites getPrerequisites()
    {
        return getModel().getPrerequisites();
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {
        getModel().setIssueManagement( issueManagement );
    }

    public CiManagement getCiManagement()
    {
        return getModel().getCiManagement();
    }

    public void setCiManagement( CiManagement ciManagement )
    {
        getModel().setCiManagement( ciManagement );
    }

    public IssueManagement getIssueManagement()
    {
        return getModel().getIssueManagement();
    }

    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        getModel().setDistributionManagement( distributionManagement );
    }

    public DistributionManagement getDistributionManagement()
    {
        return getModel().getDistributionManagement();
    }

    public void setDescription( String description )
    {
        getModel().setDescription( description );
    }

    public String getDescription()
    {
        return getModel().getDescription();
    }

    public void setOrganization( Organization organization )
    {
        getModel().setOrganization( organization );
    }

    public Organization getOrganization()
    {
        return getModel().getOrganization();
    }

    public void setScm( Scm scm )
    {
        getModel().setScm( scm );
    }

    public Scm getScm()
    {
        return getModel().getScm();
    }

    public void setMailingLists( List<MailingList> mailingLists )
    {
        getModel().setMailingLists( mailingLists );
    }

    public List<MailingList> getMailingLists()
    {
        return getModel().getMailingLists();
    }

    public void addMailingList( MailingList mailingList )
    {
        getModel().addMailingList( mailingList );
    }

    public void setDevelopers( List<Developer> developers )
    {
        getModel().setDevelopers( developers );
    }

    public List<Developer> getDevelopers()
    {
        return getModel().getDevelopers();
    }

    public void addDeveloper( Developer developer )
    {
        getModel().addDeveloper( developer );
    }

    public void setContributors( List<Contributor> contributors )
    {
        getModel().setContributors( contributors );
    }

    public List<Contributor> getContributors()
    {
        return getModel().getContributors();
    }

    public void addContributor( Contributor contributor )
    {
        getModel().addContributor( contributor );
    }

    public void setBuild( Build build )
    {
        getModel().setBuild( build );
    }

    public Build getBuild()
    {
        return getModelBuild();
    }

    public List<Resource> getResources()
    {
        return getBuild().getResources();
    }

    public List<Resource> getTestResources()
    {
        return getBuild().getTestResources();
    }

    public void addResource( Resource resource )
    {
        getBuild().addResource( resource );
    }

    public void addTestResource( Resource testResource )
    {
        getBuild().addTestResource( testResource );
    }

    public void setLicenses( List<License> licenses )
    {
        getModel().setLicenses( licenses );
    }

    public List<License> getLicenses()
    {
        return getModel().getLicenses();
    }

    public void addLicense( License license )
    {
        getModel().addLicense( license );
    }

    public void setArtifacts( Set<Artifact> artifacts )
    {
        ArtifactsHolder artifactsHolder = threadLocalArtifactsHolder.get();
        artifactsHolder.artifacts = artifacts;

        // flush the calculated artifactMap
        artifactsHolder.artifactMap = null;
    }

    /**
     * All dependencies that this project has, including transitive ones. Contents are lazily populated, so depending on
     * what phases have run dependencies in some scopes won't be included. eg. if only compile phase has run,
     * dependencies with scope test won't be included.
     *
     * @return {@link Set} &lt; {@link Artifact} &gt;
     * @see #getDependencyArtifacts() to get only direct dependencies
     */
    public Set<Artifact> getArtifacts()
    {
        ArtifactsHolder artifactsHolder = threadLocalArtifactsHolder.get();
        if ( artifactsHolder.artifacts == null )
        {
            if ( artifactsHolder.artifactFilter == null || resolvedArtifacts == null )
            {
                artifactsHolder.artifacts = new LinkedHashSet<>();
            }
            else
            {
                artifactsHolder.artifacts = new LinkedHashSet<>( resolvedArtifacts.size() * 2 );
                for ( Artifact artifact : resolvedArtifacts )
                {
                    if ( artifactsHolder.artifactFilter.include( artifact ) )
                    {
                        artifactsHolder.artifacts.add( artifact );
                    }
                }
            }
        }
        return artifactsHolder.artifacts;
    }

    public Map<String, Artifact> getArtifactMap()
    {
        ArtifactsHolder artifactsHolder = threadLocalArtifactsHolder.get();
        if ( artifactsHolder.artifactMap == null )
        {
            artifactsHolder.artifactMap = ArtifactUtils.artifactMapByVersionlessId( getArtifacts() );
        }
        return artifactsHolder.artifactMap;
    }

    public void setPluginArtifacts( Set<Artifact> pluginArtifacts )
    {
        this.pluginArtifacts = pluginArtifacts;

        this.pluginArtifactMap = null;
    }

    public Set<Artifact> getPluginArtifacts()
    {
        return pluginArtifacts;
    }

    public Map<String, Artifact> getPluginArtifactMap()
    {
        if ( pluginArtifactMap == null )
        {
            pluginArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getPluginArtifacts() );
        }

        return pluginArtifactMap;
    }

    public void setParentArtifact( Artifact parentArtifact )
    {
        this.parentArtifact = parentArtifact;
    }

    public Artifact getParentArtifact()
    {
        return parentArtifact;
    }

    public List<Repository> getRepositories()
    {
        return getModel().getRepositories();
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List<Plugin> getBuildPlugins()
    {
        if ( getModel().getBuild() == null )
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList( getModel().getBuild().getPlugins() );
    }

    public List<String> getModules()
    {
        return getModel().getModules();
    }

    public PluginManagement getPluginManagement()
    {
        PluginManagement pluginMgmt = null;

        Build build = getModel().getBuild();
        if ( build != null )
        {
            pluginMgmt = build.getPluginManagement();
        }

        return pluginMgmt;
    }

    private Build getModelBuild()
    {
        Build build = getModel().getBuild();

        if ( build == null )
        {
            build = new Build();

            getModel().setBuild( build );
        }

        return build;
    }

    public void setRemoteArtifactRepositories( List<ArtifactRepository> remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
        this.remoteProjectRepositories = RepositoryUtils.toRepos( getRemoteArtifactRepositories() );
    }

    public List<ArtifactRepository> getRemoteArtifactRepositories()
    {
        if ( remoteArtifactRepositories == null )
        {
            remoteArtifactRepositories = new ArrayList<>();
        }

        return remoteArtifactRepositories;
    }

    public void setPluginArtifactRepositories( List<ArtifactRepository> pluginArtifactRepositories )
    {
        this.pluginArtifactRepositories = pluginArtifactRepositories;
        this.remotePluginRepositories = RepositoryUtils.toRepos( getPluginArtifactRepositories() );
    }

    /**
     * @return a list of ArtifactRepository objects constructed from the Repository objects returned by
     *         getPluginRepositories.
     */
    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        if ( pluginArtifactRepositories == null )
        {
            pluginArtifactRepositories = new ArrayList<>();
        }

        return pluginArtifactRepositories;
    }

    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return getArtifact().isSnapshot() && ( getSnapshotArtifactRepository() != null )
                        ? getSnapshotArtifactRepository()
                        : getReleaseArtifactRepository();
    }

    public List<Repository> getPluginRepositories()
    {
        return getModel().getPluginRepositories();
    }

    public List<RemoteRepository> getRemoteProjectRepositories()
    {
        return remoteProjectRepositories;
    }

    public List<RemoteRepository> getRemotePluginRepositories()
    {
        return remotePluginRepositories;
    }

    public void setActiveProfiles( List<Profile> activeProfiles )
    {
        this.activeProfiles = activeProfiles;
    }

    public List<Profile> getActiveProfiles()
    {
        return activeProfiles;
    }

    public void setInjectedProfileIds( String source, List<String> injectedProfileIds )
    {
        if ( injectedProfileIds != null )
        {
            this.injectedProfileIds.put( source, new ArrayList<>( injectedProfileIds ) );
        }
        else
        {
            this.injectedProfileIds.remove( source );
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
    public Map<String, List<String>> getInjectedProfileIds()
    {
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
    public void addAttachedArtifact( Artifact artifact )
        throws DuplicateArtifactAttachmentException
    {
        // if already there we remove it and add again
        int index = attachedArtifacts.indexOf( artifact );
        if ( index >= 0 )
        {
            LOGGER.warn( "artifact '{}' already attached, replacing previous instance", artifact );
            attachedArtifacts.set( index, artifact );
        }
        else
        {
            attachedArtifacts.add( artifact );
        }
    }

    public List<Artifact> getAttachedArtifacts()
    {
        if ( attachedArtifacts == null )
        {
            attachedArtifacts = new ArrayList<>();
        }
        return Collections.unmodifiableList( attachedArtifacts );
    }

    public Xpp3Dom getGoalConfiguration( String pluginGroupId, String pluginArtifactId, String executionId,
                                         String goalId )
    {
        Xpp3Dom dom = null;

        if ( getBuildPlugins() != null )
        {
            for ( Plugin plugin : getBuildPlugins() )
            {
                if ( pluginGroupId.equals( plugin.getGroupId() ) && pluginArtifactId.equals( plugin.getArtifactId() ) )
                {
                    dom = (Xpp3Dom) plugin.getConfiguration();

                    if ( executionId != null )
                    {
                        PluginExecution execution = plugin.getExecutionsAsMap().get( executionId );
                        if ( execution != null )
                        {
                            // NOTE: The PluginConfigurationExpander already merged the plugin-level config in
                            dom = (Xpp3Dom) execution.getConfiguration();
                        }
                    }
                    break;
                }
            }
        }

        if ( dom != null )
        {
            // make a copy so the original in the POM doesn't get messed with
            dom = new Xpp3Dom( dom );
        }

        return dom;
    }

    public MavenProject getExecutionProject()
    {
        return ( executionProject == null ? this : executionProject );
    }

    public void setExecutionProject( MavenProject executionProject )
    {
        this.executionProject = executionProject;
    }

    public List<MavenProject> getCollectedProjects()
    {
        return collectedProjects;
    }

    public void setCollectedProjects( List<MavenProject> collectedProjects )
    {
        this.collectedProjects = collectedProjects;
    }

    /**
     * Direct dependencies that this project has.
     *
     * @return {@link Set} &lt; {@link Artifact} &gt;
     * @see #getArtifacts() to get all transitive dependencies
     */
    @Deprecated
    public Set<Artifact> getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    @Deprecated
    public void setDependencyArtifacts( Set<Artifact> dependencyArtifacts )
    {
        this.dependencyArtifacts = dependencyArtifacts;
    }

    public void setReleaseArtifactRepository( ArtifactRepository releaseArtifactRepository )
    {
        this.releaseArtifactRepository = releaseArtifactRepository;
    }

    public void setSnapshotArtifactRepository( ArtifactRepository snapshotArtifactRepository )
    {
        this.snapshotArtifactRepository = snapshotArtifactRepository;
    }

    public void setOriginalModel( Model originalModel )
    {
        this.originalModel = originalModel;
    }

    public Model getOriginalModel()
    {
        return originalModel;
    }

    public void setManagedVersionMap( Map<String, Artifact> map )
    {
        managedVersionMap = map;
    }

    public Map<String, Artifact> getManagedVersionMap()
    {
        return managedVersionMap;
    }

    @Override
    public boolean equals( Object other )
    {
        if ( other == this )
        {
            return true;
        }
        else if ( !( other instanceof MavenProject ) )
        {
            return false;
        }

        MavenProject that = (MavenProject) other;

        return Objects.equals( getArtifactId(), that.getArtifactId() )
            && Objects.equals( getGroupId(), that.getGroupId() )
            && Objects.equals( getVersion(), that.getVersion() );
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 31 * hash + getGroupId().hashCode();
        hash = 31 * hash + getArtifactId().hashCode();
        hash = 31 * hash + getVersion().hashCode();
        return hash;
    }

    public List<Extension> getBuildExtensions()
    {
        Build build = getBuild();
        if ( ( build == null ) || ( build.getExtensions() == null ) )
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.unmodifiableList( build.getExtensions() );
        }
    }

    public void addProjectReference( MavenProject project )
    {
        projectReferences.put( getProjectReferenceId( project.getGroupId(), project.getArtifactId(),
                                                      project.getVersion() ), project );
    }

    public Properties getProperties()
    {
        return getModel().getProperties();
    }

    public List<String> getFilters()
    {
        return getBuild().getFilters();
    }

    public Map<String, MavenProject> getProjectReferences()
    {
        return projectReferences;
    }

    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    public void setExecutionRoot( boolean executionRoot )
    {
        this.executionRoot = executionRoot;
    }

    public String getDefaultGoal()
    {
        return getBuild() != null ? getBuild().getDefaultGoal() : null;
    }

    public Plugin getPlugin( String pluginKey )
    {
        return getBuild().getPluginsAsMap().get( pluginKey );
    }

    /**
     * Default toString
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( 128 );
        sb.append( "MavenProject: " );
        sb.append( getGroupId() );
        sb.append( ':' );
        sb.append( getArtifactId() );
        sb.append( ':' );
        sb.append( getVersion() );
        if ( getFile() != null )
        {
            sb.append( " @ " );
            sb.append( getFile().getPath() );
        }

        return sb.toString();
    }

    /**
     * @since 2.0.9
     */
    @Override
    public MavenProject clone()
    {
        MavenProject clone;
        try
        {
            clone = (MavenProject) super.clone();
        }
        catch ( CloneNotSupportedException e )
        {
            throw new UnsupportedOperationException( e );
        }

        clone.deepCopy( this );

        return clone;
    }

    public void setModel( Model model )
    {
        this.model = model;
    }

    protected void setAttachedArtifacts( List<Artifact> attachedArtifacts )
    {
        this.attachedArtifacts = attachedArtifacts;
    }

    protected void setCompileSourceRoots( List<String> compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    protected void setTestCompileSourceRoots( List<String> testCompileSourceRoots )
    {
        this.testCompileSourceRoots = testCompileSourceRoots;
    }

    protected ArtifactRepository getReleaseArtifactRepository()
    {
        return releaseArtifactRepository;
    }

    protected ArtifactRepository getSnapshotArtifactRepository()
    {
        return snapshotArtifactRepository;
    }

    private void deepCopy( MavenProject project )
    {
        // disown the parent

        // copy fields
        file = project.file;
        basedir = project.basedir;

        // don't need a deep copy, they don't get modified or added/removed to/from - but make them unmodifiable to be
        // sure!
        if ( project.getDependencyArtifacts() != null )
        {
            setDependencyArtifacts( Collections.unmodifiableSet( project.getDependencyArtifacts() ) );
        }

        if ( project.getArtifacts() != null )
        {
            setArtifacts( Collections.unmodifiableSet( project.getArtifacts() ) );
        }

        if ( project.getParentFile() != null )
        {
            parentFile = new File( project.getParentFile().getAbsolutePath() );
        }

        if ( project.getPluginArtifacts() != null )
        {
            setPluginArtifacts( Collections.unmodifiableSet( project.getPluginArtifacts() ) );
        }

        if ( project.getReportArtifacts() != null )
        {
            setReportArtifacts( Collections.unmodifiableSet( project.getReportArtifacts() ) );
        }

        if ( project.getExtensionArtifacts() != null )
        {
            setExtensionArtifacts( Collections.unmodifiableSet( project.getExtensionArtifacts() ) );
        }

        setParentArtifact( ( project.getParentArtifact() ) );

        if ( project.getRemoteArtifactRepositories() != null )
        {
            setRemoteArtifactRepositories( Collections.unmodifiableList( project.getRemoteArtifactRepositories() ) );
        }

        if ( project.getPluginArtifactRepositories() != null )
        {
            setPluginArtifactRepositories( Collections.unmodifiableList( project.getPluginArtifactRepositories() ) );
        }

        if ( project.getActiveProfiles() != null )
        {
            setActiveProfiles( ( Collections.unmodifiableList( project.getActiveProfiles() ) ) );
        }

        if ( project.getAttachedArtifacts() != null )
        {
            // clone properties modifyable by plugins in a forked lifecycle
            setAttachedArtifacts( new ArrayList<>( project.getAttachedArtifacts() ) );
        }

        if ( project.getCompileSourceRoots() != null )
        {
            // clone source roots
            setCompileSourceRoots( ( new ArrayList<>( project.getCompileSourceRoots() ) ) );
        }

        if ( project.getTestCompileSourceRoots() != null )
        {
            setTestCompileSourceRoots( ( new ArrayList<>( project.getTestCompileSourceRoots() ) ) );
        }

        if ( project.getScriptSourceRoots() != null )
        {
            setScriptSourceRoots( ( new ArrayList<>( project.getScriptSourceRoots() ) ) );
        }

        if ( project.getModel() != null )
        {
            setModel( project.getModel().clone() );
        }

        if ( project.getOriginalModel() != null )
        {
            setOriginalModel( project.getOriginalModel() );
        }

        setExecutionRoot( project.isExecutionRoot() );

        if ( project.getArtifact() != null )
        {
            setArtifact( ArtifactUtils.copyArtifact( project.getArtifact() ) );
        }

        if ( project.getManagedVersionMap() != null )
        {
            setManagedVersionMap( project.getManagedVersionMap() );
        }

        lifecyclePhases.addAll( project.lifecyclePhases );
    }

    private void addArtifactPath( Artifact artifact, List<String> classpath )
    {
        File file = artifact.getFile();
        if ( file != null )
        {
            classpath.add( file.getPath() );
        }
    }

    private static String getProjectReferenceId( String groupId, String artifactId, String version )
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( groupId ).append( ':' ).append( artifactId ).append( ':' ).append( version );
        return buffer.toString();
    }

    /**
     * Sets the value of the context value of this project identified by the given key. If the supplied value is
     * <code>null</code>, the context value is removed from this project. Context values are intended to allow core
     * extensions to associate derived state with project instances.
     */
    public void setContextValue( String key, Object value )
    {
        if ( context == null )
        {
            context = new HashMap<>();
        }
        if ( value != null )
        {
            context.put( key, value );
        }
        else
        {
            context.remove( key );
        }
    }

    /**
     * Returns context value of this project associated with the given key or null if this project has no such value.
     */
    public Object getContextValue( String key )
    {
        if ( context == null )
        {
            return null;
        }
        return context.get( key );
    }

    /**
     * Sets the project's class realm. <strong>Warning:</strong> This is an internal utility method that is only public
     * for technical reasons, it is not part of the public API. In particular, this method can be changed or deleted
     * without prior notice and must not be used by plugins.
     *
     * @param classRealm The class realm hosting the build extensions of this project, may be {@code null}.
     */
    public void setClassRealm( ClassRealm classRealm )
    {
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
    public ClassRealm getClassRealm()
    {
        return classRealm;
    }

    /**
     * Sets the artifact filter used to exclude shared extension artifacts from plugin realms. <strong>Warning:</strong>
     * This is an internal utility method that is only public for technical reasons, it is not part of the public API.
     * In particular, this method can be changed or deleted without prior notice and must not be used by plugins.
     *
     * @param extensionDependencyFilter The dependency filter to apply to plugins, may be {@code null}.
     */
    public void setExtensionDependencyFilter( DependencyFilter extensionDependencyFilter )
    {
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
    public DependencyFilter getExtensionDependencyFilter()
    {
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
    public void setResolvedArtifacts( Set<Artifact> artifacts )
    {
        this.resolvedArtifacts = ( artifacts != null ) ? artifacts : Collections.<Artifact>emptySet();
        ArtifactsHolder artifactsHolder = threadLocalArtifactsHolder.get();
        artifactsHolder.artifacts = null;
        artifactsHolder.artifactMap = null;
    }

    /**
     * Sets the scope filter to select the artifacts being exposed to the currently executed mojo.
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param artifactFilter The artifact filter, may be {@code null} to exclude all artifacts.
     */
    public void setArtifactFilter( ArtifactFilter artifactFilter )
    {
        ArtifactsHolder artifactsHolder = threadLocalArtifactsHolder.get();
        artifactsHolder.artifactFilter = artifactFilter;
        artifactsHolder.artifacts = null;
        artifactsHolder.artifactMap = null;
    }

    /**
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param phase The phase to check for, must not be {@code null}.
     * @return {@code true} if the phase has been seen.
     */
    public boolean hasLifecyclePhase( String phase )
    {
        return lifecyclePhases.contains( phase );
    }

    /**
     * <strong>Warning:</strong> This is an internal utility method that is only public for technical reasons, it is not
     * part of the public API. In particular, this method can be changed or deleted without prior notice and must not be
     * used by plugins.
     *
     * @param lifecyclePhase The lifecycle phase to add, must not be {@code null}.
     */
    public void addLifecyclePhase( String lifecyclePhase )
    {
        lifecyclePhases.add( lifecyclePhase );
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
    public String getModulePathAdjustment( MavenProject moduleProject )
        throws IOException
    {
        // FIXME: This is hacky. What if module directory doesn't match artifactid, and parent
        // is coming from the repository??
        String module = moduleProject.getArtifactId();

        File moduleFile = moduleProject.getFile();

        if ( moduleFile != null )
        {
            File moduleDir = moduleFile.getCanonicalFile().getParentFile();

            module = moduleDir.getName();
        }

        if ( moduleAdjustments == null )
        {
            moduleAdjustments = new HashMap<>();

            List<String> modules = getModules();
            if ( modules != null )
            {
                for ( String modulePath : modules )
                {
                    String moduleName = modulePath;

                    if ( moduleName.endsWith( "/" ) || moduleName.endsWith( "\\" ) )
                    {
                        moduleName = moduleName.substring( 0, moduleName.length() - 1 );
                    }

                    int lastSlash = moduleName.lastIndexOf( '/' );

                    if ( lastSlash < 0 )
                    {
                        lastSlash = moduleName.lastIndexOf( '\\' );
                    }

                    String adjustment = null;

                    if ( lastSlash > -1 )
                    {
                        moduleName = moduleName.substring( lastSlash + 1 );
                        adjustment = modulePath.substring( 0, lastSlash );
                    }

                    moduleAdjustments.put( moduleName, adjustment );
                }
            }
        }

        return moduleAdjustments.get( module );
    }

    @Deprecated
    public Set<Artifact> createArtifacts( ArtifactFactory artifactFactory, String inheritedScope,
                                          ArtifactFilter filter )
        throws InvalidDependencyVersionException
    {
        return MavenMetadataSource.createArtifacts( artifactFactory, getDependencies(), inheritedScope, filter, this );
    }

    @Deprecated
    protected void setScriptSourceRoots( List<String> scriptSourceRoots )
    {
        this.scriptSourceRoots = scriptSourceRoots;
    }

    @Deprecated
    public void addScriptSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !getScriptSourceRoots().contains( path ) )
                {
                    getScriptSourceRoots().add( path );
                }
            }
        }
    }

    @Deprecated
    public List<String> getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    @Deprecated
    public List<Artifact> getCompileArtifacts()
    {
        List<Artifact> list = new ArrayList<>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    @Deprecated
    public List<Dependency> getCompileDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( ( artifacts == null ) || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO let the scope handler deal with this
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                     || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
            {
                Dependency dependency = new Dependency();

                dependency.setArtifactId( a.getArtifactId() );
                dependency.setGroupId( a.getGroupId() );
                dependency.setVersion( a.getVersion() );
                dependency.setScope( a.getScope() );
                dependency.setType( a.getType() );
                dependency.setClassifier( a.getClassifier() );

                list.add( dependency );
            }
        }
        return Collections.unmodifiableList( list );
    }

    @Deprecated
    public List<Artifact> getTestArtifacts()
    {
        List<Artifact> list = new ArrayList<>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                list.add( a );
            }
        }
        return list;
    }

    @Deprecated
    public List<Dependency> getTestDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( ( artifacts == null ) || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
            Dependency dependency = new Dependency();

            dependency.setArtifactId( a.getArtifactId() );
            dependency.setGroupId( a.getGroupId() );
            dependency.setVersion( a.getVersion() );
            dependency.setScope( a.getScope() );
            dependency.setType( a.getType() );
            dependency.setClassifier( a.getClassifier() );

            list.add( dependency );
        }
        return Collections.unmodifiableList( list );
    }

    @Deprecated // used by the Maven ITs
    public List<Dependency> getRuntimeDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( ( artifacts == null ) || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO let the scope handler deal with this
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
            {
                Dependency dependency = new Dependency();

                dependency.setArtifactId( a.getArtifactId() );
                dependency.setGroupId( a.getGroupId() );
                dependency.setVersion( a.getVersion() );
                dependency.setScope( a.getScope() );
                dependency.setType( a.getType() );
                dependency.setClassifier( a.getClassifier() );

                list.add( dependency );
            }
        }
        return Collections.unmodifiableList( list );
    }

    @Deprecated
    public List<Artifact> getRuntimeArtifacts()
    {
        List<Artifact> list = new ArrayList<>( getArtifacts().size() );

        for ( Artifact a : getArtifacts()  )
        {
            // TODO classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath()
            // TODO let the scope handler deal with this
                && ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) ) )
            {
                list.add( a );
            }
        }
        return list;
    }

    @Deprecated
    public List<String> getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<>( getArtifacts().size() );

        String d = getBuild().getOutputDirectory();
        if ( d != null )
        {
            list.add( d );
        }

        for ( Artifact a : getArtifacts() )
        {
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO let the scope handler deal with this
                if ( Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    addArtifactPath( a, list );
                }
            }
        }
        return list;
    }

    @Deprecated
    public List<Artifact> getSystemArtifacts()
    {
        List<Artifact> list = new ArrayList<>( getArtifacts().size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO let the scope handler deal with this
                if ( Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    @Deprecated
    public List<Dependency> getSystemDependencies()
    {
        Set<Artifact> artifacts = getArtifacts();

        if ( ( artifacts == null ) || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Dependency> list = new ArrayList<>( artifacts.size() );

        for ( Artifact a : getArtifacts() )
        {
            // TODO let the scope handler deal with this
            if ( Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
            {
                Dependency dependency = new Dependency();

                dependency.setArtifactId( a.getArtifactId() );
                dependency.setGroupId( a.getGroupId() );
                dependency.setVersion( a.getVersion() );
                dependency.setScope( a.getScope() );
                dependency.setType( a.getType() );
                dependency.setClassifier( a.getClassifier() );

                list.add( dependency );
            }
        }
        return Collections.unmodifiableList( list );
    }

    @Deprecated
    public void setReporting( Reporting reporting )
    {
        getModel().setReporting( reporting );
    }

    @Deprecated
    public Reporting getReporting()
    {
        return getModel().getReporting();
    }

    @Deprecated
    public void setReportArtifacts( Set<Artifact> reportArtifacts )
    {
        this.reportArtifacts = reportArtifacts;

        reportArtifactMap = null;
    }

    @Deprecated
    public Set<Artifact> getReportArtifacts()
    {
        return reportArtifacts;
    }

    @Deprecated
    public Map<String, Artifact> getReportArtifactMap()
    {
        if ( reportArtifactMap == null )
        {
            reportArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getReportArtifacts() );
        }
        return reportArtifactMap;
    }

    @Deprecated
    public void setExtensionArtifacts( Set<Artifact> extensionArtifacts )
    {
        this.extensionArtifacts = extensionArtifacts;

        extensionArtifactMap = null;
    }

    @Deprecated
    public Set<Artifact> getExtensionArtifacts()
    {
        return extensionArtifacts;
    }

    @Deprecated
    public Map<String, Artifact> getExtensionArtifactMap()
    {
        if ( extensionArtifactMap == null )
        {
            extensionArtifactMap = ArtifactUtils.artifactMapByVersionlessId( getExtensionArtifacts() );
        }
        return extensionArtifactMap;
    }

    @Deprecated
    public List<ReportPlugin> getReportPlugins()
    {
        if ( getModel().getReporting() == null )
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList( getModel().getReporting().getPlugins() );
    }

    @Deprecated
    public Xpp3Dom getReportConfiguration( String pluginGroupId, String pluginArtifactId, String reportSetId )
    {
        Xpp3Dom dom = null;

        // ----------------------------------------------------------------------
        // I would like to be able to lookup the Mojo object using a key but
        // we have a limitation in modello that will be remedied shortly. So
        // for now I have to iterate through and see what we have.
        // ----------------------------------------------------------------------

        if ( getReportPlugins() != null )
        {
            for ( ReportPlugin plugin : getReportPlugins() )
            {
                if ( pluginGroupId.equals( plugin.getGroupId() ) && pluginArtifactId.equals( plugin.getArtifactId() ) )
                {
                    dom = (Xpp3Dom) plugin.getConfiguration();

                    if ( reportSetId != null )
                    {
                        ReportSet reportSet = plugin.getReportSetsAsMap().get( reportSetId );
                        if ( reportSet != null )
                        {
                            Xpp3Dom executionConfiguration = (Xpp3Dom) reportSet.getConfiguration();
                            if ( executionConfiguration != null )
                            {
                                Xpp3Dom newDom = new Xpp3Dom( executionConfiguration );
                                dom = Xpp3Dom.mergeXpp3Dom( newDom, dom );
                            }
                        }
                    }
                    break;
                }
            }
        }

        if ( dom != null )
        {
            // make a copy so the original in the POM doesn't get messed with
            dom = new Xpp3Dom( dom );
        }

        return dom;
    }

    /**
     * @deprecated Use MavenProjectHelper.attachArtifact(..) instead.
     */
    @Deprecated
    public void attachArtifact( String type, String classifier, File file )
    {
    }

    /**
     * @deprecated Use {@link org.apache.maven.model.io.ModelWriter}.
     */
    @Deprecated
    public void writeModel( Writer writer )
        throws IOException
    {
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write( writer, getModel() );
    }

    /**
     * @deprecated Use {@link org.apache.maven.model.io.ModelWriter}.
     */
    @Deprecated
    public void writeOriginalModel( Writer writer )
        throws IOException
    {
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write( writer, getOriginalModel() );
    }

    @Deprecated
    public Artifact replaceWithActiveArtifact( Artifact pluginArtifact )
    {
        return pluginArtifact;
    }

    /**
     * Gets the project building request from which this project instance was created. <strong>Warning:</strong> This is
     * an utility method that is meant to assist integrators of Maven, it must not be used by Maven plugins.
     *
     * @return The project building request or {@code null}.
     * @since 2.1
     */
    @Deprecated
    public ProjectBuildingRequest getProjectBuildingRequest()
    {
        return projectBuilderConfiguration;
    }

    /**
     * Sets the project building request from which this project instance was created. <strong>Warning:</strong> This is
     * an utility method that is meant to assist integrators of Maven, it must not be used by Maven plugins.
     *
     * @param projectBuildingRequest The project building request, may be {@code null}.
     * @since 2.1
     */
    // used by maven-dependency-tree
    @Deprecated
    public void setProjectBuildingRequest( ProjectBuildingRequest projectBuildingRequest )
    {
        this.projectBuilderConfiguration = projectBuildingRequest;
    }

    private static class ArtifactsHolder
    {
        private ArtifactFilter artifactFilter;
        private Set<Artifact> artifacts;
        private Map<String, Artifact> artifactMap;
    }
}
