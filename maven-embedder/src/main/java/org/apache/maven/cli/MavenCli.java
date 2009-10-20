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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
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

    private DefaultPlexusContainer container;

    private PrintStreamLogger logger;

    private ModelProcessor modelProcessor;
    
    public static void main( String[] args )
    {
        int result = main( args, null );

        System.exit( result );
    }

    /** @noinspection ConfusingMainMethod */
    public static int main( String[] args, ClassWorld classWorld )
    {
        MavenCli cli = new MavenCli( classWorld );

        return cli.doMain( args, null, System.out, System.err );
    }

    public MavenCli()
    {
        this( null );
    }

    public MavenCli( ClassWorld classWorld )
    {
        if ( classWorld == null )
        {
            classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
        }

        try
        {
            ContainerConfiguration cc =
                new DefaultContainerConfiguration().setClassWorld( classWorld ).setName( "embedder" );

            container = new DefaultPlexusContainer( cc );
        }
        catch ( PlexusContainerException e )
        {
            throw new IllegalStateException( "Could not start component container: " + e.getMessage(), e );
        }

        logger = new PrintStreamLogger( System.out );

        container.setLoggerManager( new MavenLoggerManager( logger ) );
        
        customizeContainer( container );
    }

    protected void customizeContainer( PlexusContainer container )
    {        
    }
    
    public int doMain( String[] args, String workingDirectory, PrintStream stdout, PrintStream stderr )
    {
        if ( stdout == null )
        {
            stdout = System.out;
        }
        if ( stderr == null )
        {
            stderr = System.err;
        }
        if ( workingDirectory == null )
        {
            workingDirectory = System.getProperty( "user.dir" );
        }

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
            stderr.println( "Unable to parse command line options: " + e.getMessage() );
            cliManager.displayHelp( stdout );
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
            cliManager.displayHelp( stdout );

            return 0;
        }

        if ( commandLine.hasOption( CLIManager.VERSION ) )
        {
            CLIReportingUtils.showVersion( stdout );

            return 0;
        }

        // Make sure the Maven home directory is an absolute path to save us from confusion with say drive-relative
        // Windows paths.
        String mavenHome = System.getProperty( "maven.home" );
        if ( mavenHome != null )
        {
            System.setProperty( "maven.home", new File( mavenHome ).getAbsolutePath() );
        }

        PrintStream fileStream = null;

        if ( commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            File logFile = new File( commandLine.getOptionValue( CLIManager.LOG_FILE ) );
            logFile = resolveFile( logFile, workingDirectory );

            try
            {
                fileStream = new PrintStream( logFile );
                logger.setStream( fileStream );
            }
            catch ( FileNotFoundException e )
            {
                stderr.println( e );
                logger.setStream( stdout );
            }
        }
        else
        {
            logger.setStream( stdout );
        }

        //
        
        Maven maven;
        
        try
        {
            maven = container.lookup( Maven.class );
            
            modelProcessor = createModelProcessor( container );
        }
        catch ( ComponentLookupException e )
        {
            CLIReportingUtils.showError( logger, "Unable to start the embedder: ", e, showErrors );

            return 1;
        }
                
        Configuration configuration = buildEmbedderConfiguration( commandLine, workingDirectory );        
        
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();

        request.setGlobalSettingsFile( configuration.getGlobalSettingsFile() );
        request.setUserSettingsFile( configuration.getUserSettingsFile() );

        populateProperties( request, commandLine );

        Settings settings;

        try
        {
            SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
            settingsRequest.setGlobalSettingsFile( configuration.getGlobalSettingsFile() );
            settingsRequest.setUserSettingsFile( configuration.getUserSettingsFile() );
            settingsRequest.setSystemProperties( request.getSystemProperties() );
            settingsRequest.setUserProperties( request.getUserProperties() );

            SettingsBuilder settingsBuilder = container.lookup( SettingsBuilder.class );

            try
            {
                SettingsBuildingResult settingsResult = settingsBuilder.build( settingsRequest );

                settings = settingsResult.getEffectiveSettings();

                if ( !settingsResult.getProblems().isEmpty() && logger.isWarnEnabled() )
                {
                    logger.warn( "" );
                    logger.warn( "Some problems were encountered while building the effective settings" );

                    for ( SettingsProblem problem : settingsResult.getProblems() )
                    {
                        logger.warn( problem.getMessage() + " @ " + problem.getLocation() );
                    }

                    logger.warn( "" );
                }
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
        catch ( SettingsBuildingException e )
        {
            CLIReportingUtils.showError( logger, "Failed to read settings: ", e, showErrors );

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

        populateRequest( request, commandLine, workingDirectory, debug, quiet, showErrors );

        request.setExecutionListener( new ExecutionEventLogger( logger ) );

        container.getLoggerManager().setThresholds( request.getLoggingLevel() );

        if ( debug || commandLine.hasOption( CLIManager.SHOW_VERSION ) )
        {
            CLIReportingUtils.showVersion( stdout );
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

                stdout.println( cipher.encryptAndDecorate( passwd, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION ) );

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
                    stderr.println( "Master password is not set in the setting security file" );

                    return 1;
                }

                DefaultPlexusCipher cipher = new DefaultPlexusCipher();
                String masterPasswd = cipher.decryptDecorated( master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
                stdout.println( cipher.encryptAndDecorate( passwd, masterPasswd ) );

                return 0;
            }
        }
        catch ( Exception e )
        {
            stderr.println( "FATAL ERROR: " + "Error encrypting password: " + e.getMessage() );

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

        try
        {
            return processResult( request, result, showErrors );
        }
        finally
        {
            if ( fileStream != null )
            {
                fileStream.close();
            }
        }
    }

    private int processResult( MavenExecutionRequest request, MavenExecutionResult result, boolean showErrors )
    {
        if ( result.hasExceptions() )
        {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<String, String>();

            for ( Throwable exception : result.getExceptions() )
            {
                ExceptionSummary summary = handler.handleException( exception );

                logSummary( summary, references, "", showErrors );
            }

            logger.error( "" );

            if ( !showErrors )
            {
                logger.error( "To see the full stack trace of the errors, re-run Maven with the -e switch." );
            }
            if ( !logger.isDebugEnabled() )
            {
                logger.error( "Re-run Maven using the -X switch to enable full debug logging." );
            }

            if ( !references.isEmpty() )
            {
                logger.error( "" );
                logger.error( "For more information about the errors and possible solutions"
                    + ", please read the following articles:" );

                for ( Map.Entry<String, String> entry : references.entrySet() )
                {
                    logger.error( entry.getValue() + " " + entry.getKey() );
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

    private void logSummary( ExceptionSummary summary, Map<String, String> references, String indent, boolean showErrors )
    {
        String referenceKey = "";

        if ( StringUtils.isNotEmpty( summary.getReference() ) )
        {
            referenceKey = references.get( summary.getReference() );
            if ( referenceKey == null )
            {
                referenceKey = "[" + references.size() + "]";
                references.put( summary.getReference(), referenceKey );
            }
        }

        if ( showErrors )
        {
            logger.error( indent + referenceKey, summary.getException() );
        }
        else
        {
            logger.error( indent + summary.getMessage() + " " + referenceKey );
        }

        indent += "  ";

        for ( ExceptionSummary child : summary.getChildren() )
        {
            logSummary( child, references, indent, showErrors );
        }
    }

    protected ModelProcessor createModelProcessor( PlexusContainer container ) 
        throws ComponentLookupException
    {
        return container.lookup( ModelProcessor.class );        
    }

    private Configuration buildEmbedderConfiguration( CommandLine commandLine, String workingDirectory )
    {
        File userSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) );
            userSettingsFile = resolveFile( userSettingsFile, workingDirectory );
        }
        else
        {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        File globalSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) )
        {
            globalSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) );
            globalSettingsFile = resolveFile( globalSettingsFile, workingDirectory );
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

    private void populateProperties( MavenExecutionRequest request, CommandLine commandLine )
    {
        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();
        populateProperties( commandLine, systemProperties, userProperties );
        request.setUserProperties( userProperties );
        request.setSystemProperties( systemProperties );
    }
    
    private MavenExecutionRequest populateRequest( MavenExecutionRequest request, CommandLine commandLine,
                                                  String workingDirectory, boolean debug, boolean quiet, boolean showErrors )
    {
        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            request.setInteractiveMode( false );
        }

        boolean pluginUpdateOverride = false;

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES )
            || commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            pluginUpdateOverride = true;
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            pluginUpdateOverride = false;
        }

        boolean noSnapshotUpdates = false;
        if ( commandLine.hasOption( CLIManager.SUPRESS_SNAPSHOT_UPDATES ) )
        {
            noSnapshotUpdates = true;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        @SuppressWarnings( "unchecked" )
        List<String> goals = commandLine.getArgList();

        boolean recursive = true;

        // this is the default behavior.
        String reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;

        if ( commandLine.hasOption( CLIManager.NON_RECURSIVE ) )
        {
            recursive = false;
        }

        if ( commandLine.hasOption( CLIManager.FAIL_FAST ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_AT_END ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_AT_END;
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_NEVER ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_NEVER;
        }

        if ( commandLine.hasOption( CLIManager.OFFLINE ) )
        {
            request.setOffline( true );
        }

        boolean updateSnapshots = false;

        if ( commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            updateSnapshots = true;
        }

        String globalChecksumPolicy = null;

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        }

        File baseDirectory = new File( workingDirectory, "" ).getAbsoluteFile();

        // ----------------------------------------------------------------------
        // Profile Activation
        // ----------------------------------------------------------------------

        List<String> activeProfiles = new ArrayList<String>();

        List<String> inactiveProfiles = new ArrayList<String>();

        if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
        {
            String [] profileOptionValues = commandLine.getOptionValues( CLIManager.ACTIVATE_PROFILES );
            if ( profileOptionValues != null )
            {
                for ( int i = 0; i < profileOptionValues.length; ++i )
                {
                    StringTokenizer profileTokens = new StringTokenizer( profileOptionValues[i] , "," );

                    while ( profileTokens.hasMoreTokens() )
                    {
                        String profileAction = profileTokens.nextToken().trim();

                        if ( profileAction.startsWith( "-" ) || profileAction.startsWith( "!" ) )
                        {
                            inactiveProfiles.add( profileAction.substring( 1 ) );
                        }
                        else if ( profileAction.startsWith( "+" ) )
                        {
                            activeProfiles.add( profileAction.substring( 1 ) );
                        }
                        else
                        {
                            activeProfiles.add( profileAction );
                        }
                    }
                }
            }
        }

        ArtifactTransferListener transferListener;

        if ( request.isInteractiveMode() )
        {
            transferListener = new ConsoleMavenTransferListener();
        }
        else
        {
            transferListener = new BatchModeMavenTransferListener();
        }

        transferListener.setShowChecksumEvents( false );

        String alternatePomFile = null;
        if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
        {
            alternatePomFile = commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE );
        }

        int loggingLevel;

        if ( debug )
        {
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_DEBUG;
        }
        else if ( quiet )
        {
            // TODO: we need to do some more work here. Some plugins use sys out or log errors at info level.
            // Ideally, we could use Warn across the board
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_ERROR;
            // TODO:Additionally, we can't change the mojo level because the component key includes the version and it isn't known ahead of time. This seems worth changing.
        }
        else
        {
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_INFO;
        }

        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();
        populateProperties( commandLine, systemProperties, userProperties );

        File userToolchainsFile;
        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_TOOLCHAINS ) )
        {
            userToolchainsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_TOOLCHAINS ) );
            userToolchainsFile = resolveFile( userToolchainsFile, workingDirectory );
        }
        else
        {
            userToolchainsFile = MavenCli.DEFAULT_USER_TOOLCHAINS_FILE;
        }

        request
            .setBaseDirectory( baseDirectory )
            .setGoals( goals )
            .setSystemProperties( systemProperties )
            .setUserProperties( userProperties )
            .setReactorFailureBehavior( reactorFailureBehaviour ) // default: fail fast
            .setRecursive( recursive ) // default: true
            .setShowErrors( showErrors ) // default: false
            .setUsePluginUpdateOverride( pluginUpdateOverride )
            .addActiveProfiles( activeProfiles ) // optional
            .addInactiveProfiles( inactiveProfiles ) // optional
            .setLoggingLevel( loggingLevel ) // default: info
            .setTransferListener( transferListener ) // default: batch mode which goes along with interactive
            .setUpdateSnapshots( updateSnapshots ) // default: false
            .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
            .setGlobalChecksumPolicy( globalChecksumPolicy ) // default: warn
            .setUserToolchainsFile( userToolchainsFile );

        if ( alternatePomFile != null )
        {
            request.setPom( resolveFile( new File( alternatePomFile ), workingDirectory ) );
        }
        else if ( request.getPom() != null && !request.getPom().isAbsolute() )
        {
            request.setPom( request.getPom().getAbsoluteFile() );
        }

        if ( ( request.getPom() != null ) && ( request.getPom().getParentFile() != null ) )
        {
            request.setBaseDirectory( request.getPom().getParentFile() );
        }
        else if ( ( request.getPom() == null ) && ( request.getBaseDirectory() != null ) )
        {
            File pom = modelProcessor.locatePom( new File( request.getBaseDirectory() ) );

            request.setPom( pom );
        }
        // TODO: Is this correct?
        else if ( request.getBaseDirectory() == null )
        {
            request.setBaseDirectory( new File( System.getProperty( "user.dir" ) ) );
        }        
        
        if ( commandLine.hasOption( CLIManager.RESUME_FROM ) )
        {
            request.setResumeFrom( commandLine.getOptionValue( CLIManager.RESUME_FROM ) );
        }

        if ( commandLine.hasOption( CLIManager.PROJECT_LIST ) )
        {
            String projectList = commandLine.getOptionValue( CLIManager.PROJECT_LIST );
            String[] projects = StringUtils.split( projectList, "," );
            request.setSelectedProjects( Arrays.asList( projects ) );
        }

        if ( commandLine.hasOption( CLIManager.ALSO_MAKE ) && !commandLine.hasOption( CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( MavenExecutionRequest.REACTOR_MAKE_UPSTREAM );
        }
        else if ( !commandLine.hasOption( CLIManager.ALSO_MAKE )
            && commandLine.hasOption( CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM );
        }
        else if ( commandLine.hasOption( CLIManager.ALSO_MAKE )
            && commandLine.hasOption( CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( MavenExecutionRequest.REACTOR_MAKE_BOTH );
        }

        String localRepoProperty = request.getUserProperties().getProperty( MavenCli.LOCAL_REPO_PROPERTY );

        if ( localRepoProperty == null )
        {
            localRepoProperty = request.getSystemProperties().getProperty( MavenCli.LOCAL_REPO_PROPERTY );
        }

        if ( localRepoProperty != null )
        {
            request.setLocalRepositoryPath( localRepoProperty );
        }

        return request;
    }

    static File resolveFile( File file, String workingDirectory )
    {
        if ( file == null )
        {
            return null;
        }
        else if ( file.isAbsolute() )
        {
            return file;
        }
        else if ( file.getPath().startsWith( File.separator ) )
        {
            // drive-relative Windows path
            return file.getAbsoluteFile();
        }
        else
        {
            return new File( workingDirectory, file.getPath() );
        }
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    static void populateProperties( CommandLine commandLine, Properties systemProperties, Properties userProperties )
    {
        // add the env vars to the property set, with the "env." prefix
        // XXX support for env vars should probably be removed from the ModelInterpolator
        try
        {
            Properties envVars = CommandLineUtils.getSystemEnvVars();
            for ( Entry<Object, Object> e : envVars.entrySet() )
            {
                systemProperties.setProperty( "env." + e.getKey().toString(), e.getValue().toString() );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Error getting environment vars for profile activation: " + e );
        }

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY );

            if ( defStrs != null )
            {
                for ( int i = 0; i < defStrs.length; ++i )
                {
                    setCliProperty( defStrs[i], userProperties );
                }
            }
        }

        systemProperties.putAll( System.getProperties() );
    }

    private static void setCliProperty( String property, Properties properties )
    {
        String name;

        String value;

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

        properties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }    
}
