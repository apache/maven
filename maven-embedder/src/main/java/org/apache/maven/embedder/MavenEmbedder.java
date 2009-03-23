package org.apache.maven.embedder;

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
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.MavenPluginDiscoverer;
import org.apache.maven.plugin.PluginLoaderException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectBuildingResult;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.reactor.MissingModuleException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsConfigurationException;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications.
 *
 * @author Jason van Zyl
 */
public class MavenEmbedder
{
    public static final String userHome = System.getProperty( "user.home" );

    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( userMavenConfigurationHome, "settings.xml" );

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
        new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    // ----------------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------------

    private MutablePlexusContainer container;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private Maven maven;

    private PluginManager pluginManager;

    private MavenProjectBuilder mavenProjectBuilder;

    private MavenXpp3Reader modelReader;

    private MavenXpp3Writer modelWriter;
    
    private MavenExecutionRequestPopulator populator;
        
    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private ClassWorld classWorld;

    private ClassRealm realm;

    private MavenEmbedderLogger logger;

    private boolean activateSystemManager;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------

    private Configuration configuration;

    // ----------------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------------

    public MavenEmbedder( Configuration embedderConfiguration )
        throws MavenEmbedderException
    {
        start( embedderConfiguration );
    }

    public MavenExecutionRequest getDefaultRequest()
    {
        return request;
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
        return request.getLocalRepository();
    }

    public Settings getSettings()
    {
        return request.getSettings();
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
        Reader reader = ReaderFactory.newXmlReader( file );

    	try
    	{
    		return readModel( reader );
    	}
    	finally
    	{
    		IOUtil.close( reader );
    	}
    }

    public Model readModel( Reader reader )
        throws XmlPullParserException, IOException
    {
    	return modelReader.read( reader );
    }

    public void writeModel( Writer writer, Model model, boolean namespaceDeclaration )
        throws IOException
    {
        modelWriter.write( writer, model );
    }

    public void writeModel( Writer writer,
                            Model model )
        throws IOException
    {
        modelWriter.write( writer, model );
    }

    // ----------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------

    public static void writeSettings( File file,
                                      Settings settings )
        throws IOException
    {
        SettingsValidator settingsValidator = new DefaultSettingsValidator();

        SettingsValidationResult validationResult = settingsValidator.validate( settings );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate Settings.\n" + validationResult.render( "\n" ) );
        }

        SettingsXpp3Writer writer = new SettingsXpp3Writer();

        Writer fileWriter = WriterFactory.newXmlWriter( file );

