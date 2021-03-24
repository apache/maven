package org.apache.maven.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.model.Profile;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.transfer.TransferListener;

/**
 * @author Jason van Zyl
 */
public class DefaultMavenExecutionRequest
    implements MavenExecutionRequest
{

    private RepositoryCache repositoryCache = new DefaultRepositoryCache();

    private WorkspaceReader workspaceReader;

    private ArtifactRepository localRepository;

    private EventSpyDispatcher eventSpyDispatcher;

    private File localRepositoryPath;

    private boolean offline = false;

    private boolean interactiveMode = true;

    private boolean cacheTransferError = false;

    private boolean cacheNotFound = false;

    private List<Proxy> proxies;

    private List<Server> servers;

    private List<Mirror> mirrors;

    private List<Profile> profiles;

    private final ProjectActivation projectActivation = new ProjectActivation();
    private final ProfileActivation profileActivation = new ProfileActivation();

    private List<String> pluginGroups;

    private boolean isProjectPresent = true;

    // ----------------------------------------------------------------------------
    // We need to allow per execution user and global settings as the embedder
    // might be running in a mode where its executing many threads with totally
    // different settings.
    // ----------------------------------------------------------------------------

    private File userSettingsFile;

    private File globalSettingsFile;

    private File userToolchainsFile;

    private File globalToolchainsFile;

    // ----------------------------------------------------------------------------
    // Request
    // ----------------------------------------------------------------------------

    private File multiModuleProjectDirectory;

    private File basedir;

    private List<String> goals;

    private boolean useReactor = false;

    private boolean recursive = true;

    private File pom;

    private String reactorFailureBehavior = REACTOR_FAIL_FAST;

    private boolean resume = false;

    private String resumeFrom;

    private String makeBehavior;

    private Properties systemProperties;

    private Properties userProperties;

    private Date startTime;

    private boolean showErrors = false;

    private TransferListener transferListener;

    private int loggingLevel = LOGGING_LEVEL_INFO;

    private String globalChecksumPolicy;

    private boolean updateSnapshots = false;

    private List<ArtifactRepository> remoteRepositories;

    private List<ArtifactRepository> pluginArtifactRepositories;

    private ExecutionListener executionListener;

    private int degreeOfConcurrency = 1;

    private String builderId = "singlethreaded";

    private Map<String, List<ToolchainModel>> toolchains;

    /**
     * Suppress SNAPSHOT updates.
     *
     * @issue MNG-2681
     */
    private boolean noSnapshotUpdates = false;

    private boolean useLegacyLocalRepositoryManager = false;

    private Map<String, Object> data;

    public DefaultMavenExecutionRequest()
    {
    }

    public static MavenExecutionRequest copy( MavenExecutionRequest original )
    {
        DefaultMavenExecutionRequest copy = new DefaultMavenExecutionRequest();
        copy.setLocalRepository( original.getLocalRepository() );
        copy.setLocalRepositoryPath( original.getLocalRepositoryPath() );
        copy.setOffline( original.isOffline() );
        copy.setInteractiveMode( original.isInteractiveMode() );
        copy.setCacheNotFound( original.isCacheNotFound() );
        copy.setCacheTransferError( original.isCacheTransferError() );
        copy.setProxies( original.getProxies() );
        copy.setServers( original.getServers() );
        copy.setMirrors( original.getMirrors() );
        copy.setProfiles( original.getProfiles() );
        copy.setPluginGroups( original.getPluginGroups() );
        copy.setProjectPresent( original.isProjectPresent() );
        copy.setUserSettingsFile( original.getUserSettingsFile() );
        copy.setGlobalSettingsFile( original.getGlobalSettingsFile() );
        copy.setUserToolchainsFile( original.getUserToolchainsFile() );
        copy.setGlobalToolchainsFile( original.getGlobalToolchainsFile() );
        copy.setBaseDirectory( ( original.getBaseDirectory() != null ) ? new File( original.getBaseDirectory() )
                                                                       : null );
        copy.setGoals( original.getGoals() );
        copy.setRecursive( original.isRecursive() );
        copy.setPom( original.getPom() );
        copy.setSystemProperties( original.getSystemProperties() );
        copy.setUserProperties( original.getUserProperties() );
        copy.setShowErrors( original.isShowErrors() );
        copy.setActiveProfiles( original.getActiveProfiles() );
        copy.setInactiveProfiles( original.getInactiveProfiles() );
        copy.setTransferListener( original.getTransferListener() );
        copy.setLoggingLevel( original.getLoggingLevel() );
        copy.setGlobalChecksumPolicy( original.getGlobalChecksumPolicy() );
        copy.setUpdateSnapshots( original.isUpdateSnapshots() );
        copy.setRemoteRepositories( original.getRemoteRepositories() );
        copy.setPluginArtifactRepositories( original.getPluginArtifactRepositories() );
        copy.setRepositoryCache( original.getRepositoryCache() );
        copy.setWorkspaceReader( original.getWorkspaceReader() );
        copy.setNoSnapshotUpdates( original.isNoSnapshotUpdates() );
        copy.setExecutionListener( original.getExecutionListener() );
        copy.setUseLegacyLocalRepository( original.isUseLegacyLocalRepository() );
        copy.setBuilderId( original.getBuilderId() );
        return copy;
    }

    @Override
    public String getBaseDirectory()
    {
        if ( basedir == null )
        {
            return null;
        }

        return basedir.getAbsolutePath();
    }

    @Override
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    @Override
    public File getLocalRepositoryPath()
    {
        return localRepositoryPath;
    }

    @Override
    public List<String> getGoals()
    {
        if ( goals == null )
        {
            goals = new ArrayList<>();
        }
        return goals;
    }

    @Override
    public Properties getSystemProperties()
    {
        if ( systemProperties == null )
        {
            systemProperties = new Properties();
        }

        return systemProperties;
    }

    @Override
    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        return userProperties;
    }

    @Override
    public File getPom()
    {
        return pom;
    }

    @Override
    public String getReactorFailureBehavior()
    {
        return reactorFailureBehavior;
    }

    @Override
    public List<String> getSelectedProjects()
    {
        return this.projectActivation.getSelectedProjects();
    }

    @Override
    public List<String> getExcludedProjects()
    {
        return this.projectActivation.getExcludedProjects();
    }

    @Override
    public boolean isResume()
    {
        return resume;
    }

    @Override
    public String getResumeFrom()
    {
        return resumeFrom;
    }

    @Override
    public String getMakeBehavior()
    {
        return makeBehavior;
    }

    @Override
    public Date getStartTime()
    {
        return startTime;
    }

    @Override
    public boolean isShowErrors()
    {
        return showErrors;
    }

    @Override
    public boolean isInteractiveMode()
    {
        return interactiveMode;
    }

    @Override
    public MavenExecutionRequest setActiveProfiles( List<String> activeProfiles )
    {
        if ( activeProfiles != null )
        {
            this.profileActivation.overwriteActiveProfiles( activeProfiles );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setInactiveProfiles( List<String> inactiveProfiles )
    {
        if ( inactiveProfiles != null )
        {
            this.profileActivation.overwriteInactiveProfiles( inactiveProfiles );
        }

        return this;
    }

    @Override
    public ProjectActivation getProjectActivation()
    {
        return this.projectActivation;
    }

    @Override
    public ProfileActivation getProfileActivation()
    {
        return this.profileActivation;
    }

    @Override
    public MavenExecutionRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        if ( remoteRepositories != null )
        {
            this.remoteRepositories = new ArrayList<>( remoteRepositories );
        }
        else
        {
            this.remoteRepositories = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setPluginArtifactRepositories( List<ArtifactRepository> pluginArtifactRepositories )
    {
        if ( pluginArtifactRepositories != null )
        {
            this.pluginArtifactRepositories = new ArrayList<>( pluginArtifactRepositories );
        }
        else
        {
            this.pluginArtifactRepositories = null;
        }

        return this;
    }

    public void setProjectBuildingConfiguration( ProjectBuildingRequest projectBuildingConfiguration )
    {
        this.projectBuildingRequest = projectBuildingConfiguration;
    }

    @Override
    public List<String> getActiveProfiles()
    {
        return this.profileActivation.getActiveProfiles();
    }

    @Override
    public List<String> getInactiveProfiles()
    {
        return this.profileActivation.getInactiveProfiles();
    }

    @Override
    public TransferListener getTransferListener()
    {
        return transferListener;
    }

    @Override
    public int getLoggingLevel()
    {
        return loggingLevel;
    }

    @Override
    public boolean isOffline()
    {
        return offline;
    }

    @Override
    public boolean isUpdateSnapshots()
    {
        return updateSnapshots;
    }

    @Override
    public boolean isNoSnapshotUpdates()
    {
        return noSnapshotUpdates;
    }

    @Override
    public String getGlobalChecksumPolicy()
    {
        return globalChecksumPolicy;
    }

    @Override
    public boolean isRecursive()
    {
        return recursive;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    @Override
    public MavenExecutionRequest setBaseDirectory( File basedir )
    {
        this.basedir = basedir;

        return this;
    }

    @Override
    public MavenExecutionRequest setStartTime( Date startTime )
    {
        this.startTime = startTime;

        return this;
    }

    @Override
    public MavenExecutionRequest setShowErrors( boolean showErrors )
    {
        this.showErrors = showErrors;

        return this;
    }

    @Override
    public MavenExecutionRequest setGoals( List<String> goals )
    {
        if ( goals != null )
        {
            this.goals = new ArrayList<>( goals );
        }
        else
        {
            this.goals = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        if ( localRepository != null )
        {
            setLocalRepositoryPath( new File( localRepository.getBasedir() ).getAbsoluteFile() );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setLocalRepositoryPath( File localRepository )
    {
        localRepositoryPath = localRepository;

        return this;
    }

    @Override
    public MavenExecutionRequest setLocalRepositoryPath( String localRepository )
    {
        localRepositoryPath = ( localRepository != null ) ? new File( localRepository ) : null;

        return this;
    }

    @Override
    public MavenExecutionRequest setSystemProperties( Properties properties )
    {
        if ( properties != null )
        {
            this.systemProperties = SystemProperties.copyProperties( properties );
        }
        else
        {
            this.systemProperties = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setUserProperties( Properties userProperties )
    {
        if ( userProperties != null )
        {
            this.userProperties = new Properties();
            this.userProperties.putAll( userProperties );
        }
        else
        {
            this.userProperties = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setReactorFailureBehavior( String failureBehavior )
    {
        reactorFailureBehavior = failureBehavior;

        return this;
    }

    @Override
    public MavenExecutionRequest setSelectedProjects( List<String> selectedProjects )
    {
        if ( selectedProjects != null )
        {
            this.projectActivation.overwriteActiveProjects( selectedProjects );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setExcludedProjects( List<String> excludedProjects )
    {
        if ( excludedProjects != null )
        {
            this.projectActivation.overwriteInactiveProjects( excludedProjects );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setResume( boolean resume )
    {
        this.resume = resume;

        return this;
    }

    @Override
    public MavenExecutionRequest setResumeFrom( String project )
    {
        this.resumeFrom = project;

        return this;
    }

    @Override
    public MavenExecutionRequest setMakeBehavior( String makeBehavior )
    {
        this.makeBehavior = makeBehavior;

        return this;
    }

    @Override
    public MavenExecutionRequest addActiveProfile( String profile )
    {
        if ( !getActiveProfiles().contains( profile ) )
        {
            getActiveProfiles().add( profile );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addInactiveProfile( String profile )
    {
        if ( !getInactiveProfiles().contains( profile ) )
        {
            getInactiveProfiles().add( profile );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addActiveProfiles( List<String> profiles )
    {
        for ( String profile : profiles )
        {
            addActiveProfile( profile );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addInactiveProfiles( List<String> profiles )
    {
        for ( String profile : profiles )
        {
            addInactiveProfile( profile );
        }

        return this;
    }

    public MavenExecutionRequest setUseReactor( boolean reactorActive )
    {
        useReactor = reactorActive;

        return this;
    }

    public boolean useReactor()
    {
        return useReactor;
    }

    /** @deprecated use {@link #setPom(File)} */
    @Deprecated
    public MavenExecutionRequest setPomFile( String pomFilename )
    {
        if ( pomFilename != null )
        {
            pom = new File( pomFilename );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setPom( File pom )
    {
        this.pom = pom;

        return this;
    }

    @Override
    public MavenExecutionRequest setInteractiveMode( boolean interactive )
    {
        interactiveMode = interactive;

        return this;
    }

    @Override
    public MavenExecutionRequest setTransferListener( TransferListener transferListener )
    {
        this.transferListener = transferListener;

        return this;
    }

    @Override
    public MavenExecutionRequest setLoggingLevel( int loggingLevel )
    {
        this.loggingLevel = loggingLevel;

        return this;
    }

    @Override
    public MavenExecutionRequest setOffline( boolean offline )
    {
        this.offline = offline;

        return this;
    }

    @Override
    public MavenExecutionRequest setUpdateSnapshots( boolean updateSnapshots )
    {
        this.updateSnapshots = updateSnapshots;

        return this;
    }

    @Override
    public MavenExecutionRequest setNoSnapshotUpdates( boolean noSnapshotUpdates )
    {
        this.noSnapshotUpdates = noSnapshotUpdates;

        return this;
    }

    @Override
    public MavenExecutionRequest setGlobalChecksumPolicy( String globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;

        return this;
    }

    // ----------------------------------------------------------------------------
    // Settings equivalents
    // ----------------------------------------------------------------------------

    @Override
    public List<Proxy> getProxies()
    {
        if ( proxies == null )
        {
            proxies = new ArrayList<>();
        }
        return proxies;
    }

    @Override
    public MavenExecutionRequest setProxies( List<Proxy> proxies )
    {
        if ( proxies != null )
        {
            this.proxies = new ArrayList<>( proxies );
        }
        else
        {
            this.proxies = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addProxy( Proxy proxy )
    {
        Objects.requireNonNull( proxy, "proxy cannot be null" );

        for ( Proxy p : getProxies() )
        {
            if ( p.getId() != null && p.getId().equals( proxy.getId() ) )
            {
                return this;
            }
        }

        getProxies().add( proxy );

        return this;
    }

    @Override
    public List<Server> getServers()
    {
        if ( servers == null )
        {
            servers = new ArrayList<>();
        }
        return servers;
    }

    @Override
    public MavenExecutionRequest setServers( List<Server> servers )
    {
        if ( servers != null )
        {
            this.servers = new ArrayList<>( servers );
        }
        else
        {
            this.servers = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addServer( Server server )
    {
        Objects.requireNonNull( server, "server cannot be null" );

        for ( Server p : getServers() )
        {
            if ( p.getId() != null && p.getId().equals( server.getId() ) )
            {
                return this;
            }
        }

        getServers().add( server );

        return this;
    }

    @Override
    public List<Mirror> getMirrors()
    {
        if ( mirrors == null )
        {
            mirrors = new ArrayList<>();
        }
        return mirrors;
    }

    @Override
    public MavenExecutionRequest setMirrors( List<Mirror> mirrors )
    {
        if ( mirrors != null )
        {
            this.mirrors = new ArrayList<>( mirrors );
        }
        else
        {
            this.mirrors = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addMirror( Mirror mirror )
    {
        Objects.requireNonNull( mirror, "mirror cannot be null" );

        for ( Mirror p : getMirrors() )
        {
            if ( p.getId() != null && p.getId().equals( mirror.getId() ) )
            {
                return this;
            }
        }

        getMirrors().add( mirror );

        return this;
    }

    @Override
    public List<Profile> getProfiles()
    {
        if ( profiles == null )
        {
            profiles = new ArrayList<>();
        }
        return profiles;
    }

    @Override
    public MavenExecutionRequest setProfiles( List<Profile> profiles )
    {
        if ( profiles != null )
        {
            this.profiles = new ArrayList<>( profiles );
        }
        else
        {
            this.profiles = null;
        }

        return this;
    }

    @Override
    public List<String> getPluginGroups()
    {
        if ( pluginGroups == null )
        {
            pluginGroups = new ArrayList<>();
        }

        return pluginGroups;
    }

    @Override
    public MavenExecutionRequest setPluginGroups( List<String> pluginGroups )
    {
        if ( pluginGroups != null )
        {
            this.pluginGroups = new ArrayList<>( pluginGroups );
        }
        else
        {
            this.pluginGroups = null;
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addPluginGroup( String pluginGroup )
    {
        if ( !getPluginGroups().contains( pluginGroup ) )
        {
            getPluginGroups().add( pluginGroup );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest addPluginGroups( List<String> pluginGroups )
    {
        for ( String pluginGroup : pluginGroups )
        {
            addPluginGroup( pluginGroup );
        }

        return this;
    }

    @Override
    public MavenExecutionRequest setRecursive( boolean recursive )
    {
        this.recursive = recursive;

        return this;
    }

    // calculated from request attributes.
    private ProjectBuildingRequest projectBuildingRequest;

    @Override
    public boolean isProjectPresent()
    {
        return isProjectPresent;
    }

    @Override
    public MavenExecutionRequest setProjectPresent( boolean projectPresent )
    {
        isProjectPresent = projectPresent;

        return this;
    }

    // Settings files

    @Override
    public File getUserSettingsFile()
    {
        return userSettingsFile;
    }

    @Override
    public MavenExecutionRequest setUserSettingsFile( File userSettingsFile )
    {
        this.userSettingsFile = userSettingsFile;

        return this;
    }

    @Override
    public File getGlobalSettingsFile()
    {
        return globalSettingsFile;
    }

    @Override
    public MavenExecutionRequest setGlobalSettingsFile( File globalSettingsFile )
    {
        this.globalSettingsFile = globalSettingsFile;

        return this;
    }

    @Override
    public File getUserToolchainsFile()
    {
        return userToolchainsFile;
    }

    @Override
    public MavenExecutionRequest setUserToolchainsFile( File userToolchainsFile )
    {
        this.userToolchainsFile = userToolchainsFile;

        return this;
    }

    @Override
    public File getGlobalToolchainsFile()
    {
        return globalToolchainsFile;
    }

    @Override
    public MavenExecutionRequest setGlobalToolchainsFile( File globalToolchainsFile )
    {
        this.globalToolchainsFile = globalToolchainsFile;
        return this;
    }

    @Override
    public MavenExecutionRequest addRemoteRepository( ArtifactRepository repository )
    {
        for ( ArtifactRepository repo : getRemoteRepositories() )
        {
            if ( repo.getId() != null && repo.getId().equals( repository.getId() ) )
            {
                return this;
            }
        }

        getRemoteRepositories().add( repository );

        return this;
    }

    @Override
    public List<ArtifactRepository> getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = new ArrayList<>();
        }
        return remoteRepositories;
    }

    @Override
    public MavenExecutionRequest addPluginArtifactRepository( ArtifactRepository repository )
    {
        for ( ArtifactRepository repo : getPluginArtifactRepositories() )
        {
            if ( repo.getId() != null && repo.getId().equals( repository.getId() ) )
            {
                return this;
            }
        }

        getPluginArtifactRepositories().add( repository );

        return this;
    }

    @Override
    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        if ( pluginArtifactRepositories == null )
        {
            pluginArtifactRepositories = new ArrayList<>();
        }
        return pluginArtifactRepositories;
    }

    // TODO this does not belong here.
    @Override
    public ProjectBuildingRequest getProjectBuildingRequest()
    {
        if ( projectBuildingRequest == null )
        {
            projectBuildingRequest = new DefaultProjectBuildingRequest();
            projectBuildingRequest.setLocalRepository( getLocalRepository() );
            projectBuildingRequest.setSystemProperties( getSystemProperties() );
            projectBuildingRequest.setUserProperties( getUserProperties() );
            projectBuildingRequest.setRemoteRepositories( getRemoteRepositories() );
            projectBuildingRequest.setPluginArtifactRepositories( getPluginArtifactRepositories() );
            projectBuildingRequest.setActiveProfileIds( getActiveProfiles() );
            projectBuildingRequest.setInactiveProfileIds( getInactiveProfiles() );
            projectBuildingRequest.setProfiles( getProfiles() );
            projectBuildingRequest.setProcessPlugins( true );
            projectBuildingRequest.setBuildStartTime( getStartTime() );
        }

        return projectBuildingRequest;
    }

    @Override
    public MavenExecutionRequest addProfile( Profile profile )
    {
        Objects.requireNonNull( profile, "profile cannot be null" );

        for ( Profile p : getProfiles() )
        {
            if ( p.getId() != null && p.getId().equals( profile.getId() ) )
            {
                return this;
            }
        }

        getProfiles().add( profile );

        return this;
    }

    @Override
    public RepositoryCache getRepositoryCache()
    {
        return repositoryCache;
    }

    @Override
    public MavenExecutionRequest setRepositoryCache( RepositoryCache repositoryCache )
    {
        this.repositoryCache = repositoryCache;

        return this;
    }

    @Override
    public ExecutionListener getExecutionListener()
    {
        return executionListener;
    }

    @Override
    public MavenExecutionRequest setExecutionListener( ExecutionListener executionListener )
    {
        this.executionListener = executionListener;

        return this;
    }

    @Override
    public void setDegreeOfConcurrency( final int degreeOfConcurrency )
    {
        this.degreeOfConcurrency = degreeOfConcurrency;
    }

    @Override
    public int getDegreeOfConcurrency()
    {
        return degreeOfConcurrency;
    }

    @Override
    public WorkspaceReader getWorkspaceReader()
    {
        return workspaceReader;
    }

    @Override
    public MavenExecutionRequest setWorkspaceReader( WorkspaceReader workspaceReader )
    {
        this.workspaceReader = workspaceReader;
        return this;
    }

    @Override
    public boolean isCacheTransferError()
    {
        return cacheTransferError;
    }

    @Override
    public MavenExecutionRequest setCacheTransferError( boolean cacheTransferError )
    {
        this.cacheTransferError = cacheTransferError;
        return this;
    }

    @Override
    public boolean isCacheNotFound()
    {
        return cacheNotFound;
    }

    @Override
    public MavenExecutionRequest setCacheNotFound( boolean cacheNotFound )
    {
        this.cacheNotFound = cacheNotFound;
        return this;
    }

    @Override
    public boolean isUseLegacyLocalRepository()
    {
        return this.useLegacyLocalRepositoryManager;
    }

    @Override
    public MavenExecutionRequest setUseLegacyLocalRepository( boolean useLegacyLocalRepositoryManager )
    {
        this.useLegacyLocalRepositoryManager = useLegacyLocalRepositoryManager;
        return this;
    }

    @Override
    public MavenExecutionRequest setBuilderId( String builderId )
    {
        this.builderId = builderId;
        return this;
    }

    @Override
    public String getBuilderId()
    {
        return builderId;
    }

    @Override
    public Map<String, List<ToolchainModel>> getToolchains()
    {
        if ( toolchains == null )
        {
            toolchains = new HashMap<>();
        }
        return toolchains;
    }

    @Override
    public MavenExecutionRequest setToolchains( Map<String, List<ToolchainModel>> toolchains )
    {
        this.toolchains = toolchains;
        return this;
    }

    @Override
    public void setMultiModuleProjectDirectory( File directory )
    {
        this.multiModuleProjectDirectory = directory;
    }

    @Override
    public File getMultiModuleProjectDirectory()
    {
        return multiModuleProjectDirectory;
    }

    @Override
    public MavenExecutionRequest setEventSpyDispatcher( EventSpyDispatcher eventSpyDispatcher )
    {
        this.eventSpyDispatcher = eventSpyDispatcher;
        return this;
    }

    @Override
    public EventSpyDispatcher getEventSpyDispatcher()
    {
        return eventSpyDispatcher;
    }

    @Override
    public Map<String, Object> getData()
    {
        if ( data == null )
        {
            data = new HashMap<>();
        }

        return data;
    }
}
