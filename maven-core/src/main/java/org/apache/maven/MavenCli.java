package org.apache.maven;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.embed.ArtifactEnabledEmbedder;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenCli
{
    public static final String POMv3 = "project.xml";

    public static final String POMv4 = "pom.xml";

    public static void main( String[] args, ClassWorld classWorld )
        throws Exception
    {
        CLIManager cliManager = new CLIManager();

        CommandLine commandLine = cliManager.parse( args );

        initializeSystemProperties( commandLine );

        //---

        ArtifactEnabledEmbedder embedder = new ArtifactEnabledEmbedder();       

        embedder.start( classWorld );

        Maven maven = (Maven) embedder.lookup( Maven.ROLE );

        maven.setMavenHome( findMavenHome() );

        maven.setLocalRepository( findLocalRepository() );

        maven.booty();

        //---

        File projectFile;

        projectFile = new File( System.getProperty( "user.dir" ), POMv4 );

        if ( !projectFile.exists() )
        {
            projectFile = new File( System.getProperty( "user.dir" ), POMv3 );
        }

        // ----------------------------------------------------------------------
        // Process particular command line options
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.HELP ) )
        {
            cliManager.displayHelp();

            return;
        }

        if ( commandLine.hasOption( CLIManager.VERSION ) )
        {
            // TODO: create some sane output.
            // Take this info from generated piece of meta data which uses
            // the POM itself as the source. We don't want to get into the same
            // bullshit of manually updating some constant in the source.
            System.out.println( "Maven version: " );

            return;
        }

        if ( commandLine.hasOption( CLIManager.LIST_GOALS ) )
        {
            Iterator goals = new TreeMap( maven.getMojoDescriptors() ).values().iterator();

            System.out.println( "Goals: " );

            while ( goals.hasNext() )
            {
                MojoDescriptor goal = (MojoDescriptor)goals.next();

                System.out.println( "    " + goal.getId() );
            }

            return;
        }

        ExecutionResponse response = null;

        // ----------------------------------------------------------------------
        // Execute the goals
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.REACTOR ) )
        {
            String includes = System.getProperty( "maven.reactor.includes", "**/" + POMv4 );

            String excludes = System.getProperty( "maven.reactor.excludes", POMv4 );

            String goals = "";

            for ( Iterator i = commandLine.getArgList().iterator(); i.hasNext(); )
            {
                goals += (String) i.next();

                if ( i.hasNext() )
                {
                    goals += ",";
                }
            }

            if ( !"".equals( goals ) )
            {
                response = maven.executeReactor( goals, includes, excludes );
            }
        }
        else
        {
            response = maven.execute( projectFile, commandLine.getArgList() );
        }
    }

    // ----------------------------------------------------------------------
    // Local repository
    // ----------------------------------------------------------------------

    /** @todo shouldn't need to duplicate the code to load maven.properties. */
    private static String findLocalRepository()
        throws Exception
    {
        Properties properties = new Properties();

        properties.load( new FileInputStream( new File( System.getProperty( "user.home" ), "maven.properties" ) ) );

        for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            properties.setProperty( key, StringUtils.interpolate( properties.getProperty( key ), System.getProperties() ) );
        }

        String localRepository = properties.getProperty( MavenConstants.MAVEN_REPO_LOCAL );

        if ( localRepository == null )
        {
            throw new Exception( "Missing 'maven.repo.local' from ~/maven.properties." );
        }

        return localRepository;
    }

    // ----------------------------------------------------------------------
    // Maven home
    // ----------------------------------------------------------------------

    private static String findMavenHome()
    {
        String mavenHome = System.getProperty( "maven.home" );

        return mavenHome;
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    private static void initializeSystemProperties( CommandLine commandLine )
    {
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.

        if ( commandLine.hasOption( CLIManager.DEBUG ) )
        {
            System.setProperty( MavenConstants.DEBUG_ON, "true" );
        }
        else
        {
            System.setProperty( MavenConstants.DEBUG_ON, "false" );
        }

        if ( commandLine.hasOption( CLIManager.WORK_OFFLINE ) )
        {
            System.setProperty( MavenConstants.WORK_OFFLINE, "true" );
        }
        else
        {
            System.setProperty( MavenConstants.WORK_OFFLINE, "false" );
        }

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

        public CommandLine parse( String[] args ) throws ParseException
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
}
