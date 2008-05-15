package org.apache.maven.cli;

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

import org.apache.commons.cli.CommandLine;
import org.apache.maven.MavenTransferListener;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public final class CLIRequestUtils
{

    private CLIRequestUtils()
    {
    }

    public static MavenExecutionRequest buildRequest( CommandLine commandLine, boolean debug, boolean quiet, boolean showErrors )
    {
        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        boolean interactive = true;

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            interactive = false;
        }

        boolean pluginUpdateOverride = false;

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES ) ||
            commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            pluginUpdateOverride = true;
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            pluginUpdateOverride = false;
        }

        boolean noSnapshotUpdates = false;
        if ( commandLine.hasOption( CLIManager.SUPRESS_SNAPSHOT_UPDATES ) )
        {
            noSnapshotUpdates = true;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        List goals = commandLine.getArgList();

        boolean recursive = true;

        // this is the default behavior.
        String reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;

        if ( commandLine.hasOption( CLIManager.NON_RECURSIVE ) )
        {
            recursive = false;
        }

        if ( commandLine.hasOption( CLIManager.FAIL_FAST ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_AT_END ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_AT_END;
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_NEVER ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_NEVER;
        }

        boolean offline = false;

        if ( commandLine.hasOption( CLIManager.OFFLINE ) )
        {
            offline = true;
        }

        boolean updateSnapshots = false;

        if ( commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            updateSnapshots = true;
        }

        String globalChecksumPolicy = null;

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            // todo; log
            System.out.println( "+ Enabling strict checksum verification on all artifact downloads." );

            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            // todo: log
            System.out.println( "+ Disabling strict checksum verification on all artifact downloads." );

            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        }

        File baseDirectory = new File( System.getProperty( "user.dir" ) );

        // ----------------------------------------------------------------------
        // Profile Activation
        // ----------------------------------------------------------------------

        List activeProfiles = new ArrayList();

        List inactiveProfiles = new ArrayList();

        if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
        {
            String [] profileOptionValues = commandLine.getOptionValues( CLIManager.ACTIVATE_PROFILES );
            if ( profileOptionValues != null )
            {
                for ( int i=0; i < profileOptionValues.length; ++i )
                {
                    StringTokenizer profileTokens = new StringTokenizer( profileOptionValues[i] , "," );

                    while ( profileTokens.hasMoreTokens() )
                    {
                        String profileAction = profileTokens.nextToken().trim();

                        if ( profileAction.startsWith( "-" ) || profileAction.startsWith( "!" ) )
                        {
                            inactiveProfiles.add( profileAction.substring( 1 ) );
                        }
                        else if ( profileAction.startsWith( "+" ) )
                        {
                            activeProfiles.add( profileAction.substring( 1 ) );
                        }
                        else
                        {
                            activeProfiles.add( profileAction );
                        }
                    }
                }
            }
        }

        MavenTransferListener transferListener;

        if ( interactive )
        {
            transferListener = new ConsoleDownloadMonitor();
        }
        else
        {
            transferListener = new BatchModeDownloadMonitor();
        }

        transferListener.setShowChecksumEvents( false );

        // This means to scan a directory structure for POMs and process them.
        boolean useReactor = false;

        if ( commandLine.hasOption( CLIManager.REACTOR ) )
        {
            useReactor = true;
        }

        String alternatePomFile = null;
        if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
        {
            alternatePomFile = commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE );
        }

        int loggingLevel;

        if ( debug )
        {
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_DEBUG;
        }
        else if ( quiet )
        {
            // TODO: we need to do some more work here. Some plugins use sys out or log errors at info level.
            // Ideally, we could use Warn across the board
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_ERROR;
            // TODO:Additionally, we can't change the mojo level because the component key includes the version and it isn't known ahead of time. This seems worth changing.
        }
        else
        {
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_INFO;
        }

        Properties executionProperties = new Properties();
        Properties userProperties = new Properties();
        populateProperties( commandLine, executionProperties, userProperties );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBaseDirectory( baseDirectory )
            .setGoals( goals )
            .setProperties( executionProperties ) // optional
            .setUserProperties( userProperties ) // optional
            .setReactorFailureBehavior( reactorFailureBehaviour ) // default: fail fast
            .setRecursive( recursive ) // default: true
            .setUseReactor( useReactor ) // default: false
            .setShowErrors( showErrors ) // default: false
            .setInteractiveMode( interactive ) // default: false
            .setOffline( offline ) // default: false
            .setUsePluginUpdateOverride( pluginUpdateOverride )
            .addActiveProfiles( activeProfiles ) // optional
            .addInactiveProfiles( inactiveProfiles ) // optional
            .setLoggingLevel( loggingLevel ) // default: info
            .setTransferListener( transferListener ) // default: batch mode which goes along with interactive
            .setUpdateSnapshots( updateSnapshots ) // default: false
            .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
            .setGlobalChecksumPolicy( globalChecksumPolicy ); // default: warn

        if ( alternatePomFile != null )
        {
            request.setPom( new File( alternatePomFile ) );
            System.out.println( "Request pom set to: " + request.getPom() );
        }

        return request;
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    static void populateProperties( CommandLine commandLine, Properties executionProperties, Properties userProperties )
    {
        System.setProperty( MavenEmbedder.STANDALONE_MODE, "true" );
        executionProperties.setProperty( MavenEmbedder.STANDALONE_MODE, "true" );

        // add the env vars to the property set, with the "env." prefix
        // XXX support for env vars should probably be removed from the ModelInterpolator
        try
        {
            Properties envVars = CommandLineUtils.getSystemEnvVars();
            Iterator i = envVars.entrySet().iterator();
            while ( i.hasNext() )
            {
                Entry e = (Entry) i.next();
                executionProperties.setProperty( "env." + e.getKey().toString(), e.getValue().toString() );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Error getting environment vars for profile activation: " + e );
        }

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY );

            if ( defStrs != null )
            {
                for ( int i = 0; i < defStrs.length; ++i )
                {
                    setCliProperty( defStrs[i], userProperties );
                }
            }

            executionProperties.putAll( userProperties );
        }

        executionProperties.putAll( System.getProperties() );
    }

    private static void setCliProperty( String property,
                                        Properties executionProperties )
    {
        String name;

        String value;

        int i = property.indexOf( "=" );

        if ( i <= 0 )
        {
            name = property.trim();

            value = "true";
        }
        else
        {
            name = property.substring( 0, i ).trim();

            value = property.substring( i + 1 ).trim();
        }

        executionProperties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }
}
