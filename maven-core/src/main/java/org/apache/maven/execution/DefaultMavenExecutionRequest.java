package org.apache.maven.execution;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultMavenExecutionRequest
    implements MavenExecutionRequest
{
    // ----------------------------------------------------------------------------
    // Settings equivalents
    // ----------------------------------------------------------------------------

    private Settings settings;

    private String settingsFile;

    private ArtifactRepository localRepository;
    
    private File localRepositoryPath;

    private boolean offline = false;

    private boolean interactiveMode = true;

    private List proxies;

    private List servers;

    private List mirrors;

    private List profiles;

    private List pluginGroups;

    // This is off by default because it causes so many problem. It should just be extracted and redone.
    private boolean usePluginRegistry = false;

    private boolean usePluginUpdateOverride;

    // ----------------------------------------------------------------------------
    // Request
    // ----------------------------------------------------------------------------

    private File basedir;

    private List goals;

    private boolean useReactor = false;

    private String pomFile;

    private String reactorFailureBehavior = REACTOR_FAIL_FAST;

    private Properties properties;

    private Date startTime;

    private boolean showErrors = false;

    private List eventMonitors;

    private List activeProfiles;

    private List inactiveProfiles;

    private TransferListener transferListener;

    private int loggingLevel = LOGGING_LEVEL_INFO;

    private String globalChecksumPolicy = CHECKSUM_POLICY_WARN;

    private boolean recursive = true;
        
    private boolean updateSnapshots = false;

    /**
     * Suppress SNAPSHOT updates. 
     * @issue MNG-2681
     */
    private boolean noSnapshotUpdates;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getSettingsFile()
    {
        return settingsFile;
    }

    public MavenExecutionRequest setSettingsFile( String settingsFile )
    {
        this.settingsFile = settingsFile;

        return this;
    }

    public String getBaseDirectory()
    {
        if ( basedir == null )
        {
            return null;
        }

        return basedir.getAbsolutePath();
    }

    public Settings getSettings()
    {
        return settings;
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

    public String getPomFile()
    {
        return pomFile;
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

    public MavenExecutionRequest setSettings( Settings settings )
    {
        this.settings = settings;

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
        this.localRepositoryPath = localRepository;

        return this;
    }

    public MavenExecutionRequest setLocalRepositoryPath( String localRepository )
    {
        this.localRepositoryPath = new File( localRepository );

        return this;
    }

    public MavenExecutionRequest setProperties( Properties properties )
    {
        this.properties = properties;

        return this;
    }

    public MavenExecutionRequest setReactorFailureBehavior( String failureBehavior )
    {
        this.reactorFailureBehavior = failureBehavior;

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
        this.useReactor = reactorActive;

        return this;
    }

    public boolean useReactor()
    {
        return useReactor;
    }

    public MavenExecutionRequest setPomFile( String pomFilename )
    {
        this.pomFile = pomFilename;

        return this;
    }

    public MavenExecutionRequest setInteractiveMode( boolean interactive )
    {
        this.interactiveMode = interactive;

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

    public boolean isUsePluginRegistry()
    {
        return usePluginRegistry;
    }

    public MavenExecutionRequest setUsePluginRegistry( boolean usePluginRegistry )
    {
        this.usePluginRegistry = usePluginRegistry;

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

    //

    public MavenExecutionRequest setRecursive( boolean recursive )
    {
        this.recursive = recursive;

        return this;
    }

}
