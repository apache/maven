package org.apache.maven.embedder;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.Maven;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.embedder.configuration.Configuration;
import org.apache.maven.embedder.configuration.ConfigurationValidationResult;
import org.apache.maven.embedder.configuration.DefaultConfigurationValidationResult;
import org.apache.maven.embedder.execution.MavenExecutionRequestDefaultsPopulator;
import org.apache.maven.embedder.writer.WriterUtils;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.jdom.SettingsJDOMWriter;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author Jason van Zyl
 */
public class MavenEmbedder
{
    public static final String DEFAULT_LOCAL_REPO_ID = "local";

    public static final String DEFAULT_LAYOUT_ID = "default";

    public static final String userHome = System.getProperty( "user.home" );

    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );

    public static final String mavenHome = System.getProperty( "maven.home" );

    public static final File defaultUserLocalRepository = new File( userMavenConfigurationHome, "repository" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( userMavenConfigurationHome, "settings.xml" );

    // ----------------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------------

    private MutablePlexusContainer container;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private MavenProjectBuilder mavenProjectBuilder;

    private MavenXpp3Reader modelReader;

    private MavenJDOMWriter modelWriter;

    private ProfileManager profileManager;

    private PluginDescriptorBuilder pluginDescriptorBuilder;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    private ArtifactHandlerManager artifactHandlerManager;

    private Maven maven;

    private MavenSettingsBuilder settingsBuilder;

    private MavenExecutionRequestDefaultsPopulator defaultsPopulator;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private Settings settings;

    private ArtifactRepository localRepository;

    private ClassWorld classWorld;

    private ClassRealm realm;

    private MavenEmbedderLogger logger;

    private boolean activateSystemManager;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------

    private Configuration configuration;

    private BuildContextManager buildContextManager;

    // ----------------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------------

    public MavenEmbedder( Configuration embedderConfiguration )
        throws MavenEmbedderException
    {
        start( embedderConfiguration );
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public ClassWorld getClassWorld()
    {
        return classWorld;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public MavenEmbedderLogger getLogger()
    {
        return logger;
    }

    public void setLogger( MavenEmbedderLogger logger )
    {
        this.logger = logger;
    }

    public Model readModel( File file )
        throws XmlPullParserException, IOException
    {
        Reader reader = new FileReader( file );

        Model model;

        try
        {
            model = modelReader.read( reader );
        }
        finally
        {
            reader.close();
        }

        return model;
    }

    public void writeModel( Writer writer,
                            Model model,
                            boolean namespaceDeclaration )
        throws IOException
    {
        WriterUtils.write( writer, model, true );
    }

    public void writeModel( Writer writer,
                            Model model )
        throws IOException
    {
        WriterUtils.write( writer, model, false );
    }

    // ----------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------

    public static void writeSettings( File file,
                                      Settings settings )
        throws IOException
    {
        Writer fileWriter = new FileWriter( file );

        SettingsValidator settingsValidator = new DefaultSettingsValidator();

        SettingsValidationResult validationResult = settingsValidator.validate( settings );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate Settings.\n" + validationResult.render( "\n" ) );
        }

        Element root = new Element( "settings" );

        Document doc = new Document( root );

        SettingsJDOMWriter writer = new SettingsJDOMWriter();

        String encoding = settings.getModelEncoding() != null ? settings.getModelEncoding() : "UTF-8";

        Format format = Format.getPrettyFormat().setEncoding( encoding );

        try
        {
            writer.write( settings, doc, fileWriter, format );
        }
        finally
        {
            fileWriter.close();
        }
    }

    public static Settings readSettings( File file )
        throws IOException, SettingsConfigurationException
    {
        Reader fileReader = new FileReader( file );

        SettingsValidator settingsValidator = new DefaultSettingsValidator();

        SettingsXpp3Reader reader = new SettingsXpp3Reader();

        try
        {
            Settings settings = reader.read( fileReader );

            SettingsValidationResult validationResult = settingsValidator.validate( settings );

            if ( validationResult.getMessageCount() > 0 )
            {
                throw new IOException( "Failed to validate Settings.\n" + validationResult.render( "\n" ) );
            }

            return settings;
        }
        catch ( XmlPullParserException e )
        {
            throw new SettingsConfigurationException( "Failed to parse settings.", e );
        }
        finally
        {
            fileReader.close();
        }
    }

    /**
     * mkleint: protected so that IDE integrations can selectively allow downloading artifacts
     * from remote repositories (if they prohibit by default on project loading)
     */
    protected void verifyPlugin( Plugin plugin,
                                 MavenProject project )
        throws ComponentLookupException, ArtifactResolutionException, PluginVersionResolutionException,
        ArtifactNotFoundException, InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException,
        PluginNotFoundException, PluginVersionNotFoundException
    {
        PluginManager pluginManager = (PluginManager) container.lookup( PluginManager.ROLE );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setSettings( settings )
            .setLocalRepository( localRepository );

        MavenSession session = new MavenSession( container, request, null, null );

        pluginManager.verifyPlugin( plugin, project, session );
    }

    /** protected for tests only.. */
    protected Map getPluginExtensionComponents( Plugin plugin )
        throws PluginManagerException
    {
        try
        {
            PluginManager pluginManager = (PluginManager) container.lookup( PluginManager.ROLE );
            return pluginManager.getPluginComponents( plugin, ArtifactHandler.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            getLogger().debug( "Unable to find the lifecycle component in the extension", e );
            return new HashMap();
        }
    }

    /**
     * mkleint: copied from DefaultLifecycleExecutor
     *
     * @todo Not particularly happy about this. Would like WagonManager and ArtifactTypeHandlerManager to be able to
     * lookup directly, or have them passed in
     * @todo Move this sort of thing to the tail end of the project-building process
     */
    private Map findArtifactTypeHandlers( MavenProject project )
        throws Exception
    {
        Map map = new HashMap();

        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            if ( plugin.isExtensions() )
            {
                verifyPlugin( plugin, project );

                map.putAll( getPluginExtensionComponents( plugin ) );

                // shudder...
                for ( Iterator j = map.values().iterator(); j.hasNext(); )
                {
                    ArtifactHandler handler = (ArtifactHandler) j.next();
                    if ( project.getPackaging().equals( handler.getPackaging() ) )
                    {
                        project.getArtifact().setArtifactHandler( handler );
                    }
                }
            }
        }

        return map;
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException
    {
        return mavenProjectBuilder.build( mavenProject, localRepository, profileManager );
    }

    /**
     * This method is used to grab the list of dependencies that belong to a project so that a UI
     * can be populated. For example, a list of libraries that are used by an Eclipse, Netbeans, or
     * IntelliJ project.
     */
    public MavenExecutionResult readProjectWithDependencies( MavenExecutionRequest request )
    {
        MavenProject project = null;

        try
        {
            request = defaultsPopulator.populateDefaults( request, this );
            //mkleint: copied from DefaultLifecycleExecutor
            project = readProject( new File( request.getPomFile() ) );
            Map handlers = findArtifactTypeHandlers( project );
            //is this necessary in this context, I doubt it..mkleint
            artifactHandlerManager.addHandlers( handlers );
            project = mavenProjectBuilder.buildWithDependencies( new File( request.getPomFile() ),
                                                                 request.getLocalRepository(), profileManager,
                                                                 request.getTransferListener() );
        }
        catch ( Exception e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }

        return new DefaultMavenExecutionResult( project, Collections.EMPTY_LIST );
    }

    // ----------------------------------------------------------------------
    // Artifacts
    // ----------------------------------------------------------------------

    public Artifact createArtifact( String groupId,
                                    String artifactId,
                                    String version,
                                    String scope,
                                    String type )
    {
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifactWithClassifier( String groupId,
                                                  String artifactId,
                                                  String version,
                                                  String type,
                                                  String classifier )
    {
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public void resolve( Artifact artifact,
                         List remoteRepositories,
                         ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        artifactResolver.resolve( artifact, remoteRepositories, localRepository );
    }

    public ArtifactHandler getArtifactHandler( Artifact artifact )
    {
        return artifactHandlerManager.getArtifactHandler( artifact.getType() );
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // Execution of phases/goals
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // Lifecycle information
    // ----------------------------------------------------------------------

    public List getLifecyclePhases()
        throws MavenEmbedderException
    {
        List phases = new ArrayList();

        ComponentDescriptor descriptor = container.getComponentDescriptor( LifecycleExecutor.ROLE );

        PlexusConfiguration configuration = descriptor.getConfiguration();

        PlexusConfiguration[] phasesConfigurations =
            configuration.getChild( "lifecycles" ).getChild( 0 ).getChild( "phases" ).getChildren( "phase" );

        try
        {
            for ( int i = 0; i < phasesConfigurations.length; i++ )
            {
                phases.add( phasesConfigurations[i].getValue() );
            }
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Cannot retrieve default lifecycle phasesConfigurations.", e );
        }

        return phases;
    }

    // ----------------------------------------------------------------------
    // Remote Repository
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    private void start( Configuration configuration )
        throws MavenEmbedderException
    {
        this.classWorld = configuration.getClassWorld();

        this.logger = configuration.getMavenEmbedderLogger();

        // ----------------------------------------------------------------------------
        // Don't override any existing SecurityManager if one has been installed. Our
        // SecurityManager just checks to make sure
        // ----------------------------------------------------------------------------

        try
        {
            if ( System.getSecurityManager() == null && activateSystemManager )
            {
                System.setSecurityManager( new MavenEmbedderSecurityManager() );
            }
        }
        catch ( RuntimeException e )
        {
            logger.warn( "Error trying to set the SecurityManager: " + e.getMessage() );
        }

        this.configuration = configuration;

        try
        {
            container = new DefaultPlexusContainer( null, null, classWorld );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Error starting Maven embedder.", e );
        }

        if ( logger != null )
        {
            MavenEmbedderLoggerManager loggerManager =
                new MavenEmbedderLoggerManager( new PlexusLoggerAdapter( logger ) );

            container.setLoggerManager( loggerManager );
        }

        try
        {
            if ( configuration.getContainerCustomizer() != null )
            {
                configuration.getContainerCustomizer().customize( container );
            }

            handleExtensions( configuration.getExtensions() );

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            modelWriter = new MavenJDOMWriter();

            maven = (Maven) container.lookup( Maven.ROLE );

            settingsBuilder = (MavenSettingsBuilder) container.lookup( MavenSettingsBuilder.ROLE );

            pluginDescriptorBuilder = new PluginDescriptorBuilder();

            profileManager = new DefaultProfileManager( container, configuration.getSystemProperties() );

            profileManager.explicitlyActivate( configuration.getActiveProfiles() );

            profileManager.explicitlyDeactivate( configuration.getInactiveProfiles() );

            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

            buildContextManager = (BuildContextManager) container.lookup( BuildContextManager.ROLE );

            // ----------------------------------------------------------------------
            // Artifact related components
            // ----------------------------------------------------------------------

            artifactRepositoryFactory = (ArtifactRepositoryFactory) container.lookup( ArtifactRepositoryFactory.ROLE );

            artifactFactory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );

            artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

            defaultArtifactRepositoryLayout =
                (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, DEFAULT_LAYOUT_ID );

            defaultsPopulator = (MavenExecutionRequestDefaultsPopulator) container.lookup(
                MavenExecutionRequestDefaultsPopulator.ROLE );

            artifactHandlerManager = (ArtifactHandlerManager) container.lookup( ArtifactHandlerManager.ROLE );

            try
            {
                if ( configuration.getUserSettingsFile() == null )
                {
                    configuration.setUserSettingsFile( DEFAULT_USER_SETTINGS_FILE );
                }

                settings = settingsBuilder.buildSettings( configuration.getUserSettingsFile(),
                                                          configuration.getGlobalSettingsFile() );
            }
            catch ( Exception e )
            {
                // If something goes wrong with parsing the settings
                settings = new Settings();
            }

            localRepository = createLocalRepository( settings );

            profileManager.loadSettingsProfiles( settings );

        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    private void handleExtensions( List extensions )
        throws MavenEmbedderException
    {
        ClassRealm childRealm;
        try
        {
            childRealm = container.getContainerRealm().createChildRealm( "embedder-extensions" );
        }
        catch ( DuplicateRealmException e1 )
        {
            try
            {
                childRealm = classWorld.getRealm( "embedder-extensions" );
            }
            catch ( NoSuchRealmException e )
            {
                throw new MavenEmbedderException( "Cannot create realm 'extensions'", e );
            }
        }

        for ( Iterator it = extensions.iterator(); it.hasNext(); )
        {
            childRealm.addURL( (URL) it.next() );
        }

        try
        {
            container.discoverComponents( childRealm, true );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Configuration error while discovering extension components", e );
        }
        catch ( ComponentRepositoryException e )
        {
            throw new MavenEmbedderException( "Component repository error while discovering extension components", e );
        }
    }

    public void stop()
        throws MavenEmbedderException
    {
        try
        {
            buildContextManager.clearBuildContext();

            container.release( buildContextManager );

            container.release( mavenProjectBuilder );

            container.release( artifactRepositoryFactory );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Cannot stop the embedder.", e );
        }
    }

    // ----------------------------------------------------------------------------
    // Validation
    // ----------------------------------------------------------------------------

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
        ConfigurationValidationResult result = new DefaultConfigurationValidationResult();

        if ( configuration.getUserSettingsFile() == null )
        {
            configuration.setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE );
        }

        Reader fileReader;

        // User settings

        try
        {
            fileReader = new FileReader( configuration.getUserSettingsFile() );

            new SettingsXpp3Reader().read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            result.setUserSettingsFilePresent( false );
        }
        catch ( IOException e )
        {
            result.setUserSettingsFileParses( false );
        }
        catch ( XmlPullParserException e )
        {
            result.setUserSettingsFileParses( false );
        }

        // Global settings

        if ( configuration.getGlobalSettingsFile() != null )
        {
            try
            {
                fileReader = new FileReader( configuration.getGlobalSettingsFile() );

                new SettingsXpp3Reader().read( fileReader );
            }
            catch ( FileNotFoundException e )
            {
                result.setGlobalSettingsFilePresent( false );
            }
            catch ( IOException e )
            {
                result.setGlobalSettingsFileParses( false );
            }
            catch ( XmlPullParserException e )
            {
                result.setGlobalSettingsFileParses( false );
            }
        }

        return result;
    }

    // ----------------------------------------------------------------------
    // Local Repository
    // ----------------------------------------------------------------------

    public ArtifactRepository createLocalRepository( Settings settings )
        throws MavenEmbedderException
    {
        String localRepositoryPath = null;

        if ( configuration.getLocalRepository() != null )
        {
            localRepositoryPath = configuration.getLocalRepository().getAbsolutePath();
        }

        if ( StringUtils.isEmpty( localRepositoryPath ) )
        {
            localRepositoryPath = settings.getLocalRepository();
        }

        if ( StringUtils.isEmpty( localRepositoryPath ) )
        {
            localRepositoryPath = MavenEmbedder.defaultUserLocalRepository.getAbsolutePath();
        }

        return createLocalRepository( localRepositoryPath, MavenEmbedder.DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( String url,
                                                     String repositoryId )
        throws MavenEmbedderException
    {
        try
        {
            return createRepository( canonicalFileUrl( url ), repositoryId );
        }
        catch ( IOException e )
        {
            throw new MavenEmbedderException( "Unable to resolve canonical path for local repository " + url, e );
        }
    }

    private String canonicalFileUrl( String url )
        throws IOException
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }
        else if ( url.startsWith( "file:" ) && !url.startsWith( "file://" ) )
        {
            url = "file://" + url.substring( "file:".length() );
        }

        // So now we have an url of the form file://<path>

        // We want to eliminate any relative path nonsense and lock down the path so we
        // need to fully resolve it before any sub-modules use the path. This can happen
        // when you are using a custom settings.xml that contains a relative path entry
        // for the local repository setting.

        File localRepository = new File( url.substring( "file://".length() ) );

        if ( !localRepository.isAbsolute() )
        {
            url = "file://" + localRepository.getCanonicalPath();
        }

        return url;
    }

    public ArtifactRepository createRepository( String url,
                                                String repositoryId )
    {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout,
                                                                   snapshotsPolicy, releasesPolicy );
    }

    // ----------------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------------

    public Configuration getConfiguration()
    {
        return configuration;
    }

    // ----------------------------------------------------------------------
    // Start of new embedder API
    // ----------------------------------------------------------------------

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        LoggerManager loggerManager = container.getLoggerManager();

        int oldThreshold = loggerManager.getThreshold();

        try
        {
            loggerManager.setThresholds( request.getLoggingLevel() );

            try
            {
                request = defaultsPopulator.populateDefaults( request, this );
            }
            catch ( MavenEmbedderException e )
            {
                return new DefaultMavenExecutionResult( Collections.singletonList( e ) );
            }

            return maven.execute( request );
        }
        finally
        {
            loggerManager.setThresholds( oldThreshold );
        }
    }
}
