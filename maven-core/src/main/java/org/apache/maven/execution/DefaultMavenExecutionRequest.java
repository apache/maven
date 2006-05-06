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

import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultMavenExecutionRequest
    implements MavenExecutionRequest
{
    private File basedir;

    /**
     * @todo [BP] is this required? This hands off to MavenSession, but could be passed through the handler.handle function (+ createSession).
     */
    private ArtifactRepository localRepository;

    private File localRepositoryPath;

    private  List goals;

    protected MavenSession session;

    private  Settings settings;

    private boolean recursive = true;

    private boolean reactorActive;

    private String pomFilename;

    private String failureBehavior;

    private  Properties properties;

    private  Date startTime;

    private  boolean showErrors;

    private List eventMonitors;

    private List activeProfiles;

    private List inactiveProfiles;

    private boolean interactive;

    private TransferListener transferListener;

    private int loggingLevel;

    private boolean activateDefaultEventMonitor;

    private boolean offline;

    private boolean updateSnapshots;

    private String globalChecksumPolicy;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getBaseDirectory()
    {
        return basedir.getAbsolutePath();
    }

    public Settings getSettings()
    {
        return settings;
    }

    public boolean isRecursive()
    {
        return recursive;
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

    public MavenSession getSession()
    {
        return session;
    }

    public String getPomFile()
    {
        return pomFilename;
    }

    public String getFailureBehavior()
    {
        return failureBehavior;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public boolean isShowErrors()
    {
        return showErrors;
    }

    public boolean isInteractive()
    {
        return interactive;
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

    public boolean isDefaultEventMonitorActivated()
    {
        return activateDefaultEventMonitor;
    }

    public int getLoggingLevel()
    {
        return loggingLevel;
    }

    public boolean isDefaultEventMonitorActive()
    {
        return activateDefaultEventMonitor;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public boolean isUpdateSnapshots()
    {
        return updateSnapshots;
    }

    public String getGlobalChecksumPolicy()
    {
        return globalChecksumPolicy;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public MavenExecutionRequest setBasedir( File basedir )
    {
        this.basedir = basedir;

        return this;
    }

    public MavenExecutionRequest setStartTime( Date startTime )
    {
        this.startTime= startTime;

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

    public MavenExecutionRequest setFailureBehavior( String failureBehavior )
    {
        this.failureBehavior = failureBehavior;

        return this;
    }

    public MavenExecutionRequest setSession( MavenSession session )
    {
        this.session = session;

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

    public MavenExecutionRequest activateDefaultEventMonitor()
    {
        activateDefaultEventMonitor = true;

        return this;
    }

    public MavenExecutionRequest setReactorActive( boolean reactorActive )
    {
        this.reactorActive = reactorActive;

        return this;
    }

    public boolean isReactorActive()
    {
        return reactorActive;
    }

    public MavenExecutionRequest setPomFile( String pomFilename )
    {
        this.pomFilename = pomFilename;

        return this;
    }

    public MavenExecutionRequest setRecursive( boolean recursive )
    {
        this.recursive = recursive;

        return this;
    }

    public MavenExecutionRequest setInteractive( boolean interactive )
    {
        this.interactive = interactive;

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

    public MavenExecutionRequest setGlobalChecksumPolicy( String globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;

        return this;
    }
}
