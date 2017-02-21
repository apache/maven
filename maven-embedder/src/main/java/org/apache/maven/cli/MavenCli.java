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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.maven.BuildAbort;
import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.building.Source;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.cli.event.DefaultEventSpyContext;
import org.apache.maven.cli.event.ExecutionEventLogger;
import org.apache.maven.cli.internal.BootstrapCoreExtensionManager;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Reader;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.apache.maven.cli.logging.Slf4jLoggerManager;
import org.apache.maven.cli.logging.Slf4jStdoutLogger;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

// TODO push all common bits back to plexus cli and prepare for transition to Guice. We don't need 50 ways to make CLIs

/**
 * @author Jason van Zyl
 * @noinspection UseOfSystemOutOrSystemErr, ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    public static final String LOCAL_REPO_PROPERTY = "maven.repo.local";

    public static final String THREADS_DEPRECATED = "maven.threads.experimental";

    public static final String MULTIMODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";

    @SuppressWarnings( "checkstyle:constantname" )
    public static final String userHome = System.getProperty( "user.home" );

    @SuppressWarnings( "checkstyle:constantname" )
    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );

    /**
     * @deprecated use {@link SettingsXmlConfigurationProcessor#DEFAULT_USER_SETTINGS_FILE}
     */
    public static final File DEFAULT_USER_SETTINGS_FILE = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;

    /**
     * @deprecated use {@link SettingsXmlConfigurationProcessor#DEFAULT_GLOBAL_SETTINGS_FILE}
     */
    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
        SettingsXmlConfigurationProcessor.DEFAULT_GLOBAL_SETTINGS_FILE;

    public static final File DEFAULT_USER_TOOLCHAINS_FILE = new File( userMavenConfigurationHome, "toolchains.xml" );

    public static final File DEFAULT_GLOBAL_TOOLCHAINS_FILE =
        new File( System.getProperty( "maven.conf" ), "toolchains.xml" );

    private static final String EXT_CLASS_PATH = "maven.ext.class.path";

    private static final String EXTENSIONS_FILENAME = ".mvn/extensions.xml";

    private static final String MVN_MAVEN_CONFIG = ".mvn/maven.config";

    private ClassWorld classWorld;

    private LoggerManager plexusLoggerManager;

    private ILoggerFactory slf4jLoggerFactory;

    private Logger slf4jLogger;

    private EventSpyDispatcher eventSpyDispatcher;

    private ModelProcessor modelProcessor;

    private Maven maven;

    private MavenExecutionRequestPopulator executionRequestPopulator;

    private ToolchainsBuilder toolchainsBuilder;

    private DefaultSecDispatcher dispatcher;

    private Map<String, ConfigurationProcessor> configurationProcessors;

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

    /**
     * @noinspection ConfusingMainMethod
     */
    public static int main( String[] args, ClassWorld classWorld )
    {
        MavenCli cli = new MavenCli();

        MessageUtils.systemInstall();
        int result = cli.doMain( new CliRequest( args, classWorld ) );
        MessageUtils.systemUninstall();

        return result;
    }

    // TODO need to externalize CliRequest
    public static int doMain( String[] args, ClassWorld classWorld )
    {
        MavenCli cli = new MavenCli();
        return cli.doMain( new CliRequest( args, classWorld ) );
    }

    /**
     * This supports painless invocation by the Verifier during embedded execution of the core ITs.
     * See <a href="http://maven.apache.org/shared/maven-verifier/xref/org/apache/maven/it/Embedded3xLauncher.html">
     * <code>Embedded3xLauncher</code> in <code>maven-verifier</code></a>
     */
    public int doMain( String[] args, String workingDirectory, PrintStream stdout, PrintStream stderr )
    {
        PrintStream oldout = System.out;
        PrintStream olderr = System.err;

        final Set<String> realms;
        if ( classWorld != null )
        {
            realms = new HashSet<>();
            for ( ClassRealm realm : classWorld.getRealms() )
            {
                realms.add( realm.getId() );
            }
        }
        else
        {
            realms = Collections.emptySet();
        }

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
            if ( classWorld != null )
            {
                for ( ClassRealm realm : new ArrayList<>( classWorld.getRealms() ) )
                {
                    String realmId = realm.getId();
                    if ( !realms.contains( realmId ) )
                    {
                        try
                        {
                            classWorld.disposeRealm( realmId );
                        }
                        catch ( NoSuchRealmException ignored )
                        {
                            // can't happen
                        }
                    }
                }
            }
            System.setOut( oldout );
            System.setErr( olderr );
        }
    }

    // TODO need to externalize CliRequest
    public int doMain( CliRequest cliRequest )
    {
        PlexusContainer localContainer = null;
        try
        {
            initialize( cliRequest );
            cli( cliRequest );
            logging( cliRequest );
            version( cliRequest );
            properties( cliRequest );
            localContainer = container( cliRequest );
            commands( cliRequest );
            configure( cliRequest );
            toolchains( cliRequest );
            populateRequest( cliRequest );
            encryption( cliRequest );
            repository( cliRequest );
            return execute( cliRequest );
        }
        catch ( ExitException e )
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
            CLIReportingUtils.showError( slf4jLogger, "ABORTED", e, cliRequest.showErrors );

            return 2;
        }
        catch ( Exception e )
        {
            CLIReportingUtils.showError( slf4jLogger, "Error executing Maven.", e, cliRequest.showErrors );

            return 1;
        }
        finally
        {
            if ( localContainer != null )
            {
                localContainer.dispose();
            }
        }
    }

    void initialize( CliRequest cliRequest )
        throws ExitException
    {
        if ( cliRequest.workingDirectory == null )
        {
            cliRequest.workingDirectory = System.getProperty( "user.dir" );
        }

        if ( cliRequest.multiModuleProjectDirectory == null )
        {
            String basedirProperty = System.getProperty( MULTIMODULE_PROJECT_DIRECTORY );
            if ( basedirProperty == null )
            {
                System.err.format(
                    "-D%s system property is not set.", MULTIMODULE_PROJECT_DIRECTORY );
                throw new ExitException( 1 );
            }
            File basedir = basedirProperty != null ? new File( basedirProperty ) : new File( "" );
            try
            {
                cliRequest.multiModuleProjectDirectory = basedir.getCanonicalFile();
            }
            catch ( IOException e )
            {
                cliRequest.multiModuleProjectDirectory = basedir.getAbsoluteFile();
            }
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

    void cli( CliRequest cliRequest )
        throws Exception
    {
        //
        // Parsing errors can happen during the processing of the arguments and we prefer not having to check if
        // the logger is null and construct this so we can use an SLF4J logger everywhere.
        //
        slf4jLogger = new Slf4jStdoutLogger();

        CLIManager cliManager = new CLIManager();

        List<String> args = new ArrayList<>();

        try
        {
            File configFile = new File( cliRequest.multiModuleProjectDirectory, MVN_MAVEN_CONFIG );

            if ( configFile.isFile() )
            {
                for ( String arg : Files.toString( configFile, Charsets.UTF_8 ).split( "\\s+" ) )
                {
                    if ( !arg.isEmpty() )
                    {
                        args.add( arg );
                    }
                }

                CommandLine config = cliManager.parse( args.toArray( new String[args.size()] ) );
                List<?> unrecongized = config.getArgList();
                if ( !unrecongized.isEmpty() )
                {
                    throw new ParseException( "Unrecognized maven.config entries: " + unrecongized );
                }
            }
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse maven.config: " + e.getMessage() );
            cliManager.displayHelp( System.out );
            throw e;
        }

        try
        {
            int index = 0;
            for ( String arg : cliRequest.args )
            {
                if ( arg.startsWith( "-D" ) )
                {
                    // a property definition so needs to come last so that the last property wins
                    args.add( arg );
                }
                else
                {
                    // not a property definition so needs to come first to override maven.config
                    args.add( index++, arg );
                }
            }
            cliRequest.commandLine = cliManager.parse( args.toArray( new String[args.size()] ) );
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );
            cliManager.displayHelp( System.out );
            throw e;
        }

        if ( cliRequest.commandLine.hasOption( CLIManager.HELP ) )
        {
            cliManager.displayHelp( System.out );
            throw new ExitException( 0 );
        }

        if ( cliRequest.commandLine.hasOption( CLIManager.VERSION ) )
        {
            System.out.println( CLIReportingUtils.showVersion() );
            throw new ExitException( 0 );
        }
    }

    /**
     * configure logging
     */
    private void logging( CliRequest cliRequest )
    {
        cliRequest.debug = cliRequest.commandLine.hasOption( CLIManager.DEBUG );
        cliRequest.quiet = !cliRequest.debug && cliRequest.commandLine.hasOption( CLIManager.QUIET );
        cliRequest.showErrors = cliRequest.debug || cliRequest.commandLine.hasOption( CLIManager.ERRORS );

        slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration( slf4jLoggerFactory );

        if ( cliRequest.debug )
        {
            cliRequest.request.setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_DEBUG );
            slf4jConfiguration.setRootLoggerLevel( Slf4jConfiguration.Level.DEBUG );
        }
        else if ( cliRequest.quiet )
        {
            cliRequest.request.setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_ERROR );
            slf4jConfiguration.setRootLoggerLevel( Slf4jConfiguration.Level.ERROR );
        }
        // else fall back to default log level specified in conf
        // see https://issues.apache.org/jira/browse/MNG-2570

        if ( cliRequest.commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            MessageUtils.setColorEnabled( false );
        }

        if ( cliRequest.commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            File logFile = new File( cliRequest.commandLine.getOptionValue( CLIManager.LOG_FILE ) );
            logFile = resolveFile( logFile, cliRequest.workingDirectory );

            MessageUtils.setColorEnabled( false );

            // redirect stdout and stderr to file
            try
            {
                PrintStream ps = new PrintStream( new FileOutputStream( logFile ) );
                System.setOut( ps );
                System.setErr( ps );
            }
            catch ( FileNotFoundException e )
            {
                //
                // Ignore
                //
            }
        }

        slf4jConfiguration.activate();

        plexusLoggerManager = new Slf4jLoggerManager();
        slf4jLogger = slf4jLoggerFactory.getLogger( this.getClass().getName() );
    }

    private void version( CliRequest cliRequest )
    {
        if ( cliRequest.debug || cliRequest.commandLine.hasOption( CLIManager.SHOW_VERSION ) )
        {
            System.out.println( CLIReportingUtils.showVersion() );
        }
    }

    private void commands( CliRequest cliRequest )
    {
        if ( cliRequest.showErrors )
        {
            slf4jLogger.info( "Error stacktraces are turned on." );
        }

        if ( MavenExecutionRequest.CHECKSUM_POLICY_WARN.equals( cliRequest.request.getGlobalChecksumPolicy() ) )
        {
            slf4jLogger.info( "Disabling strict checksum verification on all artifact downloads." );
        }
        else if ( MavenExecutionRequest.CHECKSUM_POLICY_FAIL.equals( cliRequest.request.getGlobalChecksumPolicy() ) )
        {
            slf4jLogger.info( "Enabling strict checksum verification on all artifact downloads." );
        }

        if ( slf4jLogger.isDebugEnabled() )
        {
            slf4jLogger.debug( "Message scheme: " + ( MessageUtils.isColorEnabled() ? "color" : "plain" ) );
            if ( MessageUtils.isColorEnabled() )
            {
                MessageBuilder buff = MessageUtils.buffer();
                buff.a( "Message styles: " );
                buff.debug( "debug" ).a( ' ' );
                buff.info( "info" ).a( ' ' );
                buff.warning( "warning" ).a( ' ' );
                buff.error( "error" ).a( ' ' );
                buff.success( "success" ).a( ' ' );
                buff.failure( "failure" ).a( ' ' );
                buff.strong( "strong" ).a( ' ' );
                buff.mojo( "mojo" ).a( ' ' );
                buff.project( "project" );
                slf4jLogger.debug( buff.toString() );
            }
        }
    }

    //Needed to make this method package visible to make writing a unit test possible
    //Maybe it's better to move some of those methods to separate class (SoC).
    void properties( CliRequest cliRequest )
    {
        populateProperties( cliRequest.commandLine, cliRequest.systemProperties, cliRequest.userProperties );
    }

    private PlexusContainer container( CliRequest cliRequest )
        throws Exception
    {
        if ( cliRequest.classWorld == null )
        {
            cliRequest.classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
        }

        ClassRealm coreRealm = cliRequest.classWorld.getClassRealm( "plexus.core" );
        if ( coreRealm == null )
        {
            coreRealm = cliRequest.classWorld.getRealms().iterator().next();
        }

        List<File> extClassPath = parseExtClasspath( cliRequest );

        CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom( coreRealm );
        List<CoreExtensionEntry> extensions =
            loadCoreExtensions( cliRequest, coreRealm, coreEntry.getExportedArtifacts() );

        ClassRealm containerRealm = setupContainerRealm( cliRequest.classWorld, coreRealm, extClassPath, extensions );

        ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld( cliRequest.classWorld ).setRealm(
            containerRealm ).setClassPathScanning( PlexusConstants.SCANNING_INDEX ).setAutoWiring( true ).setName(
            "maven" );

        Set<String> exportedArtifacts = new HashSet<>( coreEntry.getExportedArtifacts() );
        Set<String> exportedPackages = new HashSet<>( coreEntry.getExportedPackages() );
        for ( CoreExtensionEntry extension : extensions )
        {
            exportedArtifacts.addAll( extension.getExportedArtifacts() );
            exportedPackages.addAll( extension.getExportedPackages() );
        }

        final CoreExports exports = new CoreExports( containerRealm, exportedArtifacts, exportedPackages );

        DefaultPlexusContainer container = new DefaultPlexusContainer( cc, new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind( ILoggerFactory.class ).toInstance( slf4jLoggerFactory );
                bind( CoreExports.class ).toInstance( exports );
            }
        } );

        // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
        container.setLookupRealm( null );

        container.setLoggerManager( plexusLoggerManager );

        for ( CoreExtensionEntry extension : extensions )
        {
            container.discoverComponents( extension.getClassRealm() );
        }

        customizeContainer( container );

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
        slf4jLogger = slf4jLoggerFactory.getLogger( this.getClass().getName() );

        maven = container.lookup( Maven.class );

        executionRequestPopulator = container.lookup( MavenExecutionRequestPopulator.class );

        modelProcessor = createModelProcessor( container );

        configurationProcessors = container.lookupMap( ConfigurationProcessor.class );

        toolchainsBuilder = container.lookup( ToolchainsBuilder.class );

        dispatcher = (DefaultSecDispatcher) container.lookup( SecDispatcher.class, "maven" );

        return container;
    }

    private List<CoreExtensionEntry> loadCoreExtensions( CliRequest cliRequest, ClassRealm containerRealm,
                                                         Set<String> providedArtifacts )
    {
        if ( cliRequest.multiModuleProjectDirectory == null )
        {
            return Collections.emptyList();
        }

        File extensionsFile = new File( cliRequest.multiModuleProjectDirectory, EXTENSIONS_FILENAME );
        if ( !extensionsFile.isFile() )
        {
            return Collections.emptyList();
        }

        try
        {
            List<CoreExtension> extensions = readCoreExtensionsDescriptor( extensionsFile );
            if ( extensions.isEmpty() )
            {
                return Collections.emptyList();
            }

            ContainerConfiguration cc = new DefaultContainerConfiguration() //
                .setClassWorld( cliRequest.classWorld ) //
                .setRealm( containerRealm ) //
                .setClassPathScanning( PlexusConstants.SCANNING_INDEX ) //
                .setAutoWiring( true ) //
                .setName( "maven" );

            DefaultPlexusContainer container = new DefaultPlexusContainer( cc, new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind( ILoggerFactory.class ).toInstance( slf4jLoggerFactory );
                }
            } );

            try
            {
                container.setLookupRealm( null );

                container.setLoggerManager( plexusLoggerManager );

                container.getLoggerManager().setThresholds( cliRequest.request.getLoggingLevel() );

                Thread.currentThread().setContextClassLoader( container.getContainerRealm() );

                executionRequestPopulator = container.lookup( MavenExecutionRequestPopulator.class );

                configurationProcessors = container.lookupMap( ConfigurationProcessor.class );

                configure( cliRequest );

                MavenExecutionRequest request = DefaultMavenExecutionRequest.copy( cliRequest.request );

                request = populateRequest( cliRequest, request );

                request = executionRequestPopulator.populateDefaults( request );

                BootstrapCoreExtensionManager resolver = container.lookup( BootstrapCoreExtensionManager.class );

                return resolver.loadCoreExtensions( request, providedArtifacts, extensions );
            }
            finally
            {
                executionRequestPopulator = null;
                container.dispose();
            }
        }
        catch ( RuntimeException e )
        {
            // runtime exceptions are most likely bugs in maven, let them bubble up to the user
            throw e;
        }
        catch ( Exception e )
        {
            slf4jLogger.warn( "Failed to read extensions descriptor " + extensionsFile + ": " + e.getMessage() );
        }
        return Collections.emptyList();
    }

    private List<CoreExtension> readCoreExtensionsDescriptor( File extensionsFile )
        throws IOException, XmlPullParserException
    {
        CoreExtensionsXpp3Reader parser = new CoreExtensionsXpp3Reader();

        try ( InputStream is = new BufferedInputStream( new FileInputStream( extensionsFile ) ) )
        {

            return parser.read( is ).getExtensions();
        }

    }

    private ClassRealm setupContainerRealm( ClassWorld classWorld, ClassRealm coreRealm, List<File> extClassPath,
                                            List<CoreExtensionEntry> extensions )
        throws Exception
    {
        if ( !extClassPath.isEmpty() || !extensions.isEmpty() )
        {
            ClassRealm extRealm = classWorld.newRealm( "maven.ext", null );

            extRealm.setParentRealm( coreRealm );

            slf4jLogger.debug( "Populating class realm " + extRealm.getId() );

            for ( File file : extClassPath )
            {
                slf4jLogger.debug( "  Included " + file );

                extRealm.addURL( file.toURI().toURL() );
            }

            for ( CoreExtensionEntry entry : reverse( extensions ) )
            {
                Set<String> exportedPackages = entry.getExportedPackages();
                ClassRealm realm = entry.getClassRealm();
                for ( String exportedPackage : exportedPackages )
                {
                    extRealm.importFrom( realm, exportedPackage );
                }
                if ( exportedPackages.isEmpty() )
                {
                    // sisu uses realm imports to establish component visibility
                    extRealm.importFrom( realm, realm.getId() );
                }
            }

            return extRealm;
        }

        return coreRealm;
    }

    private static <T> List<T> reverse( List<T> list )
    {
        List<T> copy = new ArrayList<>( list );
        Collections.reverse( copy );
        return copy;
    }

    private List<File> parseExtClasspath( CliRequest cliRequest )
    {
        String extClassPath = cliRequest.userProperties.getProperty( EXT_CLASS_PATH );
        if ( extClassPath == null )
        {
            extClassPath = cliRequest.systemProperties.getProperty( EXT_CLASS_PATH );
        }

        List<File> jars = new ArrayList<>();

        if ( StringUtils.isNotEmpty( extClassPath ) )
        {
            for ( String jar : StringUtils.split( extClassPath, File.pathSeparator ) )
            {
                File file = resolveFile( new File( jar ), cliRequest.workingDirectory );

                slf4jLogger.debug( "  Included " + file );

                jars.add( file );
            }
        }

        return jars;
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

            if ( passwd == null )
            {
                Console cons = System.console();
                char[] password = ( cons == null ) ? null : cons.readPassword( "Master password: " );
                if ( password != null )
                {
                    // Cipher uses Strings
                    passwd = String.copyValueOf( password );

                    // Sun/Oracle advises to empty the char array
                    java.util.Arrays.fill( password, ' ' );
                }
            }

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();

            System.out.println(
                cipher.encryptAndDecorate( passwd, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION ) );

            throw new ExitException( 0 );
        }
        else if ( cliRequest.commandLine.hasOption( CLIManager.ENCRYPT_PASSWORD ) )
        {
            String passwd = cliRequest.commandLine.getOptionValue( CLIManager.ENCRYPT_PASSWORD );

            if ( passwd == null )
            {
                Console cons = System.console();
                char[] password = ( cons == null ) ? null : cons.readPassword( "Password: " );
                if ( password != null )
                {
                    // Cipher uses Strings
                    passwd = String.copyValueOf( password );

                    // Sun/Oracle advises to empty the char array
                    java.util.Arrays.fill( password, ' ' );
                }
            }

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

    private void repository( CliRequest cliRequest )
        throws Exception
    {
        if ( cliRequest.commandLine.hasOption( CLIManager.LEGACY_LOCAL_REPOSITORY ) || Boolean.getBoolean(
            "maven.legacyLocalRepo" ) )
        {
            cliRequest.request.setUseLegacyLocalRepository( true );
        }
    }

    private int execute( CliRequest cliRequest )
        throws MavenExecutionRequestPopulationException
    {
        MavenExecutionRequest request = executionRequestPopulator.populateDefaults( cliRequest.request );

        eventSpyDispatcher.onEvent( request );

        MavenExecutionResult result = maven.execute( request );

        eventSpyDispatcher.onEvent( result );

        eventSpyDispatcher.close();

        if ( result.hasExceptions() )
        {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<>();

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

            slf4jLogger.error( "" );

            if ( !cliRequest.showErrors )
            {
                slf4jLogger.error( "To see the full stack trace of the errors, re-run Maven with the "
                    + buffer().strong( "-e" ) + " switch." );
            }
            if ( !slf4jLogger.isDebugEnabled() )
            {
                slf4jLogger.error( "Re-run Maven using the " + buffer().strong( "-X" )
                    + " switch to enable full debug logging." );
            }

            if ( !references.isEmpty() )
            {
                slf4jLogger.error( "" );
                slf4jLogger.error( "For more information about the errors and possible solutions"
                                       + ", please read the following articles:" );

                for ( Map.Entry<String, String> entry : references.entrySet() )
                {
                    slf4jLogger.error( buffer().strong( entry.getValue() ) + " " + entry.getKey() );
                }
            }

            if ( project != null && !project.equals( result.getTopologicallySortedProjects().get( 0 ) ) )
            {
                slf4jLogger.error( "" );
                slf4jLogger.error( "After correcting the problems, you can resume the build with the command" );
                slf4jLogger.error( buffer().a( "  " ).strong( "mvn <goals> -rf :"
                                + project.getArtifactId() ).toString() );
            }

            if ( MavenExecutionRequest.REACTOR_FAIL_NEVER.equals( cliRequest.request.getReactorFailureBehavior() ) )
            {
                slf4jLogger.info( "Build failures were ignored." );

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
                msg += " -> " + buffer().strong( referenceKey );
            }
            else
            {
                msg += "\n-> " + buffer().strong( referenceKey );
            }
        }

        String[] lines = msg.split( "(\r\n)|(\r)|(\n)" );
        String currentColor = "";

        for ( int i = 0; i < lines.length; i++ )
        {
            // add eventual current color inherited from previous line 
            String line = currentColor + lines[i];

            // look for last ANSI escape sequence to check if nextColor
            Matcher matcher = LAST_ANSI_SEQUENCE.matcher( line );
            String nextColor = "";
            if ( matcher.find() )
            {
                nextColor = matcher.group( 1 );
                if ( ANSI_RESET.equals( nextColor ) )
                {
                    // last ANSI escape code is reset: no next color
                    nextColor = "";
                }
            }

            // effective line, with indent and reset if end is colored
            line = indent + line + ( "".equals( nextColor ) ? "" : ANSI_RESET );

            if ( ( i == lines.length - 1 ) && ( showErrors
                || ( summary.getException() instanceof InternalErrorException ) ) )
            {
                slf4jLogger.error( line, summary.getException() );
            }
            else
            {
                slf4jLogger.error( line );
            }

            currentColor = nextColor;
        }

        indent += "  ";

        for ( ExceptionSummary child : summary.getChildren() )
        {
            logSummary( child, references, indent, showErrors );
        }
    }

    private static final Pattern LAST_ANSI_SEQUENCE = Pattern.compile( "(\u001B\\[[;\\d]*[ -/]*[@-~])[^\u001B]*$" );

    private static final String ANSI_RESET = "\u001B\u005Bm";

    private void configure( CliRequest cliRequest )
        throws Exception
    {
        //
        // This is not ideal but there are events specifically for configuration from the CLI which I don't
        // believe are really valid but there are ITs which assert the right events are published so this
        // needs to be supported so the EventSpyDispatcher needs to be put in the CliRequest so that
        // it can be accessed by configuration processors.
        //
        cliRequest.request.setEventSpyDispatcher( eventSpyDispatcher );

        //
        // We expect at most 2 implementations to be available. The SettingsXmlConfigurationProcessor implementation
        // is always available in the core and likely always will be, but we may have another ConfigurationProcessor
        // present supplied by the user. The rule is that we only allow the execution of one ConfigurationProcessor.
        // If there is more than one then we execute the one supplied by the user, otherwise we execute the
        // the default SettingsXmlConfigurationProcessor.
        //
        int userSuppliedConfigurationProcessorCount = configurationProcessors.size() - 1;

        if ( userSuppliedConfigurationProcessorCount == 0 )
        {
            //
            // Our settings.xml source is historically how we have configured Maven from the CLI so we are going to
            // have to honour its existence forever. So let's run it.
            //
            configurationProcessors.get( SettingsXmlConfigurationProcessor.HINT ).process( cliRequest );
        }
        else if ( userSuppliedConfigurationProcessorCount == 1 )
        {
            //
            // Run the user supplied ConfigurationProcessor
            //
            for ( Entry<String, ConfigurationProcessor> entry : configurationProcessors.entrySet() )
            {
                String hint = entry.getKey();
                if ( !hint.equals( SettingsXmlConfigurationProcessor.HINT ) )
                {
                    ConfigurationProcessor configurationProcessor = entry.getValue();
                    configurationProcessor.process( cliRequest );
                }
            }
        }
        else if ( userSuppliedConfigurationProcessorCount > 1 )
        {
            //
            // There are too many ConfigurationProcessors so we don't know which one to run so report the error.
            //
            StringBuilder sb = new StringBuilder(
                String.format( "\nThere can only be one user supplied ConfigurationProcessor, there are %s:\n\n",
                               userSuppliedConfigurationProcessorCount ) );
            for ( Entry<String, ConfigurationProcessor> entry : configurationProcessors.entrySet() )
            {
                String hint = entry.getKey();
                if ( !hint.equals( SettingsXmlConfigurationProcessor.HINT ) )
                {
                    ConfigurationProcessor configurationProcessor = entry.getValue();
                    sb.append( String.format( "%s\n", configurationProcessor.getClass().getName() ) );
                }
            }
            sb.append( String.format( "\n" ) );
            throw new Exception( sb.toString() );
        }
    }

    private void toolchains( CliRequest cliRequest )
        throws Exception
    {
        File userToolchainsFile;

        if ( cliRequest.commandLine.hasOption( CLIManager.ALTERNATE_USER_TOOLCHAINS ) )
        {
            userToolchainsFile =
                new File( cliRequest.commandLine.getOptionValue( CLIManager.ALTERNATE_USER_TOOLCHAINS ) );
            userToolchainsFile = resolveFile( userToolchainsFile, cliRequest.workingDirectory );

            if ( !userToolchainsFile.isFile() )
            {
                throw new FileNotFoundException(
                    "The specified user toolchains file does not exist: " + userToolchainsFile );
            }
        }
        else
        {
            userToolchainsFile = DEFAULT_USER_TOOLCHAINS_FILE;
        }

        File globalToolchainsFile;

        if ( cliRequest.commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS ) )
        {
            globalToolchainsFile =
                new File( cliRequest.commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS ) );
            globalToolchainsFile = resolveFile( globalToolchainsFile, cliRequest.workingDirectory );

            if ( !globalToolchainsFile.isFile() )
            {
                throw new FileNotFoundException(
                    "The specified global toolchains file does not exist: " + globalToolchainsFile );
            }
        }
        else
        {
            globalToolchainsFile = DEFAULT_GLOBAL_TOOLCHAINS_FILE;
        }

        cliRequest.request.setGlobalToolchainsFile( globalToolchainsFile );
        cliRequest.request.setUserToolchainsFile( userToolchainsFile );

        DefaultToolchainsBuildingRequest toolchainsRequest = new DefaultToolchainsBuildingRequest();
        if ( globalToolchainsFile.isFile() )
        {
            toolchainsRequest.setGlobalToolchainsSource( new FileSource( globalToolchainsFile ) );
        }
        if ( userToolchainsFile.isFile() )
        {
            toolchainsRequest.setUserToolchainsSource( new FileSource( userToolchainsFile ) );
        }

        eventSpyDispatcher.onEvent( toolchainsRequest );

        slf4jLogger.debug(
            "Reading global toolchains from " + getLocation( toolchainsRequest.getGlobalToolchainsSource(),
                                                             globalToolchainsFile ) );
        slf4jLogger.debug( "Reading user toolchains from " + getLocation( toolchainsRequest.getUserToolchainsSource(),
                                                                          userToolchainsFile ) );

        ToolchainsBuildingResult toolchainsResult = toolchainsBuilder.build( toolchainsRequest );

        eventSpyDispatcher.onEvent( toolchainsRequest );

        executionRequestPopulator.populateFromToolchains( cliRequest.request,
                                                          toolchainsResult.getEffectiveToolchains() );

        if ( !toolchainsResult.getProblems().isEmpty() && slf4jLogger.isWarnEnabled() )
        {
            slf4jLogger.warn( "" );
            slf4jLogger.warn( "Some problems were encountered while building the effective toolchains" );

            for ( Problem problem : toolchainsResult.getProblems() )
            {
                slf4jLogger.warn( problem.getMessage() + " @ " + problem.getLocation() );
            }

            slf4jLogger.warn( "" );
        }
    }

    private Object getLocation( Source source, File defaultLocation )
    {
        if ( source != null )
        {
            return source.getLocation();
        }
        return defaultLocation;
    }

    private MavenExecutionRequest populateRequest( CliRequest cliRequest )
    {
        return populateRequest( cliRequest, cliRequest.request );
    }

    private MavenExecutionRequest populateRequest( CliRequest cliRequest, MavenExecutionRequest request )
    {
        CommandLine commandLine = cliRequest.commandLine;
        String workingDirectory = cliRequest.workingDirectory;
        boolean quiet = cliRequest.quiet;
        boolean showErrors = cliRequest.showErrors;

        String[] deprecatedOptions = { "up", "npu", "cpu", "npr" };
        for ( String deprecatedOption : deprecatedOptions )
        {
            if ( commandLine.hasOption( deprecatedOption ) )
            {
                slf4jLogger.warn( "Command line option -" + deprecatedOption
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

        @SuppressWarnings( "unchecked" ) List<String> goals = commandLine.getArgList();

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

        List<String> activeProfiles = new ArrayList<>();

        List<String> inactiveProfiles = new ArrayList<>();

        if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
        {
            String[] profileOptionValues = commandLine.getOptionValues( CLIManager.ACTIVATE_PROFILES );
            if ( profileOptionValues != null )
            {
                for ( String profileOptionValue : profileOptionValues )
                {
                    StringTokenizer profileTokens = new StringTokenizer( profileOptionValue, "," );

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
        else if ( request.isInteractiveMode() && !cliRequest.commandLine.hasOption( CLIManager.LOG_FILE ) )
        {
            //
            // If we're logging to a file then we don't want the console transfer listener as it will spew
            // download progress all over the place
            //
            transferListener = getConsoleTransferListener( cliRequest.commandLine.hasOption( CLIManager.DEBUG ) );
        }
        else
        {
            transferListener = getBatchTransferListener();
        }

        ExecutionListener executionListener = new ExecutionEventLogger();
        if ( eventSpyDispatcher != null )
        {
            executionListener = eventSpyDispatcher.chainListener( executionListener );
        }

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

        request.setBaseDirectory( baseDirectory ).setGoals( goals ).setSystemProperties(
            cliRequest.systemProperties ).setUserProperties( cliRequest.userProperties ).setReactorFailureBehavior(
            reactorFailureBehaviour ) // default: fail fast
            .setRecursive( recursive ) // default: true
            .setShowErrors( showErrors ) // default: false
            .addActiveProfiles( activeProfiles ) // optional
            .addInactiveProfiles( inactiveProfiles ) // optional
            .setExecutionListener( executionListener ).setTransferListener(
            transferListener ) // default: batch mode which goes along with interactive
            .setUpdateSnapshots( updateSnapshots ) // default: false
            .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
            .setGlobalChecksumPolicy( globalChecksumPolicy ) // default: warn
            .setMultiModuleProjectDirectory( cliRequest.multiModuleProjectDirectory );

        if ( alternatePomFile != null )
        {
            File pom = resolveFile( new File( alternatePomFile ), workingDirectory );
            if ( pom.isDirectory() )
            {
                pom = new File( pom, "pom.xml" );
            }

            request.setPom( pom );
        }
        else if ( modelProcessor != null )
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
            String[] projectOptionValues = commandLine.getOptionValues( CLIManager.PROJECT_LIST );

            List<String> inclProjects = new ArrayList<>();
            List<String> exclProjects = new ArrayList<>();

            if ( projectOptionValues != null )
            {
                for ( String projectOptionValue : projectOptionValues )
                {
                    StringTokenizer projectTokens = new StringTokenizer( projectOptionValue, "," );

                    while ( projectTokens.hasMoreTokens() )
                    {
                        String projectAction = projectTokens.nextToken().trim();

                        if ( projectAction.startsWith( "-" ) || projectAction.startsWith( "!" ) )
                        {
                            exclProjects.add( projectAction.substring( 1 ) );
                        }
                        else if ( projectAction.startsWith( "+" ) )
                        {
                            inclProjects.add( projectAction.substring( 1 ) );
                        }
                        else
                        {
                            inclProjects.add( projectAction );
                        }
                    }
                }
            }

            request.setSelectedProjects( inclProjects );
            request.setExcludedProjects( exclProjects );
        }

        if ( commandLine.hasOption( CLIManager.ALSO_MAKE ) && !commandLine.hasOption(
            CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( MavenExecutionRequest.REACTOR_MAKE_UPSTREAM );
        }
        else if ( !commandLine.hasOption( CLIManager.ALSO_MAKE ) && commandLine.hasOption(
            CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM );
        }
        else if ( commandLine.hasOption( CLIManager.ALSO_MAKE ) && commandLine.hasOption(
            CLIManager.ALSO_MAKE_DEPENDENTS ) )
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

        request.setCacheNotFound( true );
        request.setCacheTransferError( false );

        //
        // Builder, concurrency and parallelism
        //
        // We preserve the existing methods for builder selection which is to look for various inputs in the threading
        // configuration. We don't have an easy way to allow a pluggable builder to provide its own configuration
        // parameters but this is sufficient for now. Ultimately we want components like Builders to provide a way to
        // extend the command line to accept its own configuration parameters.
        //
        final String threadConfiguration = commandLine.hasOption( CLIManager.THREADS )
            ? commandLine.getOptionValue( CLIManager.THREADS )
            : request.getSystemProperties().getProperty(
                MavenCli.THREADS_DEPRECATED ); // TODO Remove this setting. Note that the int-tests use it

        if ( threadConfiguration != null )
        {
            //
            // Default to the standard multithreaded builder
            //
            request.setBuilderId( "multithreaded" );

            if ( threadConfiguration.contains( "C" ) )
            {
                request.setDegreeOfConcurrency( calculateDegreeOfConcurrencyWithCoreMultiplier( threadConfiguration ) );
            }
            else
            {
                request.setDegreeOfConcurrency( Integer.valueOf( threadConfiguration ) );
            }
        }

        //
        // Allow the builder to be overridden by the user if requested. The builders are now pluggable.
        //
        if ( commandLine.hasOption( CLIManager.BUILDER ) )
        {
            request.setBuilderId( commandLine.getOptionValue( CLIManager.BUILDER ) );
        }

        return request;
    }

    int calculateDegreeOfConcurrencyWithCoreMultiplier( String threadConfiguration )
    {
        int procs = Runtime.getRuntime().availableProcessors();
        return (int) ( Float.valueOf( threadConfiguration.replace( "C", "" ) ) * procs );
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
                for ( String defStr : defStrs )
                {
                    setCliProperty( defStr, userProperties );
                }
            }
        }

        SystemProperties.addSystemProperties( systemProperties );

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

        int i = property.indexOf( '=' );

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

    static class ExitException
        extends Exception
    {
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public int exitCode;

        public ExitException( int exitCode )
        {
            this.exitCode = exitCode;
        }
    }

    //
    // Customizations available via the CLI
    //

    protected TransferListener getConsoleTransferListener( boolean printResourceNames )
    {
        return new ConsoleMavenTransferListener( System.out, printResourceNames );
    }

    protected TransferListener getBatchTransferListener()
    {
        return new Slf4jMavenTransferListener();
    }

    protected void customizeContainer( PlexusContainer container )
    {
    }

    protected ModelProcessor createModelProcessor( PlexusContainer container )
        throws ComponentLookupException
    {
        return container.lookup( ModelProcessor.class );
    }
}
