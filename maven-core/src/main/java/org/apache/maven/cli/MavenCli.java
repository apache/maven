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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.ReactorManager;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenCli
{
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

        if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
        {
            System.setProperty( ProfileActivationUtils.ACTIVE_PROFILE_IDS,
                                commandLine.getOptionValue( CLIManager.ACTIVATE_PROFILES ) );
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
        
        String userSettingsPath = null;
        
        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsPath = commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS );
        }

        Settings settings = null;
        try
        {
            MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );
            
            if ( userSettingsPath != null )
            {
                File userSettingsFile = new File( userSettingsPath );
                
                if ( userSettingsFile.exists() && !userSettingsFile.isDirectory() )
                {
                    settings = settingsBuilder.buildSettings( userSettingsFile );
                }
                else
                {
                    System.out.println("WARNING: Alternate user settings file: " + userSettingsPath + " is invalid. Using default path." );
                }
            }

            if ( settings == null )
            {
                settings = settingsBuilder.buildSettings();
            }
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

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES ) ||
            commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            settings.getRuntimeInfo().setPluginUpdateOverride( Boolean.TRUE );
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            settings.getRuntimeInfo().setPluginUpdateOverride( Boolean.FALSE );
        }

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_LATEST_CHECK ) )
        {
            settings.getRuntimeInfo().setCheckLatestPluginVersion( Boolean.TRUE );
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_LATEST_CHECK ) )
        {
            settings.getRuntimeInfo().setCheckLatestPluginVersion( Boolean.FALSE );
        }

        if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_REGISTRY ) )
        {
            settings.setUsePluginRegistry( false );
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

            request = createRequest( embedder, commandLine, settings, eventDispatcher, manager );
            
            setProjectFileOptions( commandLine, request );

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

    private static MavenExecutionRequest createRequest( Embedder embedder, CommandLine commandLine,
                                                        Settings settings, EventDispatcher eventDispatcher,
                                                        LoggerManager manager )
        throws ComponentLookupException
    {
        MavenExecutionRequest request = null;

        ArtifactRepository localRepository = createLocalRepository( embedder, settings, commandLine );

        request = new DefaultMavenExecutionRequest( localRepository, settings, eventDispatcher,
                                                    commandLine.getArgList(), userDir.getPath() );

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
        
        if ( commandLine.hasOption( CLIManager.FAIL_FAST ) )
        {
            request.setFailureBehavior( ReactorManager.FAIL_FAST );
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_AT_END ) )
        {
            request.setFailureBehavior( ReactorManager.FAIL_AT_END );
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_NEVER ) )
        {
            request.setFailureBehavior( ReactorManager.FAIL_NEVER );
        }
        
        return request;
    }

    private static void setProjectFileOptions( CommandLine commandLine, MavenExecutionRequest request )
    {
        if ( commandLine.hasOption( CLIManager.REACTOR ) )
        {
            request.setReactorActive( true );
        }
        else if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
        {
            request.setPomFile( commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE ) );
        }
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

            artifactRepositoryFactory.setGlobalEnable( false );
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            System.out.println( "+ Enabling strict checksum verification on all artifact downloads." );

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL );
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            System.out.println( "+ Disabling strict checksum verification on all artifact downloads." );

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
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
        public static final char ALTERNATE_POM_FILE = 'f';
        
        public static final char BATCH_MODE = 'B';

        public static final char SET_SYSTEM_PROPERTY = 'D';

        public static final char OFFLINE = 'o';

        public static final char REACTOR = 'r';

        public static final char DEBUG = 'X';

        public static final char ERRORS = 'e';

        public static final char HELP = 'h';

        public static final char VERSION = 'v';

        private Options options = null;

        public static final char NON_RECURSIVE = 'N';

        public static final char UPDATE_SNAPSHOTS = 'U';

        public static final char ACTIVATE_PROFILES = 'P';

        public static final String FORCE_PLUGIN_UPDATES = "cpu";

        public static final String FORCE_PLUGIN_UPDATES2 = "up";

        public static final String SUPPRESS_PLUGIN_UPDATES = "npu";

        public static final String SUPPRESS_PLUGIN_REGISTRY = "npr";

        public static final String FORCE_PLUGIN_LATEST_CHECK = "cpl";

        public static final String SUPPRESS_PLUGIN_LATEST_CHECK = "npl";

        public static final char CHECKSUM_FAILURE_POLICY = 'C';

        public static final char CHECKSUM_WARNING_POLICY = 'c';

        private static final char ALTERNATE_USER_SETTINGS = 's';

        private static final String FAIL_FAST = "ff";

        private static final String FAIL_AT_END = "fae";

        private static final String FAIL_NEVER = "fn";

        public CLIManager()
        {
            options = new Options();
            
            options.addOption( OptionBuilder.withLongOpt( "file").hasArg().withDescription( "Force the use of an alternate POM file." ).create( ALTERNATE_POM_FILE ) );
            
            options.addOption(
                OptionBuilder.withLongOpt( "define" ).hasArg().withDescription( "Define a system property" ).create(
                    SET_SYSTEM_PROPERTY ) );
            options.addOption(
                OptionBuilder.withLongOpt( "offline" ).withDescription( "Work offline" ).create( OFFLINE ) );
//            options.addOption( OptionBuilder.withLongOpt( "mojoDescriptors" ).withDescription(
//                "Display available mojoDescriptors" ).create( LIST_GOALS ) );
            options.addOption(
                OptionBuilder.withLongOpt( "help" ).withDescription( "Display help information" ).create( HELP ) );
            options.addOption(
                OptionBuilder.withLongOpt( "version" ).withDescription( "Display version information" ).create(
                    VERSION ) );
            options.addOption(
                OptionBuilder.withLongOpt( "debug" ).withDescription( "Produce execution debug output" ).create(
                    DEBUG ) );
            options.addOption(
                OptionBuilder.withLongOpt( "errors" ).withDescription( "Produce execution error messages" ).create(
                    ERRORS ) );
            options.addOption( OptionBuilder.withLongOpt( "reactor" ).withDescription(
                "Execute goals for project found in the reactor" ).create( REACTOR ) );
            options.addOption( OptionBuilder.withLongOpt( "non-recursive" ).withDescription(
                "Do not recurse into sub-projects" ).create( NON_RECURSIVE ) );
            options.addOption( OptionBuilder.withLongOpt( "update-snapshots" ).withDescription(
                "Update all snapshots regardless of repository policies" ).create( UPDATE_SNAPSHOTS ) );
            options.addOption( OptionBuilder.withLongOpt( "activate-profiles" ).withDescription(
                "Comma-delimited list of profiles to activate" ).hasArg().create( ACTIVATE_PROFILES ) );

            options.addOption( OptionBuilder.withLongOpt( "batch-mode" ).withDescription(
                "Run in non-interactive (batch) mode" ).create( BATCH_MODE ) );

            options.addOption( OptionBuilder.withLongOpt( "check-plugin-updates" ).withDescription(
                "Force upToDate check for any relevant registered plugins" ).create( FORCE_PLUGIN_UPDATES ) );
            options.addOption( OptionBuilder.withLongOpt( "update-plugins" ).withDescription(
                "Synonym for " + FORCE_PLUGIN_UPDATES ).create( FORCE_PLUGIN_UPDATES2 ) );
            options.addOption( OptionBuilder.withLongOpt( "no-plugin-updates" ).withDescription(
                "Suppress upToDate check for any relevant registered plugins" ).create( SUPPRESS_PLUGIN_UPDATES ) );
            options.addOption( OptionBuilder.withLongOpt( "check-plugin-latest" ).withDescription(
                "Force checking of LATEST metadata for plugin versions" ).create( FORCE_PLUGIN_LATEST_CHECK ) );
            options.addOption( OptionBuilder.withLongOpt( "no-plugin-latest" ).withDescription(
                "Suppress checking of LATEST metadata for plugin versions" ).create( SUPPRESS_PLUGIN_LATEST_CHECK ) );

            options.addOption( OptionBuilder.withLongOpt( "no-plugin-registry" ).withDescription(
                "Don't use ~/.m2/plugin-registry.xml for plugin versions" ).create( SUPPRESS_PLUGIN_REGISTRY ) );

            options.addOption( OptionBuilder.withLongOpt( "strict-checksums" ).withDescription(
                "Fail the build if checksums don't match" ).create( CHECKSUM_FAILURE_POLICY ) );
            options.addOption(
                OptionBuilder.withLongOpt( "lax-checksums" ).withDescription( "Warn if checksums don't match" ).create(
                    CHECKSUM_WARNING_POLICY ) );
            
            options.addOption( OptionBuilder.withLongOpt( "settings" )
                .withDescription( "Alternate path for the user settings file" ).hasArg()
                .create( ALTERNATE_USER_SETTINGS ) );
            
            options.addOption( OptionBuilder.withLongOpt( "fail-fast" ).withDescription( "Stop at first failure in reactorized builds" ).create( FAIL_FAST ) );
            
            options.addOption( OptionBuilder.withLongOpt( "fail-at-end" ).withDescription( "Only fail the build afterwards; allow all non-impacted builds to continue" ).create( FAIL_AT_END ) );
            
            options.addOption( OptionBuilder.withLongOpt( "fail-never" ).withDescription( "NEVER fail the build, regardless of project result" ).create( FAIL_NEVER ) );
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
