package org.apache.maven.cli;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.maven.Maven;
import org.apache.maven.MavenConstants;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenInitializingExecutionRequest;
import org.apache.maven.execution.MavenProjectExecutionRequest;
import org.apache.maven.execution.MavenReactorExecutionRequest;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.Plugin;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.embed.ArtifactEnabledEmbedder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenCli
{
    public static final String POMv4 = "pom.xml";

    public static final String userHome = System.getProperty( "user.home" );

    public static File userDir = new File( System.getProperty( "user.dir" ) );

    public static int main( String[] args, ClassWorld classWorld )
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Setup the command line parser
        // ----------------------------------------------------------------------

        CLIManager cliManager = new CLIManager();

        CommandLine commandLine = cliManager.parse( args );

        // ----------------------------------------------------------------------
        //
        // 1) maven user configuration directory ( ~/.m2 )
        // 2) maven home
        // 3) local repository
        //
        // ----------------------------------------------------------------------

        File userConfigurationDirectory = getUserConfigurationDirectory();

        Properties mavenProperties = getMavenProperties( userConfigurationDirectory );

        ArtifactRepository localRepository = getLocalRepository( mavenProperties, userConfigurationDirectory );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        initializeSystemProperties( commandLine );

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
            System.out.println( "Maven version: " );
            return 0;
        }

        // ----------------------------------------------------------------------
        // We will ultimately not require a flag to indicate the reactor as
        // we should take this from the execution context i.e. what the type
        // is stated as in the POM.
        // ----------------------------------------------------------------------

        MavenExecutionRequest request = null;

        File projectFile = new File( userDir, POMv4 );
        
        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        if ( projectFile.exists() )
        {
            if ( commandLine.hasOption( CLIManager.REACTOR ) )
            {
                String includes = System.getProperty( "maven.reactor.includes", "**/" + POMv4 );

                String excludes = System.getProperty( "maven.reactor.excludes", POMv4 );

                request = new MavenReactorExecutionRequest( localRepository,
                                                            eventDispatcher,
                                                            mavenProperties,
                                                            commandLine.getArgList(),
                                                            includes,
                                                            excludes,
                                                            userDir );
            }
            else
            {
                request = new MavenProjectExecutionRequest( localRepository,
                                                            eventDispatcher,
                                                            mavenProperties,
                                                            commandLine.getArgList(),
                                                            projectFile );
            }
        }
        else
        {
            request = new MavenInitializingExecutionRequest( localRepository, eventDispatcher, mavenProperties, commandLine.getArgList() );
        }

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        ArtifactEnabledEmbedder embedder = new ArtifactEnabledEmbedder();

        embedder.start( classWorld );

        LoggerManager manager = (LoggerManager) embedder.lookup( LoggerManager.ROLE );
        if ( commandLine.hasOption( CLIManager.DEBUG ) )
        {
            manager.setThreshold( Logger.LEVEL_DEBUG );
        }

        // TODO [BP]: do we set one per mojo? where to do it?
        Logger logger = manager.getLoggerForComponent( Plugin.ROLE );
        if ( logger != null )
        {
            request.setLog( new DefaultLog( logger ) );
            
            request.addEventMonitor( new DefaultEventMonitor( logger ) );
        }
        
        // TODO [BP]: doing this here as it is CLI specific, though it doesn't feel like the right place (likewise logger).
        WagonManager wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
        wagonManager.setDownloadMonitor( new ConsoleDownloadMonitor() );

        Maven maven = (Maven) embedder.lookup( Maven.ROLE );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        MavenExecutionResponse response = maven.execute( request );

        if ( response != null && response.isExecutionFailure() )
        {
            return 1;
        }
        else
        {
            return 0;
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
        public static final char NO_BANNER = 'b';

        public static final char SET_SYSTEM_PROPERTY = 'D';

        public static final char WORK_OFFLINE = 'o';

        public static final char REACTOR = 'r';

        public static final char DEBUG = 'X';

        public static final char HELP = 'h';

        public static final char VERSION = 'v';

        public static final char LIST_GOALS = 'g';

        private Options options = null;

        public CLIManager()
        {
            options = new Options();
            options.addOption( OptionBuilder
                               .withLongOpt( "nobanner" )
                               .withDescription( "Suppress logo banner" )
                               .create( NO_BANNER ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "define" )
                               .hasArg()
                               .withDescription( "Define a system property" )
                               .create( SET_SYSTEM_PROPERTY ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "offline" )
                               .hasArg()
                               .withDescription( "Work offline" )
                               .create( WORK_OFFLINE ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "mojoDescriptors" )
                               .withDescription( "Display available mojoDescriptors" )
                               .create( LIST_GOALS ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "help" )
                               .withDescription( "Display help information" )
                               .create( HELP ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "offline" )
                               .withDescription( "Build is happening offline" )
                               .create( WORK_OFFLINE ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "version" )
                               .withDescription( "Display version information" )
                               .create( VERSION ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "debug" )
                               .withDescription( "Produce execution debug output" )
                               .create( DEBUG ) );
            options.addOption( OptionBuilder
                               .withLongOpt( "reactor" )
                               .withDescription( "Execute goals for project found in the reactor" )
                               .create( REACTOR ) );
        }

        public CommandLine parse( String[] args )
            throws ParseException
        {
            CommandLineParser parser = new PosixParser();
            return parser.parse( options, args );
        }

        public void displayHelp()
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "maven [options] [goal [goal2 [goal3] ...]]", "\nOptions:", options, "\n" );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected static File getUserConfigurationDirectory()
    {
        File mavenUserConfigurationDirectory = new File( userHome, MavenConstants.MAVEN_USER_CONFIGURATION_DIRECTORY );
        if ( !mavenUserConfigurationDirectory.exists() )
        {
            if ( !mavenUserConfigurationDirectory.mkdirs() )
            {
                //throw a configuration exception
            }
        }
        return mavenUserConfigurationDirectory;
    }

    protected static Properties getMavenProperties( File mavenHomeLocal )
    {
        Properties mavenProperties = new Properties();
        File mavenPropertiesFile = new File( mavenHomeLocal, MavenConstants.MAVEN_PROPERTIES );
        try
        {
            mavenProperties.load( new FileInputStream( mavenPropertiesFile ) );
        }
        catch ( IOException e )
        {
            // do nothing
        }
        return mavenProperties;
    }

    protected static ArtifactRepository getLocalRepository( Properties mavenProperties,
                                                            File userConfigurationDirectory )
    {
        String localRepository = mavenProperties.getProperty( MavenConstants.MAVEN_REPO_LOCAL );
        if ( localRepository == null )
        {
            localRepository = new File( userConfigurationDirectory, MavenConstants.MAVEN_REPOSITORY ).getAbsolutePath();
        }

        // TODO [BP]: this should not be necessary - grep for and remove
        System.setProperty( MavenConstants.MAVEN_REPO_LOCAL, localRepository );
        return new ArtifactRepository( "local", "file://" + localRepository );
    }
}