        try
        {
            writer.write( fileWriter, settings );
        }
        finally
        {
            IOUtil.close( fileWriter );
        }
    }

    public static Settings readSettings( File file )
        throws IOException, SettingsConfigurationException
    {
        Reader fileReader = ReaderFactory.newXmlReader( file );

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
            IOUtil.close( fileReader );
        }
    }

    /**
     * mkleint: protected so that IDE integrations can selectively allow downloading artifacts
     * from remote repositories (if they prohibit by default on project loading)
     * @throws PluginLoaderException 
     */
    protected void verifyPlugin( Plugin plugin, MavenProject project )
        throws ComponentLookupException, PluginLoaderException
    {
        MavenSession session = new MavenSession( container, request, null );
        pluginManager.loadPlugin( plugin, project, session );
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException, MavenExecutionException
    {
        return readProject( mavenProject, request );
    }

    private MavenProject readProject( File mavenProject, MavenExecutionRequest request )
        throws ProjectBuildingException, MissingModuleException
    {
        getLogger().debug( "Building MavenProject instance: " + mavenProject );

        return mavenProjectBuilder.build( mavenProject, request.getProjectBuildingConfiguration() );
    }

    /**
     * This method is used to grab the list of dependencies that belong to a project so that a UI
     * can be populated. For example, a list of libraries that are used by an Eclipse, Netbeans, or
     * IntelliJ project.
     */
    public MavenExecutionResult readProjectWithDependencies( MavenExecutionRequest request )
    {
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        try
        {
            request = populator.populateDefaults( request, configuration );

            readProject( request.getPom(), request );
        }
        catch ( MavenEmbedderException e )
        {
            return result.addException( e );
        }
        catch ( ProjectBuildingException e )
        {
            return result.addException( e );
        }
        catch ( MissingModuleException e )
        {
            return result.addException( e );
        }

        ReactorManager reactorManager = maven.createReactorManager( request, result );

        if ( result.hasExceptions() )
        {
            return result;
        }

        MavenProjectBuildingResult projectBuildingResult;

        try
        {
            projectBuildingResult = mavenProjectBuilder.buildProjectWithDependencies( request.getPom(), request.getProjectBuildingConfiguration() );
        }
        catch ( ProjectBuildingException e )
        {
            return result.addException( e );
        }

        if ( reactorManager.hasMultipleProjects() )
        {
            result.setProject( projectBuildingResult.getProject() );

            result.setTopologicallySortedProjects( reactorManager.getSortedProjects() );
        }
        else
        {
            result.setProject( projectBuildingResult.getProject() );

            result.setTopologicallySortedProjects( Arrays.asList( new MavenProject[]{ projectBuildingResult.getProject()} ) );
        }

        result.setArtifactResolutionResult( projectBuildingResult.getArtifactResolutionResult() );

        // From this I could produce something that would help IDE integrators create importers:
        // - topo sorted list of projects
        // - binary dependencies
        // - source dependencies (projects in the reactor)
        //
        // We could create a layer approach here. As to do anything you must resolve a projects artifacts,
        // and with that set you could then subsequently execute goals for each of those project.

        return result;
    }

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    private MavenExecutionRequest request;

    private EventDispatcher dispatcher;

    private void start( Configuration configuration )
        throws MavenEmbedderException
    {
        classWorld = configuration.getClassWorld();

        logger = configuration.getMavenEmbedderLogger();

        // ----------------------------------------------------------------------------
        // Don't override any existing SecurityManager if one has been installed. Our
        // SecurityManager just checks to make sure
        // ----------------------------------------------------------------------------

        try
        {
            if ( ( System.getSecurityManager() == null ) && activateSystemManager )
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
            ContainerConfiguration cc = new DefaultContainerConfiguration()
                .addComponentDiscoverer( new MavenPluginDiscoverer() )
                .addComponentDiscoveryListener( new MavenPluginCollector() )
                .setClassWorld( classWorld )
                .setName( "embedder" );

            container = new DefaultPlexusContainer( cc );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Error creating Plexus container for Maven Embedder", e );
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

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            modelWriter = new MavenXpp3Writer();

            maven = container.lookup( Maven.class );

            mavenProjectBuilder = container.lookup( MavenProjectBuilder.class );

            populator = container.lookup( MavenExecutionRequestPopulator.class );

            container.lookup( RepositorySystem.class );
            
            // This is temporary as we can probably cache a single request and use it for default values and
            // simply cascade values in from requests used for individual executions.
            request = new DefaultMavenExecutionRequest();

            populator.populateDefaults( request, configuration );

            dispatcher = new DefaultEventDispatcher( request.getEventMonitors() );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
    }

    public void stop()
        throws MavenEmbedderException
    {
        container.dispose();
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

    public boolean isOffline( MavenExecutionRequest request )
        throws MavenEmbedderException
    {
        // first, grab defaults including settings, in case <offline>true</offline> is set.
        request = populator.populateDefaults( request, configuration );

        return request.isOffline();
    }

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        LoggerManager loggerManager = container.getLoggerManager();

        int oldThreshold = loggerManager.getThreshold();

        try
        {
            loggerManager.setThresholds( request.getLoggingLevel() );

            try
            {
                request = populator.populateDefaults( request, configuration );
            }
            catch ( MavenEmbedderException e )
            {
                MavenExecutionResult result = new DefaultMavenExecutionResult();

                result.addException( e );

                return result;
            }

            return maven.execute( request );
        }
        finally
        {
            loggerManager.setThresholds( oldThreshold );
        }
    }

    /**
     * Return the instance of the plexus container being used in the embedder.
     *
     * @return The plexus container used in the embedder.
     */
    public PlexusContainer getPlexusContainer()
    {
        return container;
    }

    public List getLifecyclePhases()
    {       
        return maven.getLifecyclePhases();
    }
}
