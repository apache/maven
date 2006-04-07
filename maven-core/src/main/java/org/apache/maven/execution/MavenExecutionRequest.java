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
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface MavenExecutionRequest
{
    File getLocalRepositoryPath();

    ArtifactRepository getLocalRepository();

    List getGoals();

    Settings getSettings();

    String getBaseDirectory();

    boolean isRecursive();

    boolean isInteractive();

    boolean isReactorActive();

    String getPomFile();

    String getFailureBehavior();

    Properties getProperties();

    Date getStartTime();

    boolean isShowErrors();

    List getEventMonitors();

    List getActiveProfiles();

    List getInactiveProfiles();

    TransferListener getTransferListener();

    int getLoggingLevel();

    boolean isDefaultEventMonitorActive();

    boolean isOffline();

    boolean isUpdateSnapshots();

    String getGlobalChecksumPolicy();

    // ----------------------------------------------------------------------
    // Logging
    // ----------------------------------------------------------------------

    static final int LOGGING_LEVEL_DEBUG = Logger.LEVEL_DEBUG;

    static final int LOGGING_LEVEL_INFO = Logger.LEVEL_INFO;

    static final int LOGGING_LEVEL_WARN = Logger.LEVEL_WARN;

    static final int LOGGING_LEVEL_ERROR = Logger.LEVEL_ERROR;

    static final int LOGGING_LEVEL_FATAL = Logger.LEVEL_FATAL;

    static final int LOGGING_LEVEL_DISABLED = Logger.LEVEL_DISABLED;

    // ----------------------------------------------------------------------
    // Reactor Failure Mode
    // ----------------------------------------------------------------------

    static final String REACTOR_FAIL_FAST = ReactorManager.FAIL_FAST;

    static final String REACTOR_FAIL_AT_END = ReactorManager.FAIL_AT_END;

    static final String REACTOR_FAIL_NEVER = ReactorManager.FAIL_NEVER;

    // ----------------------------------------------------------------------
    // Artifactr repository policies
    // ----------------------------------------------------------------------
    
    static final String CHECKSUM_POLICY_FAIL = ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL;

    static final String CHECKSUM_POLICY_WARN = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    MavenExecutionRequest setBasedir( File basedir );

    MavenExecutionRequest setSettings( Settings settings );

    MavenExecutionRequest setStartTime( Date start );

    MavenExecutionRequest setGoals( List goals );

    MavenExecutionRequest setLocalRepository( ArtifactRepository localRepository );

    MavenExecutionRequest setLocalRepositoryPath( String localRepository );

    MavenExecutionRequest setLocalRepositoryPath( File localRepository );

    MavenExecutionRequest setProperties( Properties properties );

    MavenExecutionRequest setFailureBehavior( String failureBehavior );

    MavenExecutionRequest setSession( MavenSession session );

    MavenExecutionRequest addActiveProfile( String profile );

    MavenExecutionRequest addInactiveProfile( String profile );

    MavenExecutionRequest addActiveProfiles( List profiles );

    MavenExecutionRequest addInactiveProfiles( List profiles );

    MavenExecutionRequest addEventMonitor( EventMonitor monitor );

    MavenExecutionRequest setReactorActive( boolean reactorActive );

    MavenExecutionRequest setPomFile( String pomFilename );

    MavenExecutionRequest setRecursive( boolean recursive );

    MavenExecutionRequest setShowErrors( boolean showErrors );

    MavenExecutionRequest setInteractive( boolean interactive );

    MavenExecutionRequest setTransferListener( TransferListener transferListener );

    MavenExecutionRequest setLoggingLevel( int loggingLevel );

    MavenExecutionRequest activateDefaultEventMonitor();

    MavenExecutionRequest setOffline( boolean offline );

    MavenExecutionRequest setUpdateSnapshots( boolean updateSnapshots );

    MavenExecutionRequest setGlobalChecksumPolicy( String globalChecksumPolicy );
}
