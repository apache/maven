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
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.maven.Maven;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.profiles.activation.ProfileActivationUtils;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenCli
{
    public static final String POMv4 = "pom.xml";

    public static File userDir = new File( System.getProperty( "user.dir" ) );

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

        // ----------------------------------------------------------------------
        //
        // 1) maven user configuration directory ( ~/.m2 )
        // 2) maven home
        // 3) local repository
        //
        // ----------------------------------------------------------------------

        //        File userConfigurationDirectory = getUserConfigurationDirectory();

        //        Properties mavenProperties = getMavenProperties( userConfigurationDirectory );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        initializeSystemProperties( commandLine );

        if( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
        {
            System.setProperty(ProfileActivationUtils.ACTIVE_PROFILE_IDS, commandLine.getOptionValue( CLIManager.ACTIVATE_PROFILES ) );
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

        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        Embedder embedder = new Embedder();

        try
        {
            embedder.start( classWorld );
        }
        catch ( PlexusContainerException e )
        {
            showFatalError( "Unable to start the embedded plexus container", e, showErrors );
            return 1;
        }

        Settings settings;
        try
        {
            MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );

            settings = settingsBuilder.buildSettings();
        }
        catch ( IOException e )
        {
            showFatalError( "Unable to read settings.xml", e, showErrors );
            return 1;
        }
        catch ( XmlPullParserException e )
        {
            showFatalError( "Unable to read settings.xml", e, showErrors );
            return 1;
        }
        catch ( ComponentLookupException e )
        {
            showFatalError( "Unable to read settings.xml", e, showErrors );
            return 1;
        }

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            settings.setInteractiveMode( false );
        }

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES ) )
        {
            settings.getRuntimeInfo().setPluginUpdateOverride( Boolean.TRUE );
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            settings.getRuntimeInfo().setPluginUpdateOverride( Boolean.FALSE );
        }

        List projectFiles = null;
        try
        {
            projectFiles = getProjectFiles( commandLine );
        }
        catch ( IOException e )
        {
            showFatalError( "Error locating project files for reactor execution", e, showErrors );
            return 1;
        }

        Maven maven = null;
        MavenExecutionRequest request = null;
        LoggerManager manager = null;
        try
        {
            // logger must be created first
            manager = (LoggerManager) embedder.lookup( LoggerManager.ROLE );
            if ( debug )
            {
                manager.setThreshold( Logger.LEVEL_DEBUG );
            }

            request = createRequest( projectFiles, embedder, commandLine, settings, eventDispatcher, manager );

            maven = createMavenInstance( embedder, settings.isInteractiveMode() );
        }
        catch ( ComponentLookupException e )
        {
            showFatalError( "Unable to configure the Maven application", e, showErrors );
            return 1;
        }
        finally
        {
            if ( manager != null )
            {
                try
                {
                    embedder.release( manager );
                }
                catch ( ComponentLifecycleException e )
                {
                    showFatalError( "Error releasing logging manager", e, showErrors );
                    return 1;
                }
            }
        }

        // TODO: this should be in default maven, and should accommodate default goals
        if ( request.getGoals().isEmpty() )
        {
            System.err.println( "You must specify at least one goal. Try 'install'" );

            cliManager.displayHelp();
            return 1;
        }

        MavenExecutionResponse response = null;
        try
        {
            response = maven.execute( request );
        }
        catch ( ReactorException e )
        {
            showFatalError( "Error executing Maven for a project", e, showErrors );
            return 1;
        }

        if ( response != null && response.isExecutionFailure() )
        {
            return 1;
        }
        else
        {
            return 0;
        }
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

    private static MavenExecutionRequest createRequest( List files, Embedder embedder,
                                                        CommandLine commandLine, Settings settings,
                                                        EventDispatcher eventDispatcher, LoggerManager manager )
        throws ComponentLookupException
    {
        MavenExecutionRequest request = null;

        ArtifactRepository localRepository = createLocalRepository( embedder, settings, commandLine );

        request = new DefaultMavenExecutionRequest( localRepository, settings, eventDispatcher,
                                                    commandLine.getArgList(), files, userDir.getPath() );

        // TODO [BP]: do we set one per mojo? where to do it?
        Logger logger = manager.getLoggerForComponent( Mojo.ROLE );
        if ( logger != null )
        {
            request.addEventMonitor( new DefaultEventMonitor( logger ) );
        }

        if ( commandLine.hasOption( CLIManager.NON_RECURSIVE ) )
        {
            request.setRecursive( false );
        }
        return request;
    }

    private static List getProjectFiles( CommandLine commandLine )
        throws IOException
    {
        List files = Collections.EMPTY_LIST;
        if ( commandLine.hasOption( CLIManager.REACTOR ) )
        {
            // TODO: should we now include the pom.xml in the current directory?
            String includes = System.getProperty( "maven.reactor.includes", "**/" + POMv4 );

            String excludes = System.getProperty( "maven.reactor.excludes", POMv4 );

            files = FileUtils.getFiles( userDir, includes, excludes );

            // make sure there is consistent ordering on all platforms, rather than using the filesystem ordering
            Collections.sort( files );
        }
        else
        {
            File projectFile = new File( userDir, POMv4 );

            if ( projectFile.exists() )
            {
                files = Collections.singletonList( projectFile );
            }
        }
        return files;
    }

    private static Maven createMavenInstance( Embedder embedder, boolean interactive )
        throws ComponentLookupException
    {
        // TODO [BP]: doing this here as it is CLI specific, though it doesn't feel like the right place (likewise logger).
        WagonManager wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
        if ( interactive )
        {
            wagonManager.setDownloadMonitor( new ConsoleDownloadMonitor() );
        }
        else
        {
            wagonManager.setDownloadMonitor( new BatchModeDownloadMonitor() );
        }

        return (Maven) embedder.lookup( Maven.ROLE );
    }

    private static ArtifactRepository createLocalRepository( Embedder embedder, Settings settings,
                                                             CommandLine commandLine )
        throws ComponentLookupException
    {
        // TODO: release
        // TODO: something in plexus to show all active hooks?
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) embedder.lookup(
            ArtifactRepositoryLayout.ROLE, "default" );

        ArtifactRepositoryFactory artifactRepositoryFactory = (ArtifactRepositoryFactory) embedder.lookup(
            ArtifactRepositoryFactory.ROLE );

        String url = "file://" + settings.getLocalRepository();
        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;
        if ( commandLine.hasOption( CLIManager.OFFLINE ) )
        {
            settings.setOffline( true );

            // TODO: this will still check to download if the artifact does not exist locally, instead of failing as it should in offline mode
            artifactRepositoryFactory.setGlobalSnapshotPolicy( ArtifactRepository.SNAPSHOT_POLICY_NEVER );
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            artifactRepositoryFactory.setGlobalSnapshotPolicy( ArtifactRepository.SNAPSHOT_POLICY_ALWAYS );
        }

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            System.out.println( "+ Enabling strict checksum verification on all artifact downloads.");

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepository.CHECKSUM_POLICY_FAIL );
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            System.out.println( "+ Disabling strict checksum verification on all artifact downloads.");

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepository.CHECKSUM_POLICY_WARN );
        }

        return localRepository;
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

            System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" ) );
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    private static void initializeSystemProperties( CommandLine commandLine )
    {
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
                setCliProperty( defStrs[i] );
            }
        }
    }

    private static void setCliProperty( String property )
    {
        String name = null;

        String value = null;

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

        System.setProperty( name, value );
    }

    // ----------------------------------------------------------------------
    // Command line manager
    // ----------------------------------------------------------------------

    static class CLIManager
    {
        public static final char BATCH_MODE = 'B';

        public static final char SET_SYSTEM_PROPERTY = 'D';

        public static final char OFFLINE = 'o';

        public static final char REACTOR = 'r';

        public static final char DEBUG = 'X';

        // TODO: [jc] Is there a better switch than '-e' for this?
        public static final char ERRORS = 'e';

        public static final char HELP = 'h';

        public static final char VERSION = 'v';

//        public static final char LIST_GOALS = 'g';

        private Options options = null;

        public static final char NON_RECURSIVE = 'N';

        public static final char UPDATE_SNAPSHOTS = 'U';

        public static final char ACTIVATE_PROFILES = 'P';

        public static final String FORCE_PLUGIN_UPDATES = "update-plugins";

        public static final String SUPPRESS_PLUGIN_UPDATES = "no-plugin-updates";

        public static final char CHECKSUM_FAILURE_POLICY = 'C';

        public static final char CHECKSUM_WARNING_POLICY = 'c';

        public CLIManager()
        {
            options = new Options();
            options.addOption( OptionBuilder.withLongOpt( "define" ).hasArg().withDescription(
                "Define a system property" ).create( SET_SYSTEM_PROPERTY ) );
            options.addOption( OptionBuilder.withLongOpt( "offline" ).withDescription( "Work offline" ).create(
                OFFLINE ) );
//            options.addOption( OptionBuilder.withLongOpt( "mojoDescriptors" ).withDescription(
//                "Display available mojoDescriptors" ).create( LIST_GOALS ) );
            options.addOption( OptionBuilder.withLongOpt( "help" ).withDescription( "Display help information" ).create(
                HELP ) );
            options.addOption( OptionBuilder.withLongOpt( "version" ).withDescription( "Display version information" ).create(
                VERSION ) );
            options.addOption( OptionBuilder.withLongOpt( "debug" ).withDescription( "Produce execution debug output" ).create(
                DEBUG ) );
            options.addOption( OptionBuilder.withLongOpt( "errors" ).withDescription(
                "Produce execution error messages" ).create( ERRORS ) );
            options.addOption( OptionBuilder.withLongOpt( "reactor" ).withDescription(
                "Execute goals for project found in the reactor" ).create( REACTOR ) );
            options.addOption( OptionBuilder.withLongOpt( "non-recursive" ).withDescription(
                "Do not recurse into sub-projects" ).create( NON_RECURSIVE ) );
            options.addOption( OptionBuilder.withLongOpt( "update-snapshots" ).withDescription(
                "Update all snapshots regardless of repository policies" ).create( UPDATE_SNAPSHOTS ) );
            options.addOption( OptionBuilder.withLongOpt( "activate-profiles" ).withDescription(
                "Comma-delimited list of profiles to activate").hasArg().create( ACTIVATE_PROFILES ) );
            options.addOption( OptionBuilder.withLongOpt( "batch-mode" ).withDescription( "Run in non-interactive (batch) mode" ).create( BATCH_MODE ) );
            options.addOption( OptionBuilder.withLongOpt( FORCE_PLUGIN_UPDATES ).withDescription( "Force upToDate check for any relevant registered plugins" ).create() );
            options.addOption( OptionBuilder.withLongOpt( SUPPRESS_PLUGIN_UPDATES ).withDescription( "Suppress upToDate check for any relevant registered plugins" ).create() );
            options.addOption( OptionBuilder.withLongOpt( "strict-checksums" ).withDescription( "Fail the build if checksums don't match" ).create( CHECKSUM_FAILURE_POLICY ) );
            options.addOption( OptionBuilder.withLongOpt( "lax-checksums" ).withDescription( "Warn if checksums don't match" ).create( CHECKSUM_WARNING_POLICY ) );
        }

        public CommandLine parse( String[] args )
            throws ParseException
        {
            CommandLineParser parser = new PosixParser();
            return parser.parse( options, args );
        }

        public void displayHelp()
        {
            System.out.println();

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "maven [options] [goal [goal2 [goal3] ...]]", "\nOptions:", options, "\n" );
        }
    }
}
