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
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.ArtifactEnabledEmbedder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

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

        boolean debug = commandLine.hasOption( CLIManager.DEBUG );

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

        ArtifactEnabledEmbedder embedder = new ArtifactEnabledEmbedder();

        try
        {
            embedder.start( classWorld );
        }
        catch ( PlexusContainerException e )
        {
            showFatalError( "Unable to start the embedded plexus container", e, debug );
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
            showFatalError( "Unable to read settings.xml", e, debug );
            return 1;
        }
        catch ( XmlPullParserException e )
        {
            showFatalError( "Unable to read settings.xml", e, debug );
            return 1;
        }
        catch ( ComponentLookupException e )
        {
            showFatalError( "Unable to read settings.xml", e, debug );
            return 1;
        }

        List projectFiles = null;
        try
        {
            projectFiles = getProjectFiles( commandLine );
        }
        catch ( IOException e )
        {
            showFatalError( "Error locating project files for reactor execution", e, debug );
            return 1;
        }

        Maven maven = null;
        MavenExecutionRequest request = null;
        try
        {
            maven = createMavenInstance( embedder );

            request = createRequest( projectFiles, embedder, commandLine, settings, eventDispatcher, debug );
        }
        catch ( ComponentLookupException e )
        {
            showFatalError( "Unable to configure the Maven application", e, debug );
            return 1;
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
            showFatalError( "Error executing Maven for a project", e, debug );
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

    private static void showFatalError( String message, Exception e, boolean debug )
    {
        System.err.println( "FATAL ERROR: " + message );
        if ( debug )
        {
            e.printStackTrace();
        }
        else
        {
            System.err.println( "For more information, run with the -X flag" );
        }
    }

    private static MavenExecutionRequest createRequest( List files, ArtifactEnabledEmbedder embedder,
                                                        CommandLine commandLine, Settings settings,
                                                        EventDispatcher eventDispatcher, boolean debug )
        throws ComponentLookupException
    {
        MavenExecutionRequest request = null;

        ArtifactRepository localRepository = createLocalRepository( embedder, settings, commandLine );

        request = new DefaultMavenExecutionRequest( localRepository, settings, eventDispatcher,
                                                    commandLine.getArgList(), files, userDir.getPath() );

        LoggerManager manager = (LoggerManager) embedder.lookup( LoggerManager.ROLE );
        if ( debug )
        {
            manager.setThreshold( Logger.LEVEL_DEBUG );
        }

        // TODO [BP]: do we set one per mojo? where to do it?
        Logger logger = manager.getLoggerForComponent( Mojo.ROLE );
        if ( logger != null )
        {
            request.setLog( new DefaultLog( logger ) );

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

    private static Maven createMavenInstance( ArtifactEnabledEmbedder embedder )
        throws ComponentLookupException
    {
        // TODO [BP]: doing this here as it is CLI specific, though it doesn't feel like the right place (likewise logger).
        WagonManager wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
        wagonManager.setDownloadMonitor( new ConsoleDownloadMonitor() );

        return (Maven) embedder.lookup( Maven.ROLE );
    }

    private static ArtifactRepository createLocalRepository( ArtifactEnabledEmbedder embedder, Settings settings,
                                                             CommandLine commandLine )
        throws ComponentLookupException
    {
        // TODO: release
        // TODO: something in plexus to show all active hooks?
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) embedder.lookup(
            ArtifactRepositoryLayout.ROLE, "default" );

        ArtifactRepositoryFactory artifactRepositoryFactory = (ArtifactRepositoryFactory) embedder.lookup(
            ArtifactRepositoryFactory.ROLE );

        String url = "file://" + settings.getActiveProfile().getLocalRepository();
        ArtifactRepository localRepository = new ArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;
        if ( commandLine.hasOption( CLIManager.OFFLINE ) )
        {
            settings.getActiveProfile().setOffline( true );

            // TODO: this will still check to download if the artifact does not exist locally, instead of failing as it should in offline mode
            artifactRepositoryFactory.setGlobalSnapshotPolicy( ArtifactRepository.SNAPSHOT_POLICY_NEVER );
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            artifactRepositoryFactory.setGlobalSnapshotPolicy( ArtifactRepository.SNAPSHOT_POLICY_ALWAYS );
        }
        return localRepository;
    }

    private static void showVersion()
    {
        // TODO: is there a beter way? Maybe read the manifest?

        String version = "unknown";

        try
        {
            for ( Enumeration e = MavenCli.class.getClassLoader().getResources( "/META-INF/maven/pom.xml" );
                  e.hasMoreElements(); )
            {
                URL resource = (URL) e.nextElement();
                if ( resource.getPath().indexOf( "maven-core" ) >= 0 )
                {
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = reader.read( new InputStreamReader( resource.openStream() ) );
                    version = model.getVersion();
                    break;
                }
            }

            System.out.println( "Maven version: " + version );
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
        catch ( XmlPullParserException e )
        {
            System.err.println( "Unable to parse POM in JAR file: " + e.getMessage() );
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
        public static final char SET_SYSTEM_PROPERTY = 'D';

        public static final char OFFLINE = 'o';

        public static final char REACTOR = 'r';

        public static final char DEBUG = 'X';

        public static final char HELP = 'h';

        public static final char VERSION = 'v';

//        public static final char LIST_GOALS = 'g';

        private Options options = null;

        public static final char NON_RECURSIVE = 'N';

        public static final char UPDATE_SNAPSHOTS = 'U';

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
            options.addOption( OptionBuilder.withLongOpt( "reactor" ).withDescription(
                "Execute goals for project found in the reactor" ).create( REACTOR ) );
            options.addOption( OptionBuilder.withLongOpt( "non-recursive" ).withDescription(
                "Do not recurse into sub-projects" ).create( NON_RECURSIVE ) );
            options.addOption( OptionBuilder.withLongOpt( "update-snapshots" ).withDescription(
                "Update all snapshots regardless of repository policies" ).create( UPDATE_SNAPSHOTS ) );
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