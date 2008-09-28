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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.realm.MavenRealmManager;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

    private List pluginGroups;

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

    private List goals;

    private boolean useReactor = false;

    private boolean recursive = true;

    private File pom;

    private String reactorFailureBehavior = REACTOR_FAIL_FAST;

    private Properties properties;

    private Properties userProperties;

    private Date startTime;

    private boolean showErrors = false;

    private List eventMonitors;

    private List activeProfiles;

    private List inactiveProfiles;

    private TransferListener transferListener;

    private int loggingLevel = LOGGING_LEVEL_INFO;

    private String globalChecksumPolicy = CHECKSUM_POLICY_WARN;

    private boolean updateSnapshots = false;

    private ProfileManager profileManager;

    private List remoteRepositories;

    /**
     * Suppress SNAPSHOT updates.
     * @issue MNG-2681
     */
    private boolean noSnapshotUpdates;

    private MavenRealmManager realmManager;

    public DefaultMavenExecutionRequest()
    {
        // default constructor.
    }

    public DefaultMavenExecutionRequest( MavenExecutionRequest original )
    {
        localRepository = original.getLocalRepository();
        localRepositoryPath = original.getLocalRepositoryPath();
        offline = original.isOffline();
        interactiveMode = original.isInteractiveMode();
        proxies = original.getProxies();
        servers = original.getServers();
        mirrors = original.getMirrors();
        profiles = original.getProfiles();
        pluginGroups = original.getPluginGroups();
        usePluginUpdateOverride = original.isUsePluginUpdateOverride();
        isProjectPresent = original.isProjectPresent();
        userSettingsFile = original.getUserSettingsFile();
        globalSettingsFile = original.getGlobalSettingsFile();
        basedir = new File( original.getBaseDirectory() );
        goals = original.getGoals();
        useReactor = original.useReactor();
        recursive = original.isRecursive();
        pom = original.getPom();
        reactorFailureBehavior = original.getReactorFailureBehavior();
        properties = original.getProperties();
        startTime = original.getStartTime();
        showErrors = original.isShowErrors();
        eventMonitors = original.getEventMonitors();
        activeProfiles = original.getActiveProfiles();
        inactiveProfiles = original.getInactiveProfiles();
        transferListener = original.getTransferListener();
        loggingLevel = original.getLoggingLevel();
        globalChecksumPolicy = original.getGlobalChecksumPolicy();
        updateSnapshots = original.isUpdateSnapshots();
        profileManager = original.getProfileManager();
        remoteRepositories = original.getRemoteRepositories();
        noSnapshotUpdates = original.isNoSnapshotUpdates();
        realmManager = original.getRealmManager();
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

    public List getGoals()
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

    public List getEventMonitors()
    {
        return eventMonitors;
    }

    public List getActiveProfiles()
    {
        if ( activeProfiles == null )
        {
            activeProfiles = new ArrayList();
        }
        return activeProfiles;
    }

    public List getInactiveProfiles()
    {
        if ( inactiveProfiles == null )
        {
            inactiveProfiles = new ArrayList();
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

    public MavenExecutionRequest setGoals( List goals )
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

    public MavenExecutionRequest addActiveProfiles( List profiles )
    {
        getActiveProfiles().addAll( profiles );

        return this;
    }

    public MavenExecutionRequest addInactiveProfiles( List profiles )
    {
        getInactiveProfiles().addAll( profiles );

        return this;
    }

    public MavenExecutionRequest addEventMonitor( EventMonitor monitor )
    {
        if ( eventMonitors == null )
        {
            eventMonitors = new ArrayList();
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

    public List getPluginGroups()
    {
        return pluginGroups;
    }

    public MavenExecutionRequest setPluginGroups( List pluginGroups )
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

    private CoreErrorReporter errorReporter;

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

    // Settings files

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
            remoteRepositories = new ArrayList();
        }

        remoteRepositories.add( repository );

        return this;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public MavenExecutionRequest setRealmManager( MavenRealmManager realmManager )
    {
        this.realmManager = realmManager;
        return this;
    }

    public MavenRealmManager getRealmManager()
    {
        return realmManager;
    }

    public MavenExecutionRequest clearAccumulatedBuildState()
    {
        realmManager.clear();

        return this;
    }

    public CoreErrorReporter getErrorReporter()
    {
        return errorReporter;
    }

    public MavenExecutionRequest setErrorReporter( CoreErrorReporter reporter )
    {
        errorReporter = reporter;
        return this;
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
        }

        return projectBuildingConfiguration;
    }
}
