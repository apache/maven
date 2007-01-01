package org.apache.maven.cli;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.MavenTransferListener;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.classworlds.ClassWorld;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author jason van zyl
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    public static void main( String[] args )
    {
        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );

        int result = main( args, classWorld );

        System.exit( result );
    }

    /** @noinspection ConfusingMainMethod */
    public static int main( String[] args,
                            ClassWorld classWorld )
    {
        // ----------------------------------------------------------------------
        // Setup the command line parser
        // ----------------------------------------------------------------------

        CLIManager cliManager = new CLIManager();

        CommandLine commandLine;
        try
        {
            commandLine = cliManager.parse( args );
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );
            cliManager.displayHelp();
            return 1;
        }

        // TODO: maybe classworlds could handle this requirement...
        if ( "1.4".compareTo( System.getProperty( "java.specification.version" ) ) > 0 )
        {
            System.err.println(
                "Sorry, but JDK 1.4 or above is required to execute Maven. You appear to be using " + "Java:" );
            System.err.println(
                "java version \"" + System.getProperty( "java.version", "<unknown java version>" ) + "\"" );
            System.err.println( System.getProperty( "java.runtime.name", "<unknown runtime name>" ) + " (build " +
                System.getProperty( "java.runtime.version", "<unknown runtime version>" ) + ")" );
            System.err.println( System.getProperty( "java.vm.name", "<unknown vm name>" ) + " (build " +
                System.getProperty( "java.vm.version", "<unknown vm version>" ) + ", " +
                System.getProperty( "java.vm.info", "<unknown vm info>" ) + ")" );

            return 1;
        }

        boolean debug = commandLine.hasOption( CLIManager.DEBUG );
        boolean quiet = !debug && commandLine.hasOption( CLIManager.QUIET );

        boolean showErrors = debug || commandLine.hasOption( CLIManager.ERRORS );

        if ( showErrors )
        {
            System.out.println( "+ Error stacktraces are turned on." );
        }

        // ----------------------------------------------------------------------
        // Process particular command line options
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.HELP ) )
        {
            cliManager.displayHelp();
            return 0;
        }

        if ( commandLine.hasOption( CLIManager.VERSION ) )
        {
            showVersion();

            return 0;
        }
        else if ( debug )
        {
            showVersion();
        }

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        //** use CLI option values directly in request where possible

        MavenEmbedder mavenEmbedder;

        try
        {
            mavenEmbedder = new MavenEmbedder( classWorld );            
        }
        catch ( MavenEmbedderException e )
        {
            showFatalError( "Unable to start the embedded plexus container", e, showErrors );

            return 1;
        }

        boolean interactive = true;

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            interactive = false;
        }

        // This is now off by default and should just be removed as it causes too many problems and
        // is turned off in 2.0.4.
        boolean usePluginRegistry = false;

        if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_REGISTRY ) )
        {
            usePluginRegistry = false;
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

            String reactorFailureBehaviour = null;

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
                String profilesLine = commandLine.getOptionValue( CLIManager.ACTIVATE_PROFILES );

                StringTokenizer profileTokens = new StringTokenizer( profilesLine, "," );

                while ( profileTokens.hasMoreTokens() )
                {
                    String profileAction = profileTokens.nextToken().trim();

                    if ( profileAction.startsWith( "-" ) )
                    {
                        activeProfiles.add( profileAction.substring( 1 ) );
                    }
                    else if ( profileAction.startsWith( "+" ) )
                    {
                        inactiveProfiles.add( profileAction.substring( 1 ) );
                    }
                    else
                    {
                        // TODO: deprecate this eventually!
                        activeProfiles.add( profileAction );
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

            Properties executionProperties = getExecutionProperties( commandLine );

            MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setBaseDirectory( baseDirectory )
                .setGoals( goals )
                .setProperties( executionProperties ) // optional
                .setReactorFailureBehavior( reactorFailureBehaviour ) // default: fail fast
                .setRecursive( recursive ) // default: true
                .setUseReactor( useReactor ) // default: false
                .setPomFile( alternatePomFile ) // optional
                .setShowErrors( showErrors ) // default: false
                    // Settings
                .setSettingsFile( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) )
                    //.setLocalRepositoryPath( localRepositoryPath ) // default: ~/.m2/repository
                .setInteractiveMode( interactive ) // default: false
                .setUsePluginRegistry( usePluginRegistry )
                .setOffline( offline ) // default: false
                .setUsePluginUpdateOverride( pluginUpdateOverride )
                .addActiveProfiles( activeProfiles ) // optional
                .addInactiveProfiles( inactiveProfiles ) // optional
                    //
                .setLoggingLevel( loggingLevel ) // default: info
                .setTransferListener( transferListener ) // default: batch mode which goes along with interactive
                .setUpdateSnapshots( updateSnapshots ) // default: false
                .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
                .setGlobalChecksumPolicy( globalChecksumPolicy ); // default: warn

        MavenExecutionResult result = mavenEmbedder.execute( request );

        if ( result.hasExceptions() )
        {                        
            showFatalError( "Unable to configure the Maven application", (Exception) result.getExceptions().get( 0 ), showErrors );

            return 1;
        }

        return 0;
    }

    private static void showFatalError( String message,
                                        Exception e,
                                        boolean show )
    {
        System.err.println( "FATAL ERROR: " + message );
        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
        else
        {
            System.err.println( "For more information, run with the -e flag" );
        }
    }

    private static void showError( String message,
                                   Exception e,
                                   boolean show )
    {
        System.err.println( message );
        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
    }

    private static void showVersion()
    {
        InputStream resourceAsStream;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = MavenCli.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/org.apache.maven/maven-core/pom.properties" );
            properties.load( resourceAsStream );

            if ( properties.getProperty( "builtOn" ) != null )
            {
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" ) + " built on " +
                    properties.getProperty( "builtOn" ) );
            }
            else
            {
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" ) );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    private static Properties getExecutionProperties( CommandLine commandLine )
    {
        Properties executionProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY );

            for ( int i = 0; i < defStrs.length; ++i )
            {
                setCliProperty( defStrs[i], executionProperties );
            }
        }

        executionProperties.putAll( System.getProperties() );

        return executionProperties;
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
