package org.apache.maven.cli;

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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.MavenTransferListener;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.Settings;
import org.codehaus.classworlds.ClassWorld;

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
    /**
     * @noinspection ConfusingMainMethod
     */
    public static int main( String[] args, ClassWorld classWorld )
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
        if ( System.getProperty( "java.class.version", "44.0" ).compareTo( "48.0" ) < 0 )
        {
            System.err.println( "Sorry, but JDK 1.4 or above is required to execute Maven" );
            System.err.println(
                "You appear to be using Java version: " + System.getProperty( "java.version", "<unknown>" ) );

            return 1;
        }

        boolean debug = commandLine.hasOption( CLIManager.DEBUG );

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

        MavenEmbedder mavenEmbedder = new MavenEmbedder();

        try
        {
            mavenEmbedder.setClassWorld( classWorld );

            mavenEmbedder.start();
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

        boolean usePluginRegistry = true;

        if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_REGISTRY ) )
        {
            usePluginRegistry = false;
        }

        Boolean pluginUpdateOverride = Boolean.FALSE;

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES ) ||
            commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            pluginUpdateOverride = Boolean.TRUE;
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            pluginUpdateOverride = Boolean.FALSE;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        try
        {
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

            boolean reactorActive = false;

            if ( commandLine.hasOption( CLIManager.REACTOR ) )
            {
                reactorActive = true;
            }

            String alternatePomFile = null;
            if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
            {
                alternatePomFile = commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE );
            }

            // ----------------------------------------------------------------------
            // From here we are CLI free
            // ----------------------------------------------------------------------

            //  -> baseDirectory
            //  -> goals
            //  -> debug: use to set the threshold on the logger manager
            //  -> active profiles (settings)
            //  -> inactive profiles (settings)
            //  -> offline (settings)
            //  -> interactive (settings)
            //  -> Settings
            //     -> localRepository
            //     -> interactiveMode
            //     -> usePluginRegistry
            //     -> offline
            //     -> proxies
            //     -> servers
            //     -> mirrors
            //     -> profiles
            //     -> activeProfiles
            //     -> pluginGroups
            //  -> executionProperties
            //  -> reactorFailureBehaviour: fail fast, fail at end, fail never
            //  -> globalChecksumPolicy: fail, warn
            //  -> showErrors (this is really CLI is but used inside Maven internals
            //  -> recursive
            //  -> updateSnapshots
            //  -> reactorActive
            //  -> transferListener: in the CLI this is batch or console

            // We have a general problem with plexus components that are singletons in that they use
            // the same logger for their lifespan. This is not good in that many requests may be fired
            // off and the singleton plexus component will continue to funnel their output to the same
            // logger. We need to be able to swap the logger.

            // the local repository should just be a path and we should look here:
            // in the system property
            // user specified settings.xml
            // default ~/.m2/settings.xml
            // and with that maven internals should contruct the ArtifactRepository object

            int loggingLevel;

            if ( debug )
            {
                loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_DEBUG;
            }
            else
            {
                loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_WARN;
            }

            Properties executionProperties = getExecutionProperties( commandLine );
            
            File userSettingsPath = mavenEmbedder.getUserSettingsPath( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) );

            File globalSettingsFile = mavenEmbedder.getGlobalSettingsPath();

            Settings settings = mavenEmbedder.buildSettings( userSettingsPath,
                                                             globalSettingsFile,
                                                             interactive,
                                                             offline,
                                                             usePluginRegistry,
                                                             pluginUpdateOverride );

            String localRepositoryPath = mavenEmbedder.getLocalRepositoryPath( settings );

            // @todo we either make Settings the official configuration mechanism or allow the indiviaul setting in the request
            // for each of the things in the settings object. Seems redundant to configure some things via settings and
            // some via the request. The Settings object is used in about 16 different places in the core so something
            // to consider.

            MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setBasedir( baseDirectory )
                .setGoals( goals )
                .setLocalRepositoryPath( localRepositoryPath )
                .setProperties( executionProperties )
                .setFailureBehavior( reactorFailureBehaviour )
                .setRecursive( recursive )
                .setReactorActive( reactorActive )
                .setPomFile( alternatePomFile )
                .setShowErrors( showErrors )
                .setInteractive( interactive )
                .addActiveProfiles( activeProfiles )
                .addInactiveProfiles( inactiveProfiles )
                .setLoggingLevel( loggingLevel )
                .activateDefaultEventMonitor()
                .setSettings( settings )
                .setTransferListener( transferListener )
                .setOffline( offline )
                .setUpdateSnapshots( updateSnapshots )
                .setGlobalChecksumPolicy( globalChecksumPolicy );

            mavenEmbedder.execute( request );
        }
        catch ( SettingsConfigurationException e )
        {
            showError( "Error reading settings.xml: " + e.getMessage(), e, showErrors );

            return 1;
        }
        catch ( MavenExecutionException e )
        {
            showFatalError( "Unable to configure the Maven application", e, showErrors );

            return 1;
        }

        return 0;
    }

    private static void showFatalError( String message, Exception e, boolean show )
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

    private static void showError( String message, Exception e, boolean show )
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
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" )
                    + " built on " + properties.getProperty( "builtOn" ) );
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

    private static void setCliProperty( String property, Properties executionProperties )
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
