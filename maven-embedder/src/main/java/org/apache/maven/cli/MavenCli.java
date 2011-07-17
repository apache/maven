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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.maven.BuildAbort;
import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleWeaveBuilder;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsSource;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.transfer.TransferListener;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

// TODO: push all common bits back to plexus cli and prepare for transition to Guice. We don't need 50 ways to make CLIs

/**
 * @author Jason van Zyl
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    public static final String LOCAL_REPO_PROPERTY = "maven.repo.local";

    public static final String THREADS_DEPRECATED = "maven.threads.experimental";

    public static final String userHome = System.getProperty( "user.home" );

    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( userMavenConfigurationHome, "settings.xml" );

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
        new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    public static final File DEFAULT_USER_TOOLCHAINS_FILE = new File( userMavenConfigurationHome, "toolchains.xml" );

    private static final String EXT_CLASS_PATH = "maven.ext.class.path";

    private ClassWorld classWorld;

    // Per-instance container supports fast embedded execution of core ITs
    private DefaultPlexusContainer container;

    private Logger logger;

    private EventSpyDispatcher eventSpyDispatcher;

    private ModelProcessor modelProcessor;

    private Maven maven;

    private MavenExecutionRequestPopulator executionRequestPopulator;

    private SettingsBuilder settingsBuilder;

    private DefaultSecDispatcher dispatcher;

    public MavenCli()
    {
        this( null );
    }

    // This supports painless invocation by the Verifier during embedded execution of the core ITs
    public MavenCli( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
    }

    public static void main( String[] args )
    {
        int result = main( args, null );

        System.exit( result );
    }

    /** @noinspection ConfusingMainMethod */
    public static int main( String[] args, ClassWorld classWorld )
    {
        MavenCli cli = new MavenCli();
        return cli.doMain( new CliRequest( args, classWorld ) );
    }

    // TODO: need to externalize CliRequest
    public static int doMain( String[] args, ClassWorld classWorld )
    {
        MavenCli cli = new MavenCli();
        return cli.doMain( new CliRequest( args, classWorld ) );
    }

    // This supports painless invocation by the Verifier during embedded execution of the core ITs
    public int doMain( String[] args, String workingDirectory, PrintStream stdout, PrintStream stderr )
    {
        PrintStream oldout = System.out;
        PrintStream olderr = System.err;

        try
        {
            if ( stdout != null )
            {
                System.setOut( stdout );
            }
            if ( stderr != null )
            {
                System.setErr( stderr );
            }

            CliRequest cliRequest = new CliRequest( args, classWorld );
            cliRequest.workingDirectory = workingDirectory;

            return doMain( cliRequest );
        }
        finally
        {
            System.setOut( oldout );
            System.setErr( olderr );
        }
    }

    // TODO: need to externalize CliRequest
    public int doMain( CliRequest cliRequest )
    {
        try
        {
            initialize( cliRequest );
            // Need to process cli options first to get possible logging options
            cli( cliRequest );
            logging( cliRequest );
            version( cliRequest );
            properties( cliRequest );
            container( cliRequest );
            commands( cliRequest );
            settings( cliRequest );
            populateRequest( cliRequest );
            encryption( cliRequest );
            return execute( cliRequest );
        }
        catch( ExitException e )
        {
            return e.exitCode;
        }
        catch ( UnrecognizedOptionException e )
        {
            // pure user error, suppress stack trace
            return 1;
        }
        catch ( BuildAbort e )
        {
            CLIReportingUtils.showError( logger, "ABORTED", e, cliRequest.showErrors );

            return 2;
        }
        catch ( Exception e )
        {
            CLIReportingUtils.showError( logger, "Error executing Maven.", e, cliRequest.showErrors );

            return 1;
        }
        finally
        {
            if ( cliRequest.fileStream != null )
            {
                cliRequest.fileStream.close();
            }
        }
    }

    private void initialize( CliRequest cliRequest )
    {
        if ( cliRequest.workingDirectory == null )
        {
            cliRequest.workingDirectory = System.getProperty( "user.dir" );
        }

        //
        // Make sure the Maven home directory is an absolute path to save us from confusion with say drive-relative
        // Windows paths.
        //
        String mavenHome = System.getProperty( "maven.home" );

        if ( mavenHome != null )
        {
            System.setProperty( "maven.home", new File( mavenHome ).getAbsolutePath() );
        }
    }

    //
    // Logging needs to be handled in a standard way at the container level.
    //
    private void logging( CliRequest cliRequest )
    {
        cliRequest.debug = cliRequest.commandLine.hasOption( CLIManager.DEBUG );
        cliRequest.quiet = !cliRequest.debug && cliRequest.commandLine.hasOption( CLIManager.QUIET );
        cliRequest.showErrors = cliRequest.debug || cliRequest.commandLine.hasOption( CLIManager.ERRORS );

        if ( cliRequest.debug )
        {
            cliRequest.request.setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_DEBUG );
        }
        else if ( cliRequest.quiet )
        {
            // TODO: we need to do some more work here. Some plugins use sys out or log errors at info level.
            // Ideally, we could use Warn across the board
            cliRequest.request.setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_ERROR );
            // TODO:Additionally, we can't change the mojo level because the component key includes the version and
            // it isn't known ahead of time. This seems worth changing.
        }
        else
        {
            cliRequest.request.setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_INFO );
        }

        if ( cliRequest.commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            File logFile = new File( cliRequest.commandLine.getOptionValue( CLIManager.LOG_FILE ) );
            logFile = resolveFile( logFile, cliRequest.workingDirectory );

            try
            {
                cliRequest.fileStream = new PrintStream( logFile );

                System.setOut( cliRequest.fileStream );
                System.setErr( cliRequest.fileStream );
            }
            catch ( FileNotFoundException e )
            {
                System.err.println( e );
            }
        }
    }

    //
    // Every bit of information taken from the CLI should be processed here.
    //
    private void cli( CliRequest cliRequest )
        throws Exception
    {
        CLIManager cliManager = new CLIManager();

        try
        {
            cliRequest.commandLine = cliManager.parse( cliRequest.args );
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );
            cliManager.displayHelp( System.out );
            throw e;
        }

        // TODO: these should be moved out of here. Wrong place.
        //
        if ( cliRequest.commandLine.hasOption( CLIManager.HELP ) )
        {
            cliManager.displayHelp( System.out );
            throw new ExitException( 0 );
        }

        if ( cliRequest.commandLine.hasOption( CLIManager.VERSION ) )
        {
            CLIReportingUtils.showVersion( System.out );
            throw new ExitException( 0 );
        }
    }

    private void version( CliRequest cliRequest )
    {
        if ( cliRequest.debug || cliRequest.commandLine.hasOption( CLIManager.SHOW_VERSION ) )
        {
            CLIReportingUtils.showVersion( System.out );
        }
    }

    private void commands( CliRequest cliRequest )
    {
        if ( cliRequest.showErrors )
        {
            logger.info( "Error stacktraces are turned on." );
        }

        if ( MavenExecutionRequest.CHECKSUM_POLICY_WARN.equals( cliRequest.request.getGlobalChecksumPolicy() ) )
        {
            logger.info( "Disabling strict checksum verification on all artifact downloads." );
        }
        else if ( MavenExecutionRequest.CHECKSUM_POLICY_FAIL.equals( cliRequest.request.getGlobalChecksumPolicy() ) )
        {
            logger.info( "Enabling strict checksum verification on all artifact downloads." );
        }
    }

    private void properties( CliRequest cliRequest )
    {
        populateProperties( cliRequest.commandLine, cliRequest.systemProperties, cliRequest.userProperties );
    }

    private void container( CliRequest cliRequest )
        throws Exception
    {
        if ( cliRequest.classWorld == null )
        {
            cliRequest.classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
        }

        DefaultPlexusContainer container = this.container;

        if ( container == null )
        {
            logger = setupLogger( cliRequest );

            ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld( cliRequest.classWorld )
                .setRealm( setupContainerRealm( cliRequest ) )
                .setName( "maven" );

            container = new DefaultPlexusContainer( cc );

            // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
            container.setLookupRealm( null );

            container.setLoggerManager( new MavenLoggerManager( logger ) );

            customizeContainer( container );

            if ( cliRequest.classWorld == classWorld )
            {
                this.container = container;
            }
        }

        container.getLoggerManager().setThresholds( cliRequest.request.getLoggingLevel() );

        Thread.currentThread().setContextClassLoader( container.getContainerRealm() );

        eventSpyDispatcher = container.lookup( EventSpyDispatcher.class );

        DefaultEventSpyContext eventSpyContext = new DefaultEventSpyContext();
        Map<String, Object> data = eventSpyContext.getData();
        data.put( "plexus", container );
        data.put( "workingDirectory", cliRequest.workingDirectory );
        data.put( "systemProperties", cliRequest.systemProperties );
        data.put( "userProperties", cliRequest.userProperties );
        data.put( "versionProperties", CLIReportingUtils.getBuildProperties() );
        eventSpyDispatcher.init( eventSpyContext );

        // refresh logger in case container got customized by spy
        logger = container.getLoggerManager().getLoggerForComponent( MavenCli.class.getName(), null );

        maven = container.lookup( Maven.class );

        executionRequestPopulator = container.lookup( MavenExecutionRequestPopulator.class );

        modelProcessor = createModelProcessor( container );

        settingsBuilder = container.lookup( SettingsBuilder.class );

        dispatcher = (DefaultSecDispatcher) container.lookup( SecDispatcher.class, "maven" );
    }

    private PrintStreamLogger setupLogger( CliRequest cliRequest )
    {
        PrintStreamLogger logger = new PrintStreamLogger( new PrintStreamLogger.Provider()
        {
            public PrintStream getStream()
            {
                return System.out;
            }
        } );

        logger.setThreshold( cliRequest.request.getLoggingLevel() );

        return logger;
    }

    private ClassRealm setupContainerRealm( CliRequest cliRequest )
        throws Exception
    {
        ClassRealm containerRealm = null;

        String extClassPath = cliRequest.userProperties.getProperty( EXT_CLASS_PATH );
        if ( extClassPath == null )
        {
            extClassPath = cliRequest.systemProperties.getProperty( EXT_CLASS_PATH );
        }

        if ( StringUtils.isNotEmpty( extClassPath ) )
        {
            String[] jars = StringUtils.split( extClassPath, File.pathSeparator );

            if ( jars.length > 0 )
            {
                ClassRealm coreRealm = cliRequest.classWorld.getClassRealm( "plexus.core" );
                if ( coreRealm == null )
                {
                    coreRealm = (ClassRealm) cliRequest.classWorld.getRealms().iterator().next();
                }

                ClassRealm extRealm = cliRequest.classWorld.newRealm( "maven.ext", null );

                logger.debug( "Populating class realm " + extRealm.getId() );

                for ( String jar : jars )
                {
                    File file = resolveFile( new File( jar ), cliRequest.workingDirectory );

                    logger.debug( "  Included " + file );

                    extRealm.addURL( file.toURI().toURL() );
                }

                extRealm.setParentRealm( coreRealm );

                containerRealm = extRealm;
            }
        }

        return containerRealm;
    }

    protected void customizeContainer( PlexusContainer container )
    {
    }

    //
    // This should probably be a separate tool and not be baked into Maven.
    //
    private void encryption( CliRequest cliRequest )
        throws Exception
    {
        if ( cliRequest.commandLine.hasOption( CLIManager.ENCRYPT_MASTER_PASSWORD ) )
        {
            String passwd = cliRequest.commandLine.getOptionValue( CLIManager.ENCRYPT_MASTER_PASSWORD );

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();

            System.out.println( cipher.encryptAndDecorate( passwd, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION ) );

            throw new ExitException( 0 );
        }
        else if ( cliRequest.commandLine.hasOption( CLIManager.ENCRYPT_PASSWORD ) )
        {
            String passwd = cliRequest.commandLine.getOptionValue( CLIManager.ENCRYPT_PASSWORD );

            String configurationFile = dispatcher.getConfigurationFile();

            if ( configurationFile.startsWith( "~" ) )
            {
                configurationFile = System.getProperty( "user.home" ) + configurationFile.substring( 1 );
            }

            String file = System.getProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile );

            String master = null;

            SettingsSecurity sec = SecUtil.read( file, true );
            if ( sec != null )
            {
                master = sec.getMaster();
            }

            if ( master == null )
            {
                throw new IllegalStateException( "Master password is not set in the setting security file: " + file );
            }

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();
            String masterPasswd = cipher.decryptDecorated( master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
            System.out.println( cipher.encryptAndDecorate( passwd, masterPasswd ) );

            throw new ExitException( 0 );
        }
    }

    private int execute( CliRequest cliRequest )
    {
        eventSpyDispatcher.onEvent( cliRequest.request );

        MavenExecutionResult result = maven.execute( cliRequest.request );

        eventSpyDispatcher.onEvent( result );

        eventSpyDispatcher.close();

        if ( result.hasExceptions() )
        {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<String, String>();

            MavenProject project = null;

            for ( Throwable exception : result.getExceptions() )
            {
                ExceptionSummary summary = handler.handleException( exception );

                logSummary( summary, references, "", cliRequest.showErrors );

                if ( project == null && exception instanceof LifecycleExecutionException )
                {
                    project = ( (LifecycleExecutionException) exception ).getProject();
                }
            }

            logger.error( "" );

            if ( !cliRequest.showErrors )
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

            if ( project != null && !project.equals( result.getTopologicallySortedProjects().get( 0 ) ) )
            {
                logger.error( "" );
                logger.error( "After correcting the problems, you can resume the build with the command" );
                logger.error( "  mvn <goals> -rf :" + project.getArtifactId() );
            }

            if ( MavenExecutionRequest.REACTOR_FAIL_NEVER.equals( cliRequest.request.getReactorFailureBehavior() ) )
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

    private void logSummary( ExceptionSummary summary, Map<String, String> references, String indent,
                             boolean showErrors )
    {
        String referenceKey = "";

        if ( StringUtils.isNotEmpty( summary.getReference() ) )
        {
            referenceKey = references.get( summary.getReference() );
            if ( referenceKey == null )
            {
                referenceKey = "[Help " + ( references.size() + 1 ) + "]";
                references.put( summary.getReference(), referenceKey );
            }
        }

        String msg = summary.getMessage();

        if ( StringUtils.isNotEmpty( referenceKey ) )
        {
            if ( msg.indexOf( '\n' ) < 0 )
            {
                msg += " -> " + referenceKey;
            }
            else
            {
                msg += "\n-> " + referenceKey;
            }
        }

        String[] lines = msg.split( "(\r\n)|(\r)|(\n)" );

        for ( int i = 0; i < lines.length; i++ )
        {
            String line = indent + lines[i].trim();

            if ( i == lines.length - 1 && ( showErrors || ( summary.getException() instanceof InternalErrorException ) ) )
            {
                logger.error( line, summary.getException() );
            }
            else
            {
                logger.error( line );
            }
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

    private void settings( CliRequest cliRequest )
        throws Exception
    {
        File userSettingsFile;

        if ( cliRequest.commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsFile = new File( cliRequest.commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) );
            userSettingsFile = resolveFile( userSettingsFile, cliRequest.workingDirectory );

            if ( !userSettingsFile.isFile() )
            {
                throw new FileNotFoundException( "The specified user settings file does not exist: "
                    + userSettingsFile );
            }
        }
        else
        {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        File globalSettingsFile;

        if ( cliRequest.commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) )
        {
            globalSettingsFile =
                new File( cliRequest.commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) );
            globalSettingsFile = resolveFile( globalSettingsFile, cliRequest.workingDirectory );

            if ( !globalSettingsFile.isFile() )
            {
                throw new FileNotFoundException( "The specified global settings file does not exist: "
                    + globalSettingsFile );
            }
        }
        else
        {
            globalSettingsFile = DEFAULT_GLOBAL_SETTINGS_FILE;
        }

        cliRequest.request.setGlobalSettingsFile( globalSettingsFile );
        cliRequest.request.setUserSettingsFile( userSettingsFile );

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile( globalSettingsFile );
        settingsRequest.setUserSettingsFile( userSettingsFile );
        settingsRequest.setSystemProperties( cliRequest.systemProperties );
        settingsRequest.setUserProperties( cliRequest.userProperties );

        eventSpyDispatcher.onEvent( settingsRequest );

        logger.debug( "Reading global settings from "
            + getSettingsLocation( settingsRequest.getGlobalSettingsSource(), settingsRequest.getGlobalSettingsFile() ) );
        logger.debug( "Reading user settings from "
            + getSettingsLocation( settingsRequest.getUserSettingsSource(), settingsRequest.getUserSettingsFile() ) );

        SettingsBuildingResult settingsResult = settingsBuilder.build( settingsRequest );

        eventSpyDispatcher.onEvent( settingsResult );

        executionRequestPopulator.populateFromSettings( cliRequest.request, settingsResult.getEffectiveSettings() );

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

    private Object getSettingsLocation( SettingsSource source, File file )
    {
        if ( source != null )
        {
            return source.getLocation();
        }
        return file;
    }

    private MavenExecutionRequest populateRequest( CliRequest cliRequest )
    {
        MavenExecutionRequest request = cliRequest.request;
        CommandLine commandLine = cliRequest.commandLine;
        String workingDirectory = cliRequest.workingDirectory;
        boolean quiet = cliRequest.quiet;
        boolean showErrors = cliRequest.showErrors;

        String[] deprecatedOptions = { "up", "npu", "cpu", "npr" };
        for ( String deprecatedOption : deprecatedOptions )
        {
            if ( commandLine.hasOption( deprecatedOption ) )
            {
                logger.warn( "Command line option -" + deprecatedOption
                    + " is deprecated and will be removed in future Maven versions." );
            }
        }

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            request.setInteractiveMode( false );
        }

        boolean noSnapshotUpdates = false;
        if ( commandLine.hasOption( CLIManager.SUPRESS_SNAPSHOT_UPDATES ) )
        {
            noSnapshotUpdates = true;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        @SuppressWarnings("unchecked")
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
            String[] profileOptionValues = commandLine.getOptionValues( CLIManager.ACTIVATE_PROFILES );
            if ( profileOptionValues != null )
            {
                for ( int i = 0; i < profileOptionValues.length; ++i )
                {
                    StringTokenizer profileTokens = new StringTokenizer( profileOptionValues[i], "," );

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

        TransferListener transferListener;

        if ( quiet )
        {
            transferListener = new QuietMavenTransferListener();
        }
        else if ( request.isInteractiveMode() )
        {
            transferListener = new ConsoleMavenTransferListener( System.out );
        }
        else
        {
            transferListener = new BatchModeMavenTransferListener( System.out );
        }

        ExecutionListener executionListener = new ExecutionEventLogger( logger );
        executionListener = eventSpyDispatcher.chainListener( executionListener );

        String alternatePomFile = null;
        if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
        {
            alternatePomFile = commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE );
        }

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

        request.setBaseDirectory( baseDirectory ).setGoals( goals )
            .setSystemProperties( cliRequest.systemProperties )
            .setUserProperties( cliRequest.userProperties )
            .setReactorFailureBehavior( reactorFailureBehaviour ) // default: fail fast
            .setRecursive( recursive ) // default: true
            .setShowErrors( showErrors ) // default: false
            .addActiveProfiles( activeProfiles ) // optional
            .addInactiveProfiles( inactiveProfiles ) // optional
            .setExecutionListener( executionListener )
            .setTransferListener( transferListener ) // default: batch mode which goes along with interactive
            .setUpdateSnapshots( updateSnapshots ) // default: false
            .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
            .setGlobalChecksumPolicy( globalChecksumPolicy ) // default: warn
            .setUserToolchainsFile( userToolchainsFile );

        if ( alternatePomFile != null )
        {
            File pom = resolveFile( new File( alternatePomFile ), workingDirectory );

            request.setPom( pom );
        }
        else
        {
            File pom = modelProcessor.locatePom( baseDirectory );

            if ( pom.isFile() )
            {
                request.setPom( pom );
            }
        }

        if ( ( request.getPom() != null ) && ( request.getPom().getParentFile() != null ) )
        {
            request.setBaseDirectory( request.getPom().getParentFile() );
        }

        if ( commandLine.hasOption( CLIManager.RESUME_FROM ) )
        {
            request.setResumeFrom( commandLine.getOptionValue( CLIManager.RESUME_FROM ) );
        }

        if ( commandLine.hasOption( CLIManager.PROJECT_LIST ) )
        {
            String[] values = commandLine.getOptionValues( CLIManager.PROJECT_LIST );
            List<String> projects = new ArrayList<String>();
            for ( int i = 0; i < values.length; i++ )
            {
                String[] tmp = StringUtils.split( values[i], "," );
                projects.addAll( Arrays.asList( tmp ) );
            }
            request.setSelectedProjects( projects );
        }

        if ( commandLine.hasOption( CLIManager.ALSO_MAKE )
                        && !commandLine.hasOption( CLIManager.ALSO_MAKE_DEPENDENTS ) )
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

        final String threadConfiguration = commandLine.hasOption( CLIManager.THREADS )
            ? commandLine.getOptionValue( CLIManager.THREADS )
            : request.getSystemProperties().getProperty(
                MavenCli.THREADS_DEPRECATED ); // TODO: Remove this setting. Note that the int-tests use it

        if ( threadConfiguration != null )
        {
            request.setPerCoreThreadCount( threadConfiguration.contains( "C" ) );
            if ( threadConfiguration.contains( "W" ) )
            {
                LifecycleWeaveBuilder.setWeaveMode( request.getUserProperties() );
            }
            request.setThreadCount( threadConfiguration.replace( "C", "" ).replace( "W", "" ).replace( "auto", "" ) );
        }

        request.setCacheNotFound( true );
        request.setCacheTransferError( false );

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
            return new File( workingDirectory, file.getPath() ).getAbsoluteFile();
        }
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    static void populateProperties( CommandLine commandLine, Properties systemProperties, Properties userProperties )
    {
        EnvironmentUtils.addEnvVars( systemProperties );

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
        
        // ----------------------------------------------------------------------
        // Properties containing info about the currently running version of Maven
        // These override any corresponding properties set on the command line
        // ----------------------------------------------------------------------

        Properties buildProperties = CLIReportingUtils.getBuildProperties();

        String mavenVersion = buildProperties.getProperty( CLIReportingUtils.BUILD_VERSION_PROPERTY );
        systemProperties.setProperty( "maven.version", mavenVersion );

        String mavenBuildVersion = CLIReportingUtils.createMavenVersionString( buildProperties );
        systemProperties.setProperty( "maven.build.version", mavenBuildVersion );
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

            value = property.substring( i + 1 );
        }

        properties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }

    static class CliRequest
    {
        String[] args;
        CommandLine commandLine;
        ClassWorld classWorld;
        String workingDirectory;
        boolean debug;
        boolean quiet;
        boolean showErrors = true;
        PrintStream fileStream;
        Properties userProperties = new Properties();
        Properties systemProperties = new Properties();
        MavenExecutionRequest request;

        CliRequest( String[] args, ClassWorld classWorld )
        {
            this.args = args;
            this.classWorld = classWorld;
            this.request = new DefaultMavenExecutionRequest();
        }
    }

    static class ExitException
        extends Exception
    {

        public int exitCode;

        public ExitException( int exitCode )
        {
            this.exitCode = exitCode;
        }

    }

}
