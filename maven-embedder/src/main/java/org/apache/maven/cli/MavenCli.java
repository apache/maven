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
import org.apache.commons.cli.ParseException;
import org.apache.maven.MavenTransferListener;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderFileLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * @author jason van zyl
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    public static final String LOCAL_REPO_PROPERTY = "maven.repo.local";

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String OS_ARCH = System.getProperty( "os.arch" ).toLowerCase( Locale.US );

    public static final String OS_VERSION = System.getProperty( "os.version" ).toLowerCase( Locale.US );

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
        MavenCli cli = new MavenCli();

        return cli.doMain( args, classWorld );
    }

    public int doMain( String[] args,
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

        File userSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) );
        }
        else
        {
            userSettingsFile =  MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
        }

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( userSettingsFile )
            .setGlobalSettingsFile( MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE )
            .setClassWorld( classWorld );

        if ( commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            File logFile = new File(
                baseDirectory,
                commandLine.getOptionValue( CLIManager.LOG_FILE ) );

            configuration.setMavenEmbedderLogger( new MavenEmbedderFileLogger( logFile ) );
        }
        else
        {
            configuration.setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );
        }

        ConfigurationValidationResult cvr = MavenEmbedder.validateConfiguration( configuration );

        if ( cvr.isUserSettingsFilePresent() && !cvr.isUserSettingsFileParses() )
        {
            showError( "Error reading user settings: ", cvr.getUserSettingsException(), showErrors );

            return 1;
        }

        if ( cvr.isGlobalSettingsFilePresent() && !cvr.isGlobalSettingsFileParses() )
        {
            showError( "Error reading global settings: ", cvr.getGlobalSettingsException(), showErrors );

            return 1;
        }

        String localRepoProperty = executionProperties.getProperty( LOCAL_REPO_PROPERTY );

        if ( localRepoProperty != null )
        {
            configuration.setLocalRepository( new File( localRepoProperty ) );
        }

        MavenEmbedder mavenEmbedder;

        try
        {
            mavenEmbedder = new MavenEmbedder( configuration );

            logger = mavenEmbedder.getLogger();
        }
        catch ( MavenEmbedderException e )
        {
            showError( "Unable to start the embedder: ", e, showErrors );

            return 1;
        }

        MavenExecutionResult result = mavenEmbedder.execute( request );

        logResult( request, result );

        if ( result.hasExceptions() )
        {
            return 1;
        }
        else
        {
            return 0;
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

            System.out.println( "Java version: " + System.getProperty( "java.version", "<unknown java version>" ) );

            //TODO: when plexus can return the family type, add that here because it will make it easier to know what profile activation settings to use.
            System.out.println( "OS name: \"" + OS_NAME + "\" version: \"" + OS_VERSION + "\" arch: \"" + OS_ARCH + "\"" );
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

    // ----------------------------------------------------------------------
    // Reporting / Logging
    // ----------------------------------------------------------------------

    private static final long MB = 1024 * 1024;

    private static final int MS_PER_SEC = 1000;

    private static final int SEC_PER_MIN = 60;

    private MavenEmbedderLogger logger;

    private MavenEmbedderLogger getLogger()
    {
        return logger;
    }

    private void logResult( MavenExecutionRequest request, MavenExecutionResult result )
    {
        ReactorManager reactorManager = result.getReactorManager();

        logReactorSummary( reactorManager );

        if ( reactorManager != null && reactorManager.hasBuildFailures() )
        {
            logErrors(
                reactorManager,
                request.isShowErrors() );

            if ( !ReactorManager.FAIL_NEVER.equals( reactorManager.getFailureBehavior() ) )
            {
                getLogger().info( "BUILD ERRORS" );

                line();

                stats( request.getStartTime() );

                line();
            }
            else
            {
                getLogger().info( " + Ignoring failures" );
            }
        }

        if ( result.hasExceptions() )
        {
            for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
            {
                Exception e = (Exception) i.next();

                showError( e.getMessage(), e, request.isShowErrors() );
            }
        }
        else
        {
            line();

            getLogger().info( "BUILD SUCCESSFUL" );

            line();

            stats( request.getStartTime() );

            line();
        }

        logger.close();
    }

    private void logErrors( ReactorManager rm,
                            boolean showErrors )
    {
        for ( Iterator it = rm.getSortedProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( rm.hasBuildFailure( project ) )
            {
                BuildFailure buildFailure = rm.getBuildFailure( project );

                getLogger().info(
                    "Error for project: " + project.getName() + " (during " + buildFailure.getTask() + ")" );

                line();
            }
        }

        if ( !showErrors )
        {
            getLogger().info( "For more information, run Maven with the -e switch" );

            line();
        }
    }

    private static void showError( String message,
                                   Exception e,
                                   boolean show )
    {
        System.err.println();
        System.err.println( message );
        System.err.println();

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

    private void logReactorSummary( ReactorManager rm )
    {
        if ( rm != null && rm.hasMultipleProjects() && rm.executedMultipleProjects() )
        {
            getLogger().info( "" );
            getLogger().info( "" );

            // -------------------------
            // Reactor Summary:
            // -------------------------
            // o project-name...........FAILED
            // o project2-name..........SKIPPED (dependency build failed or was skipped)
            // o project-3-name.........SUCCESS

            line();
            getLogger().info( "Reactor Summary:" );
            line();

            for ( Iterator it = rm.getSortedProjects().iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                if ( rm.hasBuildFailure( project ) )
                {
                    logReactorSummaryLine(
                        project.getName(),
                        "FAILED",
                        rm.getBuildFailure( project ).getTime() );
                }
                else if ( rm.isBlackListed( project ) )
                {
                    logReactorSummaryLine(
                        project.getName(),
                        "SKIPPED (dependency build failed or was skipped)" );
                }
                else if ( rm.hasBuildSuccess( project ) )
                {
                    logReactorSummaryLine(
                        project.getName(),
                        "SUCCESS",
                        rm.getBuildSuccess( project ).getTime() );
                }
                else
                {
                    logReactorSummaryLine(
                        project.getName(),
                        "NOT BUILT" );
                }
            }
            line();
        }
    }

    private void stats( Date start )
    {
        Date finish = new Date();

        long time = finish.getTime() - start.getTime();

        getLogger().info( "Total time: " + formatTime( time ) );

        getLogger().info( "Finished at: " + finish );

        //noinspection CallToSystemGC
        System.gc();

        Runtime r = Runtime.getRuntime();

        getLogger().info(
            "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / MB + "M/" + r.totalMemory() / MB + "M" );
    }

    private void line()
    {
        getLogger().info( "------------------------------------------------------------------------" );
    }

    private static String formatTime( long ms )
    {
        long secs = ms / MS_PER_SEC;

        long min = secs / SEC_PER_MIN;

        secs = secs % SEC_PER_MIN;

        String msg = "";

        if ( min > 1 )
        {
            msg = min + " minutes ";
        }
        else if ( min == 1 )
        {
            msg = "1 minute ";
        }

        if ( secs > 1 )
        {
            msg += secs + " seconds";
        }
        else if ( secs == 1 )
        {
            msg += "1 second";
        }
        else if ( min == 0 )
        {
            msg += "< 1 second";
        }
        return msg;
    }

    private void logReactorSummaryLine( String name,
                                        String status )
    {
        logReactorSummaryLine(
            name,
            status,
            -1 );
    }

    private void logReactorSummaryLine( String name,
                                        String status,
                                        long time )
    {
        StringBuffer messageBuffer = new StringBuffer();

        messageBuffer.append( name );

        int dotCount = 54;

        dotCount -= name.length();

        messageBuffer.append( " " );

        for ( int i = 0; i < dotCount; i++ )
        {
            messageBuffer.append( '.' );
        }

        messageBuffer.append( " " );

        messageBuffer.append( status );

        if ( time >= 0 )
        {
            messageBuffer.append( " [" );

            messageBuffer.append( getFormattedTime( time ) );

            messageBuffer.append( "]" );
        }

        getLogger().info( messageBuffer.toString() );
    }

    private static String getFormattedTime( long time )
    {
        String pattern = "s.SSS's'";
        if ( time / 60000L > 0 )
        {
            pattern = "m:s" + pattern;
            if ( time / 3600000L > 0 )
            {
                pattern = "H:m" + pattern;
            }
        }
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.format( new Date( time ) );
    }
}
