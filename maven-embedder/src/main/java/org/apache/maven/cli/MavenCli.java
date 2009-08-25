package org.apache.maven.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.Arrays;

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
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

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

    public int doMain( String[] args, ClassWorld classWorld )
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

        // Make sure the Maven home directory is an absolute path to save us from confusion with say drive-relative
        // Windows paths.
        String mavenHome = System.getProperty( "maven.home" );
        if ( mavenHome != null )
        {
            System.setProperty( "maven.home", new File( mavenHome ).getAbsolutePath() );
        }

        MavenExecutionRequest request = CLIRequestUtils.buildRequest( commandLine, debug, quiet, showErrors );

        Configuration configuration = buildEmbedderConfiguration( request, commandLine, classWorld );

        MavenEmbedderLogger logger = configuration.getMavenEmbedderLogger();

        request.setExecutionListeners( Arrays.<ExecutionListener> asList( new ExecutionEventLogger( logger ) ) );

        if ( debug || commandLine.hasOption( CLIManager.SHOW_VERSION ) )
        {
            CLIReportingUtils.showVersion();
        }

        if ( showErrors )
        {
            logger.info( "Error stacktraces are turned on." );
        }

        if ( MavenExecutionRequest.CHECKSUM_POLICY_WARN.equals( request.getGlobalChecksumPolicy() ) )
        {
            logger.info( "Disabling strict checksum verification on all artifact downloads." );
        }
        else if ( MavenExecutionRequest.CHECKSUM_POLICY_FAIL.equals( request.getGlobalChecksumPolicy() ) )
        {
            logger.info( "Enabling strict checksum verification on all artifact downloads." );
        }

        ConfigurationValidationResult cvr = MavenEmbedder.validateConfiguration( configuration );

        if ( cvr.isUserSettingsFilePresent() && !cvr.isUserSettingsFileParses() )
        {
            CLIReportingUtils.showError( logger, "Error reading user settings: ", cvr.getUserSettingsException(),
                                         showErrors );

            return 1;
        }

        if ( cvr.isGlobalSettingsFilePresent() && !cvr.isGlobalSettingsFileParses() )
        {
            CLIReportingUtils.showError( logger, "Error reading global settings: ", cvr.getGlobalSettingsException(),
                                         showErrors );

            return 1;
        }

        if ( configuration.getGlobalSettingsFile() != null )
        {
            request.setGlobalSettingsFile( configuration.getGlobalSettingsFile() );
        }

        if ( configuration.getUserSettingsFile() != null )
        {
            request.setUserSettingsFile( configuration.getUserSettingsFile() );
        }

        MavenEmbedder mavenEmbedder;

        try
        {
            mavenEmbedder = new MavenEmbedder( configuration );
        }
        catch ( MavenEmbedderException e )
        {
            CLIReportingUtils.showError( logger, "Unable to start the embedder: ", e, showErrors );

            return 1;
        }

        try
        {
            if ( commandLine.hasOption( CLIManager.ENCRYPT_MASTER_PASSWORD ) )
            {
                String passwd = commandLine.getOptionValue( CLIManager.ENCRYPT_MASTER_PASSWORD );

                DefaultPlexusCipher cipher = new DefaultPlexusCipher();

                System.out.println( cipher.encryptAndDecorate( passwd,
                                                               DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION ) );

                return 0;
            }
            else if ( commandLine.hasOption( CLIManager.ENCRYPT_PASSWORD ) )
            {
                String passwd = commandLine.getOptionValue( CLIManager.ENCRYPT_PASSWORD );

                DefaultSecDispatcher dispatcher;
                dispatcher = (DefaultSecDispatcher) mavenEmbedder.getPlexusContainer().lookup( SecDispatcher.class );
                String configurationFile = dispatcher.getConfigurationFile();
                if ( configurationFile.startsWith( "~" ) )
                {
                    configurationFile = System.getProperty( "user.home" ) + configurationFile.substring( 1 );
                }
                String file = System.getProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile );
                mavenEmbedder.getPlexusContainer().release( dispatcher );

                String master = null;

                SettingsSecurity sec = SecUtil.read( file, true );
                if ( sec != null )
                {
                    master = sec.getMaster();
                }

                if ( master == null )
                {
                    System.err.println( "Master password is not set in the setting security file" );

                    return 1;
                }

                DefaultPlexusCipher cipher = new DefaultPlexusCipher();
                String masterPasswd =
                    cipher.decryptDecorated( master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
                System.out.println( cipher.encryptAndDecorate( passwd, masterPasswd ) );

                return 0;
            }
        }
        catch ( Exception e )
        {
            System.err.println( "FATAL ERROR: " + "Error encrypting password: " + e.getMessage() );
            e.printStackTrace();

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
                logger.error( "", result.getExceptions().get( 0 ) );
            }
            else
            {
                if ( showErrors )
                {
                    logger.error( es.getMessage(), es.getException() );
                }
                else
                {
                    logger.error( es.getMessage() );
                }
            }

            if ( MavenExecutionRequest.REACTOR_FAIL_NEVER.equals( request.getReactorFailureBehavior() ) )
            {
                logger.info( "Build failures were ignored." );

                return 0;
            }
            else
            {
                return 1;
            }
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

        Configuration configuration = new DefaultConfiguration().setUserSettingsFile( userSettingsFile ).setGlobalSettingsFile( globalSettingsFile ).setClassWorld( classWorld );

        if ( commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            File logFile = new File( request.getBaseDirectory(), commandLine.getOptionValue( CLIManager.LOG_FILE ) );

            configuration.setMavenEmbedderLogger( new MavenEmbedderFileLogger( logFile ) );
        }
        else
        {
            configuration.setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );
        }

        String localRepoProperty = request.getUserProperties().getProperty( LOCAL_REPO_PROPERTY );

        if ( localRepoProperty != null )
        {
            configuration.setLocalRepository( new File( localRepoProperty ) );
        }

        return configuration;
    }

}
