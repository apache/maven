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

import java.io.File;

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
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * @author jason van zyl
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
    public static int main( String[] args, ClassWorld classWorld )
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

        // Make sure the Maven home directory is an absolute path to save us from confusion with say drive-relative
        // Windows paths.
        String mavenHome = System.getProperty( "maven.home" );
        if ( mavenHome != null )
        {
            System.setProperty( "maven.home", new File( mavenHome ).getAbsolutePath() );
        }

        MavenExecutionRequest request = CLIRequestUtils.buildRequest( commandLine, debug, quiet, showErrors );

        Configuration configuration = buildEmbedderConfiguration( request, commandLine, classWorld );

        ConfigurationValidationResult cvr = MavenEmbedder.validateConfiguration( configuration );

        if ( cvr.isUserSettingsFilePresent() && !cvr.isUserSettingsFileParses() )
        {
            //TODO: CLIReportingUtils.showError( "Error reading user settings: ", cvr.getUserSettingsException(), showErrors );

            return 1;
        }

        if ( cvr.isGlobalSettingsFilePresent() && !cvr.isGlobalSettingsFileParses() )
        {
            //TODO: CLIReportingUtils.showError( "Error reading global settings: ", cvr.getGlobalSettingsException(), showErrors );

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
            //TODO: CLIReportingUtils.showError( "Unable to start the embedder: ", e, showErrors );

            return 1;
        }

        MavenExecutionResult result = mavenEmbedder.execute( request );

        try
        {
            mavenEmbedder.stop();
        }
        catch ( MavenEmbedderException e )
        {
            result.addException( e );
        }

        // The exception handling should be handled in Maven itself.
        
        if ( result.hasExceptions() )
        {
            ExceptionSummary es = result.getExceptionSummary();
            
            if ( es == null )
            {
                result.getExceptions().get( 0 ).printStackTrace();
            }
            else
            {
            System.out.println( es.getMessage() );
            
            es.getException().printStackTrace();
            }
            
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
            userSettingsFile = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
        }

        File globalSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) )
        {
            globalSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) );
        }
        else
        {
            globalSettingsFile = MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE;
        }

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( userSettingsFile )
            .setGlobalSettingsFile( globalSettingsFile )
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
