package org.apache.maven.cli;

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

import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.Maven;
import org.apache.maven.ExecutionResponse;

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.embed.ArtifactEnabledEmbedder;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenCli
{
    public static final String POMv3 = "project.xml";

    public static final String POMv4 = "pom.xml";

    public static int main( String[] args, ClassWorld classWorld )
        throws Exception
    {
        CLIManager cliManager = new CLIManager();

        CommandLine commandLine = cliManager.parse( args );

        initializeSystemProperties( commandLine );

        //---

        ArtifactEnabledEmbedder embedder = new ArtifactEnabledEmbedder();       

        embedder.start( classWorld );

        Maven maven = (Maven) embedder.lookup( Maven.ROLE );

        maven.setMavenHome( new File( System.getProperty( "maven.home" ) ) );

        maven.setMavenHomeLocal( new File( System.getProperty( "maven.home.local", System.getProperty( "user.home" ) + "/.m2" ) ) );

        //---

        File projectFile;

        projectFile = new File( System.getProperty( "user.dir" ), POMv4 );

        if ( !projectFile.exists() )
        {
            projectFile = new File( System.getProperty( "user.dir" ), POMv3 );

            if ( !projectFile.exists() )
            {
                System.err.println( "Could not find either a " + POMv4 + " nor a " + POMv3 + " project descriptor." );

                // TODO: Use some constant for this value. Trygve.
                return 1;
            }
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
            // TODO: create some sane output.
            // Take this info from generated piece of meta data which uses
            // the POM itself as the source. We don't want to get into the same
            // bullshit of manually updating some constant in the source.
            // [Brett] My thoughts on this (something I long ago slated for m1), is to store the pom in
            //  META-INF or something similar for a jar, and then read that back. maven-model being so
            //  trim makes that more of a reality. The other alternative is simply to store that info in
            //  the manifest in plain text and read that back.
            System.out.println( "Maven version: " );

            return 0;
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

            return 0;
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

        // @todo we may wish for more types of error codes - perhaps letting the response define them?
        if ( response.isExecutionFailure() )
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
