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
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderFileLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.classworlds.ClassWorld;

import java.io.File;

/**
 * @author jason van zyl
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    public static final String LOCAL_REPO_PROPERTY = "maven.repo.local";

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
            CLIReportingUtils.showVersion();

            return 0;
        }
        else if ( debug || commandLine.hasOption( CLIManager.SHOW_VERSION ) )
        {
            CLIReportingUtils.showVersion();
        }

        MavenExecutionRequest request = CLIRequestUtils.buildRequest( commandLine, debug, quiet, showErrors );

        Configuration configuration = buildEmbedderConfiguration( request, commandLine, classWorld );

        ConfigurationValidationResult cvr = MavenEmbedder.validateConfiguration( configuration );

        if ( cvr.isUserSettingsFilePresent() && !cvr.isUserSettingsFileParses() )
        {
            CLIReportingUtils.showError( "Error reading user settings: ", cvr.getUserSettingsException(), showErrors );

            return 1;
        }

        if ( cvr.isGlobalSettingsFilePresent() && !cvr.isGlobalSettingsFileParses() )
        {
            CLIReportingUtils.showError( "Error reading global settings: ", cvr.getGlobalSettingsException(), showErrors );

            return 1;
        }

        MavenEmbedder mavenEmbedder;
        MavenEmbedderLogger logger;
        try
        {
            mavenEmbedder = new MavenEmbedder( configuration );

            logger = mavenEmbedder.getLogger();

            if ( mavenEmbedder.isOffline( request ) )
            {
                logger.info( "You are working in offline mode." );
            }
        }
        catch ( MavenEmbedderException e )
        {
            CLIReportingUtils.showError( "Unable to start the embedder: ", e, showErrors );

            return 1;
        }

        MavenExecutionResult result = mavenEmbedder.execute( request );

        CLIReportingUtils.logResult( request, result, logger );

        if ( result.hasExceptions() )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    private Configuration buildEmbedderConfiguration( MavenExecutionRequest request, CommandLine commandLine, ClassWorld classWorld )
    {
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
            .setErrorReporter( new DefaultCoreErrorReporter() )
            .setUserSettingsFile( userSettingsFile )
            .setGlobalSettingsFile( MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE )
            .setClassWorld( classWorld );

        if ( commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            File logFile = new File(
                request.getBaseDirectory(),
                commandLine.getOptionValue( CLIManager.LOG_FILE ) );

            configuration.setMavenEmbedderLogger( new MavenEmbedderFileLogger( logFile ) );
        }
        else
        {
            configuration.setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );
        }

        String localRepoProperty = request.getProperties().getProperty( LOCAL_REPO_PROPERTY );

        if ( localRepoProperty != null )
        {
            configuration.setLocalRepository( new File( localRepoProperty ) );
        }

        return configuration;
    }

}
