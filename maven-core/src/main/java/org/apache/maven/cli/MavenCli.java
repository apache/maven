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
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    /** @deprecated use {@link Os#OS_NAME} */
    public static final String OS_NAME = Os.OS_NAME;

    /** @deprecated use {@link Os#OS_ARCH} */
    public static final String OS_ARCH = Os.OS_ARCH;

    /** @deprecated use {@link Os#OS_VERSION} */
    public static final String OS_VERSION = Os.OS_VERSION;

    private static Embedder embedder;

    /**
     * @deprecated Use {@link Main#main(String[])} instead.
     */
    public static void main( String[] args )
    {
        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );

        int result = main( args, classWorld );

        System.exit( result );
    }

    /**
     * @noinspection ConfusingMainMethod
     */
    public static int main( String[] args, ClassWorld classWorld )
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
            showVersion();

            return 0;
        }
        else if ( debug || commandLine.hasOption( CLIManager.SHOW_VERSION ) )
        {
            showVersion();
        }

        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        // Make sure the Maven home directory is an absolute path to save us from confusion with say drive-relative
        // Windows paths.
        String mavenHome = System.getProperty( "maven.home" );
        if ( mavenHome != null )
        {
            System.setProperty( "maven.home", new File( mavenHome ).getAbsolutePath() );
        }

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        embedder = new Embedder();

        try
        {
            embedder.start( classWorld );
        }
        catch ( PlexusContainerException e )
        {
            showFatalError( "Unable to start the embedded plexus container", e, showErrors );

            return 1;
        }

        // wraps the following code to ensure the embedder is stopped no matter what else happens.
        try
        {
            // ----------------------------------------------------------------------
            // The execution properties need to be created before the settings
            // are constructed.
            // ----------------------------------------------------------------------

            Properties executionProperties = new Properties();
            Properties userProperties = new Properties();
            populateProperties( commandLine, executionProperties, userProperties );

            Settings settings;

            try
            {
                settings = buildSettings( commandLine );
            }
            catch ( SettingsConfigurationException e )
            {
                showError( "Error reading settings.xml: " + e.getMessage(), e, showErrors );

                return 1;
            }
            catch ( ComponentLookupException e )
            {
                showFatalError( "Unable to read settings.xml", e, showErrors );

                return 1;
            }

            DefaultSecDispatcher dispatcher;
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

                    dispatcher = (DefaultSecDispatcher) embedder.lookup( SecDispatcher.ROLE );
                    String configurationFile = dispatcher.getConfigurationFile();
                    if ( configurationFile.startsWith( "~" ) )
                    {
                        configurationFile = System.getProperty( "user.home" ) + configurationFile.substring( 1 );
                    }
                    String file = System.getProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile );
                    embedder.release( dispatcher );

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
                showFatalError( "Error encrypting password: " + e.getMessage(), e, showErrors );

                return 1;
            }

            Maven maven = null;

            MavenExecutionRequest request = null;

            LoggerManager loggerManager = null;

            try
            {
                // logger must be created first
                loggerManager = (LoggerManager) embedder.lookup( LoggerManager.ROLE );

                if ( debug )
                {
                    loggerManager.setThreshold( Logger.LEVEL_DEBUG );
                }
                else if ( commandLine.hasOption( CLIManager.QUIET ) )
                {
                    // TODO: we need to do some more work here. Some plugins use sys out or log errors at info level.
                    // Ideally, we could use Warn across the board
                    loggerManager.setThreshold( Logger.LEVEL_ERROR );
                    // TODO:Additionally, we can't change the mojo level because the component key includes the version and it isn't known ahead of time. This seems worth changing.
                }

                ProfileManager profileManager = new DefaultProfileManager( embedder.getContainer(), executionProperties );

                if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
                {
                    String [] profileOptionValues = commandLine.getOptionValues( CLIManager.ACTIVATE_PROFILES );

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
                                    profileManager.explicitlyDeactivate( profileAction.substring( 1 ) );
                                }
                                else if ( profileAction.startsWith( "+" ) )
                                {
                                    profileManager.explicitlyActivate( profileAction.substring( 1 ) );
                                }
                                else
                                {
                                    profileManager.explicitlyActivate( profileAction );
                                }
                            }
                        }
                    }
                }

                request = createRequest( commandLine, settings, eventDispatcher, loggerManager, profileManager,
                                         executionProperties, userProperties, showErrors );

                setProjectFileOptions( commandLine, request );

                maven =
                    createMavenInstance( settings.isInteractiveMode(),
                                         loggerManager.getLoggerForComponent( WagonManager.ROLE ) );
            }
            catch ( ComponentLookupException e )
            {
                showFatalError( "Unable to configure the Maven application", e, showErrors );

                return 1;
            }
            finally
            {
                if ( loggerManager != null )
                {
                    try
                    {
                        embedder.release( loggerManager );
                    }
                    catch ( ComponentLifecycleException e )
                    {
                        showFatalError( "Error releasing logging manager", e, showErrors );
                    }
                }
            }

            try
            {
                maven.execute( request );
            }
            catch ( MavenExecutionException e )
            {
                return 1;
            }
        }
        finally
        {
            try
            {
                embedder.stop();
            }
            catch ( Exception e )
            {
                // do nothing; we took our best shot.
            }
        }

        return 0;
    }

    private static Settings buildSettings( CommandLine commandLine )
        throws ComponentLookupException, SettingsConfigurationException
    {
        String userSettingsPath = null;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsPath = commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS );
        }

        if ( commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) )
        {
            String globalSettingsPath = commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_SETTINGS );
            System.setProperty( MavenSettingsBuilder.ALT_GLOBAL_SETTINGS_XML_LOCATION, globalSettingsPath );
        }

        Settings settings = null;

        MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );

        try
        {
            if ( userSettingsPath != null )
            {
                File userSettingsFile = new File( userSettingsPath );

                if ( userSettingsFile.exists() && !userSettingsFile.isDirectory() )
                {
                    settings = settingsBuilder.buildSettings( userSettingsFile );
                }
                else
                {
                    System.out.println( "WARNING: Alternate user settings file: " + userSettingsPath
                        + " is invalid. Using default path." );
                }
            }

            if ( settings == null )
            {
                settings = settingsBuilder.buildSettings();
            }
        }
        catch ( IOException e )
        {
            throw new SettingsConfigurationException( "Error reading settings file", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new SettingsConfigurationException( e.getMessage(), e, e.getLineNumber(),
                                                      e.getColumnNumber() );
        }

        // why aren't these part of the runtime info? jvz.

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            settings.setInteractiveMode( false );
        }

        if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_REGISTRY ) )
        {
            settings.setUsePluginRegistry( false );
        }

        // Create settings runtime info

        settings.setRuntimeInfo( createRuntimeInfo( commandLine, settings ) );

        return settings;
    }

    private static RuntimeInfo createRuntimeInfo( CommandLine commandLine, Settings settings )
    {
        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES )
            || commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.TRUE );
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.FALSE );
        }

        return runtimeInfo;
    }


    private static void showFatalError( String message, Exception e, boolean show )
    {
        System.err.println( "FATAL ERROR: " + message );
        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
        else
        {
            System.err.println( "For more information, run with the -e flag" );
        }
    }

    private static void showError( String message, Exception e, boolean show )
    {
        System.err.println( message );
        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
    }

    private static MavenExecutionRequest createRequest( CommandLine commandLine, Settings settings,
                                                        EventDispatcher eventDispatcher, LoggerManager loggerManager,
                                                        ProfileManager profileManager, Properties executionProperties,
                                                        Properties userProperties, boolean showErrors )
        throws ComponentLookupException
    {
        MavenExecutionRequest request;

        ArtifactRepository localRepository = createLocalRepository( embedder, settings, commandLine );

        File userDir = new File( System.getProperty( "user.dir" ) );

        request = new DefaultMavenExecutionRequest( localRepository, settings, eventDispatcher,
                                                    commandLine.getArgList(), userDir.getPath(), profileManager,
                                                    executionProperties, userProperties, showErrors );

        // TODO [BP]: do we set one per mojo? where to do it?
        Logger logger = loggerManager.getLoggerForComponent( Mojo.ROLE );

        if ( logger != null )
        {
            request.addEventMonitor( new DefaultEventMonitor( logger ) );
        }

        if ( commandLine.hasOption( CLIManager.NON_RECURSIVE ) )
        {
            request.setRecursive( false );
        }

        if ( commandLine.hasOption( CLIManager.FAIL_FAST ) )
        {
            request.setFailureBehavior( ReactorManager.FAIL_FAST );
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_AT_END ) )
        {
            request.setFailureBehavior( ReactorManager.FAIL_AT_END );
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_NEVER ) )
        {
            request.setFailureBehavior( ReactorManager.FAIL_NEVER );
        }

        return request;
    }

    private static void setProjectFileOptions( CommandLine commandLine, MavenExecutionRequest request )
    {
        if ( commandLine.hasOption( CLIManager.REACTOR ) )
        {
            request.setReactorActive( true );
        }
        else if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
        {
            request.setPomFile( commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE ) );
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
            request.setMakeBehavior( ReactorManager.MAKE_MODE );
        }
        else if ( !commandLine.hasOption( CLIManager.ALSO_MAKE )
            && commandLine.hasOption( CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( ReactorManager.MAKE_DEPENDENTS_MODE );
        }
        if ( commandLine.hasOption( CLIManager.ALSO_MAKE ) && commandLine.hasOption( CLIManager.ALSO_MAKE_DEPENDENTS ) )
        {
            request.setMakeBehavior( ReactorManager.MAKE_BOTH_MODE );
        }
    }

    private static Maven createMavenInstance( boolean interactive, Logger logger )
        throws ComponentLookupException
    {
        // TODO [BP]: doing this here as it is CLI specific, though it doesn't feel like the right place (likewise logger).
        WagonManager wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
        if ( interactive )
        {
            wagonManager.setDownloadMonitor( new ConsoleDownloadMonitor( logger ) );
        }
        else
        {
            wagonManager.setDownloadMonitor( new BatchModeDownloadMonitor( logger ) );
        }

        wagonManager.setInteractive( interactive );

        return (Maven) embedder.lookup( Maven.ROLE );
    }

    private static ArtifactRepository createLocalRepository( Embedder embedder, Settings settings,
                                                             CommandLine commandLine )
        throws ComponentLookupException
    {
        // TODO: release
        // TODO: something in plexus to show all active hooks?
        ArtifactRepositoryLayout repositoryLayout =
            (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "default" );

        ArtifactRepositoryFactory artifactRepositoryFactory =
            (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

        String url = settings.getLocalRepository();

        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( commandLine.hasOption( CLIManager.OFFLINE ) )
        {
            settings.setOffline( true );

            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            System.out.println( "+ Enabling strict checksum verification on all artifact downloads." );

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL );
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            System.out.println( "+ Disabling strict checksum verification on all artifact downloads." );

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
        }

        return localRepository;
    }

    static Properties getBuildProperties()
    {
        Properties properties = new Properties();
        InputStream resourceAsStream = null;
        try
        {
            resourceAsStream = MavenCli.class.getClassLoader().getResourceAsStream( "org/apache/maven/messages/build.properties" );

            if ( resourceAsStream != null )
            {
                properties.load( resourceAsStream );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }

        return properties;
    }

    private static void showVersion()
    {
        Properties properties = getBuildProperties();

        String timestamp = reduce( properties.getProperty( "timestamp" ) );
        String version = reduce( properties.getProperty( "version" ) );
        String rev = reduce( properties.getProperty( "buildNumber" ) );

        String msg = "Apache Maven ";
        msg += ( version != null ? version : "<version unknown>" );
        if ( rev != null || timestamp != null )
        {
            msg += " (";
            msg += ( rev != null ? "r" + rev : "" );
            if ( timestamp != null )
            {
                SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" );
                String ts = fmt.format( new Date( Long.valueOf( timestamp ).longValue() ) );
                msg += ( rev != null ? "; " : "" ) + ts;
            }
            msg += ")";
        }

        System.out.println( msg );

        System.out.println( "Java version: " + System.getProperty( "java.version", "<unknown java version>" ) );

        System.out.println( "Java home: " + System.getProperty( "java.home", "<unknown java home>" ) );

        System.out.println( "Default locale: " + Locale.getDefault() + ", platform encoding: "
                            + System.getProperty( "file.encoding", "<unknown encoding>" ) );

        System.out.println( "OS name: \"" + Os.OS_NAME + "\" version: \"" + Os.OS_VERSION
                            + "\" arch: \"" + Os.OS_ARCH + "\" Family: \"" + Os.OS_FAMILY + "\"" );
    }

    private static String reduce( String s )
    {
        return ( s != null ? ( s.startsWith( "${" ) && s.endsWith( "}" ) ? null : s ) : null );
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    static void populateProperties( CommandLine commandLine, Properties executionProperties, Properties userProperties )
    {
        // add the env vars to the property set, with the "env." prefix
        // XXX support for env vars should probably be removed from the ModelInterpolator
        try
        {
            Properties envVars = CommandLineUtils.getSystemEnvVars();
            Iterator i = envVars.entrySet().iterator();
            while ( i.hasNext() )
            {
                Entry e = (Entry) i.next();
                executionProperties.setProperty( "env." + e.getKey().toString(), e.getValue().toString() );
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

            executionProperties.putAll( userProperties );
        }

        executionProperties.putAll( System.getProperties() );
    }

    private static void setCliProperty( String property, Properties requestProperties )
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

        requestProperties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }
}
