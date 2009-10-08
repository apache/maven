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
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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

    public static final String userHome = System.getProperty( "user.home" );

    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( userMavenConfigurationHome, "settings.xml" );

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
        new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    public static final File DEFAULT_USER_TOOLCHAINS_FILE = new File( userMavenConfigurationHome, "toolchains.xml" );
    
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

        //
        
        Maven maven;
        
        DefaultPlexusContainer container;
        
        Logger logger;
        
        try
        {
            ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld( classWorld )
                .setName( "embedder" );

            container = new DefaultPlexusContainer( cc );

            logger = container.getLogger();

            if ( commandLine.hasOption( CLIManager.LOG_FILE ) )
            {
                File logFile = new File( commandLine.getOptionValue( CLIManager.LOG_FILE ) ).getAbsoluteFile();

                logger = new FileLogger( logFile );

                container.setLoggerManager( new MavenLoggerManager( logger ) );
            }
            
            maven = container.lookup( Maven.class );
        }
        catch ( PlexusContainerException e )
        {
            CLIReportingUtils.showError( new ConsoleLogger( Logger.LEVEL_ERROR, Maven.class.getName() ), "Unable to start the embedder: ", e, showErrors );

            return 1;
        }
        catch ( ComponentLookupException e )
        {
            CLIReportingUtils.showError( new ConsoleLogger( Logger.LEVEL_ERROR, Maven.class.getName() ), "Unable to start the embedder: ", e, showErrors );

            return 1;
        }
                
        Configuration configuration = buildEmbedderConfiguration( commandLine );        
        
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();

        request.setGlobalSettingsFile( configuration.getGlobalSettingsFile() );
        request.setUserSettingsFile( configuration.getUserSettingsFile() );

        CLIRequestUtils.populateProperties( request, commandLine );

        Settings settings;

        try
        {
            MavenSettingsBuilder settingsBuilder = container.lookup( MavenSettingsBuilder.class );

            try
            {
                settings = settingsBuilder.buildSettings( request );
            }
            finally
            {
                try
                {
                    container.release( settingsBuilder );
                }
                catch ( ComponentLifecycleException e )
                {
                    logger.debug( "Failed to release component: " + e.getMessage(), e );
                }
            }
        }
        catch ( ComponentLookupException e )
        {
            CLIReportingUtils.showError( logger, "Unable to lookup settings builder: ", e, showErrors );

            return 1;
        }
        catch ( IOException e )
        {
            CLIReportingUtils.showError( logger, "Failed to read settings: ", e, showErrors );

            return 1;
        }
        catch ( XmlPullParserException e )
        {
            CLIReportingUtils.showError( logger, "Failed to parse settings: ", e, showErrors );

            return 1;
        }

        try
        {
            MavenExecutionRequestPopulator requestPopulator = container.lookup( MavenExecutionRequestPopulator.class );

            try
            {
                requestPopulator.populateFromSettings( request, settings );
            }
            finally
            {
                try
                {
                    container.release( requestPopulator );
                }
                catch ( ComponentLifecycleException e )
                {
                    logger.debug( "Failed to release component: " + e.getMessage(), e );
                }
            }
        }
        catch ( ComponentLookupException e )
        {
            CLIReportingUtils.showError( logger, "Unable to lookup execution request populator: ", e, showErrors );

            return 1;
        }
        catch ( MavenExecutionRequestPopulationException e )
        {
            CLIReportingUtils.showError( logger, "Failed to process settings: ", e, showErrors );

            return 1;
        }

        CLIRequestUtils.populateRequest( request, commandLine, debug, quiet, showErrors );

        request.setExecutionListener( new ExecutionEventLogger( logger ) );

        container.getLoggerManager().setThresholds( request.getLoggingLevel() );

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

        if ( configuration.getGlobalSettingsFile() != null )
        {
            request.setGlobalSettingsFile( configuration.getGlobalSettingsFile() );
        }

        if ( configuration.getUserSettingsFile() != null )
        {
            request.setUserSettingsFile( configuration.getUserSettingsFile() );
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
                dispatcher = (DefaultSecDispatcher) container.lookup( SecDispatcher.class );
                String configurationFile = dispatcher.getConfigurationFile();
                if ( configurationFile.startsWith( "~" ) )
                {
                    configurationFile = System.getProperty( "user.home" ) + configurationFile.substring( 1 );
                }
                String file = System.getProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile );
                container.release( dispatcher );

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
                String masterPasswd = cipher.decryptDecorated( master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
                System.out.println( cipher.encryptAndDecorate( passwd, masterPasswd ) );

                return 0;
            }
        }
        catch ( Exception e )
        {
            System.err.println( "FATAL ERROR: " + "Error encrypting password: " + e.getMessage() );

            return 1;
        }
        
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        try
        {
            MavenExecutionRequestPopulator populator = container.lookup(  MavenExecutionRequestPopulator.class );            
            
            request = populator.populateDefaults( request );
        }
        catch ( MavenExecutionRequestPopulationException e )
        {
            result.addException( e );
        }
        catch ( ComponentLookupException e )
        {
            result.addException( e );
        }

        result = maven.execute( request );

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

    private Configuration buildEmbedderConfiguration( CommandLine commandLine )
    {
        File userSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) );
        }
        else
        {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        File globalSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) )
        {
            globalSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) );
        }
        else
        {
            globalSettingsFile = DEFAULT_GLOBAL_SETTINGS_FILE;
        }

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( userSettingsFile )
            .setGlobalSettingsFile( globalSettingsFile );

        return configuration;
    }

    // ----------------------------------------------------------------------------
    // Options for settings
    //
    // 1. No settings
    // 2. User settings only
    // 3. Global settings only
    // 4. Both Users settings and Global settings. In the case that both are present
    //    the User settings take priority.
    //
    // What we would like to provide is a way that the client code does not have
    // to deal with settings configuration at all.
    // ----------------------------------------------------------------------------

    public static ConfigurationValidationResult validateConfiguration( Configuration configuration )
    {
        DefaultConfigurationValidationResult result = new DefaultConfigurationValidationResult();

        Reader fileReader = null;

        // User settings

        if ( configuration.getUserSettingsFile() != null )
        {
            try
            {
                fileReader = ReaderFactory.newXmlReader( configuration.getUserSettingsFile() );

                result.setUserSettings( new SettingsXpp3Reader().read( fileReader ) );
            }
            catch ( IOException e )
            {
                result.setUserSettingsException( e );
            }
            catch ( XmlPullParserException e )
            {
                result.setUserSettingsException( e );
            }
            finally
            {
                IOUtil.close( fileReader );
            }
        }

        // Global settings

        if ( configuration.getGlobalSettingsFile() != null )
        {
            try
            {
                fileReader = ReaderFactory.newXmlReader( configuration.getGlobalSettingsFile() );

                result.setGlobalSettings( new SettingsXpp3Reader().read( fileReader ) );
            }
            catch ( IOException e )
            {
                result.setGlobalSettingsException( e );
            }
            catch ( XmlPullParserException e )
            {
                result.setGlobalSettingsException( e );
            }
            finally
            {
                IOUtil.close( fileReader );
            }
        }

        return result;
    }    
}
