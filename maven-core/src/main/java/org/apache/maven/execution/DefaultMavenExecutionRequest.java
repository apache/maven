package org.apache.maven.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.profiles.ProfileActivationContext;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class DefaultMavenExecutionRequest
    implements MavenExecutionRequest
{
    private ArtifactRepository localRepository;
    
    private File localRepositoryPath;
    
    private boolean offline = false;

    private boolean interactiveMode = true;

    private List proxies;

    private List servers;

    private List mirrors;

    private List profiles;

    private List<String> pluginGroups = new ArrayList<String>();

    private boolean usePluginUpdateOverride;

    private boolean isProjectPresent = true;

    // ----------------------------------------------------------------------------
    // We need to allow per execution user and global settings as the embedder
    // might be running in a mode where its executing many threads with totally
    // different settings.
    // ----------------------------------------------------------------------------

    private File userSettingsFile;

    private File globalSettingsFile;

    // ----------------------------------------------------------------------------
    // Request
    // ----------------------------------------------------------------------------

    private File basedir;

    private List<String> goals;

    private boolean useReactor = false;

    private boolean recursive = true;

    private File pom;

    private String reactorFailureBehavior = REACTOR_FAIL_FAST;

    private Properties properties;

    private Properties userProperties;

    private Date startTime;

    private boolean showErrors = false;

    private List<EventMonitor> eventMonitors;

    private List<String> activeProfiles;

    private List<String> inactiveProfiles;

    private TransferListener transferListener;

    private int loggingLevel = LOGGING_LEVEL_INFO;

    private String globalChecksumPolicy = CHECKSUM_POLICY_WARN;

    private boolean updateSnapshots = false;

    private ProfileManager profileManager;

    private List<ArtifactRepository> remoteRepositories;

    /**
     * Suppress SNAPSHOT updates.
     * 
     * @issue MNG-2681
     */
    private boolean noSnapshotUpdates;
        
    public static MavenExecutionRequest copy( MavenExecutionRequest original )
    {
        DefaultMavenExecutionRequest copy = new DefaultMavenExecutionRequest();
        copy.setLocalRepository( original.getLocalRepository() );
        copy.setLocalRepositoryPath( original.getLocalRepositoryPath() );
        copy.setOffline(  original.isOffline() );
        copy.setInteractiveMode( original.isInteractiveMode() );
        copy.setProxies( original.getProxies() );
        copy.setServers( original.getServers() );
        copy.setMirrors( original.getMirrors() );
        copy.setProfiles( original.getProfiles() );
        copy.setPluginGroups( original.getPluginGroups() );
        copy.setUsePluginUpdateOverride( original.isUsePluginUpdateOverride() );
        copy.setProjectPresent( original.isProjectPresent() );
        copy.setUserSettingsFile( original.getUserSettingsFile() );
        copy.setGlobalSettingsFile( original.getGlobalSettingsFile() );
        copy.setBaseDirectory( new File( original.getBaseDirectory() ) );
        copy.setGoals( original.getGoals() );
        copy.setUseReactor( original.useReactor() );
        copy.setRecursive( original.isRecursive() );
        copy.setPom( original.getPom() );
        copy.setReactorFailureBehavior( original.getReactorFailureBehavior() );
        copy.setProperties( original.getProperties() );
        copy.setStartTime( original.getStartTime() );
        copy.setShowErrors( original.isShowErrors() );
        copy.setEventMonitors( original.getEventMonitors());
        copy.setActiveProfiles( original.getActiveProfiles());
        copy.setInactiveProfiles(  original.getInactiveProfiles());
        copy.setTransferListener( original.getTransferListener());
        copy.setLoggingLevel( original.getLoggingLevel());
        copy.setGlobalChecksumPolicy( original.getGlobalChecksumPolicy());
        copy.setUpdateSnapshots( original.isUpdateSnapshots());
        copy.setProfileManager( original.getProfileManager() );
        copy.setRemoteRepositories( original.getRemoteRepositories() );
        copy.setNoSnapshotUpdates( original.isNoSnapshotUpdates() );
        return original;        
    }
   
    public String getBaseDirectory()
    {
        if ( basedir == null )
        {
            return null;
        }

        return basedir.getAbsolutePath();
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public File getLocalRepositoryPath()
    {
        return localRepositoryPath;
    }

    public List<String> getGoals()
    {
        return goals;
    }

    public Properties getProperties()
    {
        return properties;
    }

    /** @deprecated use {@link #getPom()} */
    public String getPomFile()
    {
        return pom.getAbsolutePath();
    }

    public File getPom()
    {
        return pom;
    }

    public String getReactorFailureBehavior()
    {
        return reactorFailureBehavior;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public boolean isShowErrors()
    {
        return showErrors;
    }

    public boolean isInteractiveMode()
    {
        return interactiveMode;
    }

    public List<EventMonitor> getEventMonitors()
    {
        return eventMonitors;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public void setEventMonitors( List<EventMonitor> eventMonitors )
    {
        this.eventMonitors = eventMonitors;
    }

    public void setActiveProfiles( List<String> activeProfiles )
    {
        this.activeProfiles = activeProfiles;
    }

    public void setInactiveProfiles( List<String> inactiveProfiles )
    {
        this.inactiveProfiles = inactiveProfiles;
    }

    public MavenExecutionRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
        
        return this;
    }

    public void setProjectBuildingConfiguration( ProjectBuilderConfiguration projectBuildingConfiguration )
    {
        this.projectBuildingConfiguration = projectBuildingConfiguration;
    }

    public List<String> getActiveProfiles()
    {
        if ( activeProfiles == null )
        {
            activeProfiles = new ArrayList<String>();
        }
        return activeProfiles;
    }

    public List<String> getInactiveProfiles()
    {
        if ( inactiveProfiles == null )
        {
            inactiveProfiles = new ArrayList<String>();
        }
        return inactiveProfiles;
    }

    public TransferListener getTransferListener()
    {
        return transferListener;
    }

    public int getLoggingLevel()
    {
        return loggingLevel;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public boolean isUpdateSnapshots()
    {
        return updateSnapshots;
    }

    public boolean isNoSnapshotUpdates()
    {
        return noSnapshotUpdates;
    }

    public String getGlobalChecksumPolicy()
    {
        return globalChecksumPolicy;
    }

    public boolean isRecursive()
    {
        return recursive;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public MavenExecutionRequest setBaseDirectory( File basedir )
    {
        this.basedir = basedir;

        return this;
    }

    public MavenExecutionRequest setStartTime( Date startTime )
    {
        this.startTime = startTime;

        return this;
    }

    public MavenExecutionRequest setShowErrors( boolean showErrors )
    {
        this.showErrors = showErrors;

        return this;
    }

    public MavenExecutionRequest setGoals( List<String> goals )
    {
        this.goals = goals;

        return this;
    }

    public MavenExecutionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public MavenExecutionRequest setLocalRepositoryPath( File localRepository )
    {
        localRepositoryPath = localRepository;

        return this;
    }

    public MavenExecutionRequest setLocalRepositoryPath( String localRepository )
    {
        localRepositoryPath = new File( localRepository );

        return this;
    }

    public MavenExecutionRequest setProperties( Properties properties )
    {
        if ( this.properties == null )
        {
            this.properties = properties;
        }
        else
        {
            this.properties.putAll( properties );
        }

        return this;
    }

    public MavenExecutionRequest setProperty( String key, String value )
    {
        if ( properties == null )
        {
            properties = new Properties();
        }

        properties.setProperty( key, value );

        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        userProperties.setProperty( key, value );

        return this;
    }

    public MavenExecutionRequest setReactorFailureBehavior( String failureBehavior )
    {
        reactorFailureBehavior = failureBehavior;

        return this;
    }

    public MavenExecutionRequest addActiveProfile( String profile )
    {
        getActiveProfiles().add( profile );

        return this;
    }

    public MavenExecutionRequest addInactiveProfile( String profile )
    {
        getInactiveProfiles().add( profile );

        return this;
    }

    public MavenExecutionRequest addActiveProfiles( List<String> profiles )
    {
        getActiveProfiles().addAll( profiles );

        return this;
    }

    public MavenExecutionRequest addInactiveProfiles( List<String> profiles )
    {
        getInactiveProfiles().addAll( profiles );

        return this;
    }

    public MavenExecutionRequest addEventMonitor( EventMonitor monitor )
    {
        if ( eventMonitors == null )
        {
            eventMonitors = new ArrayList<EventMonitor>();
        }

        eventMonitors.add( monitor );

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
    public MavenExecutionRequest setPomFile( String pomFilename )
    {
        if ( pomFilename != null )
        {
            pom = new File( pomFilename );
        }

        return this;
    }

    public MavenExecutionRequest setPom( File pom )
    {
        this.pom = pom;

        return this;
    }

    public MavenExecutionRequest setInteractiveMode( boolean interactive )
    {
        interactiveMode = interactive;

        return this;
    }

    public MavenExecutionRequest setTransferListener( TransferListener transferListener )
    {
        this.transferListener = transferListener;

        return this;
    }

    public MavenExecutionRequest setLoggingLevel( int loggingLevel )
    {
        this.loggingLevel = loggingLevel;

        return this;
    }

    public MavenExecutionRequest setOffline( boolean offline )
    {
        this.offline = offline;

        return this;
    }

    public MavenExecutionRequest setUpdateSnapshots( boolean updateSnapshots )
    {
        this.updateSnapshots = updateSnapshots;

        return this;
    }

    public MavenExecutionRequest setNoSnapshotUpdates( boolean noSnapshotUpdates )
    {
        this.noSnapshotUpdates = noSnapshotUpdates;

        return this;
    }

    public MavenExecutionRequest setGlobalChecksumPolicy( String globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;

        return this;
    }

    // ----------------------------------------------------------------------------
    // Settings equivalents
    // ----------------------------------------------------------------------------

    public List getProxies()
    {
        return proxies;
    }

    public MavenExecutionRequest setProxies( List proxies )
    {
        this.proxies = proxies;

        return this;
    }

    public List getServers()
    {
        return servers;
    }

    public MavenExecutionRequest setServers( List servers )
    {
        this.servers = servers;

        return this;
    }

    public List getMirrors()
    {
        return mirrors;
    }

    public MavenExecutionRequest setMirrors( List mirrors )
    {
        this.mirrors = mirrors;

        return this;
    }

    public List getProfiles()
    {
        return profiles;
    }

    public MavenExecutionRequest setProfiles( List profiles )
    {
        this.profiles = profiles;

        return this;
    }

    public List<String> getPluginGroups()
    {
        return pluginGroups;
    }

    public MavenExecutionRequest setPluginGroups( List<String> pluginGroups )
    {
        this.pluginGroups = pluginGroups;

        return this;
    }

    public boolean isUsePluginUpdateOverride()
    {
        return usePluginUpdateOverride;
    }

    public MavenExecutionRequest setUsePluginUpdateOverride( boolean usePluginUpdateOverride )
    {
        this.usePluginUpdateOverride = usePluginUpdateOverride;

        return this;
    }

    public MavenExecutionRequest setRecursive( boolean recursive )
    {
        this.recursive = recursive;

        return this;
    }

    private Settings settings;

    private ProfileActivationContext profileActivationContext;

    // calculated from request attributes.
    private ProjectBuilderConfiguration projectBuildingConfiguration;

    public MavenExecutionRequest setSettings( Settings settings )
    {
        this.settings = settings;

        return this;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public ProfileManager getProfileManager()
    {
        return profileManager;
    }

    public MavenExecutionRequest setProfileManager( ProfileManager profileManager )
    {
        this.profileManager = profileManager;

        return this;
    }

    public boolean isProjectPresent()
    {
        return isProjectPresent;
    }

    public MavenExecutionRequest setProjectPresent( boolean projectPresent )
    {
        isProjectPresent = projectPresent;

        return this;
    }

    // Settin10gs files

    public File getUserSettingsFile()
    {
        return userSettingsFile;
    }

    public MavenExecutionRequest setUserSettingsFile( File userSettingsFile )
    {
        this.userSettingsFile = userSettingsFile;

        return this;
    }

    public File getGlobalSettingsFile()
    {
        return globalSettingsFile;
    }

    public MavenExecutionRequest setGlobalSettingsFile( File globalSettingsFile )
    {
        this.globalSettingsFile = globalSettingsFile;

        return this;
    }

    public MavenExecutionRequest addRemoteRepository( ArtifactRepository repository )
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = new ArrayList<ArtifactRepository>();
        }

        remoteRepositories.add( repository );

        return this;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public ProfileActivationContext getProfileActivationContext()
    {
        return profileActivationContext;
    }

    public MavenExecutionRequest setProfileActivationContext( ProfileActivationContext profileActivationContext )
    {
        this.profileActivationContext = profileActivationContext;
        return this;
    }

    public Properties getUserProperties()
    {
        return userProperties;
    }

    public MavenExecutionRequest setUserProperties( Properties userProperties )
    {
        this.userProperties = userProperties;
        return this;
    }

    public ProjectBuilderConfiguration getProjectBuildingConfiguration()
    {
        if ( projectBuildingConfiguration == null )
        {
            projectBuildingConfiguration = new DefaultProjectBuilderConfiguration();
            projectBuildingConfiguration.setLocalRepository( getLocalRepository() );
            projectBuildingConfiguration.setExecutionProperties( getProperties() );
            projectBuildingConfiguration.setGlobalProfileManager( getProfileManager() );
            projectBuildingConfiguration.setUserProperties( getUserProperties() );
            projectBuildingConfiguration.setBuildStartTime( getStartTime() );
            projectBuildingConfiguration.setRemoteRepositories( getRemoteRepositories() );
        }

        return projectBuildingConfiguration;
    }
}
