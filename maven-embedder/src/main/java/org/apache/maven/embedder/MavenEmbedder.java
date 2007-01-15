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
import org.apache.maven.MavenTools;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.execution.MavenExecutionRequestDefaultsPopulator;
import org.apache.maven.embedder.writer.WriterUtils;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author Jason van Zyl
 */
public class MavenEmbedder
{
    private PlexusContainer container;

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

    private MavenTools mavenTools;

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

    private MavenEmbedRequest embedderRequest;

    // ----------------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------------

    public MavenEmbedder( ClassWorld classWorld )
        throws MavenEmbedderException
    {
        this( classWorld, null );
    }

    public MavenEmbedder( ClassWorld classWorld,
                          MavenEmbedderLogger logger )
        throws MavenEmbedderException
    {
        this.classWorld = classWorld;

        this.logger = logger;

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

        start();
    }

    public MavenEmbedder( ClassLoader classLoader )
        throws MavenEmbedderException
    {
        this( classLoader, null );
    }

    public MavenEmbedder( ClassLoader classLoader,
                          MavenEmbedderLogger logger )
        throws MavenEmbedderException
    {
        this( new ClassWorld( "plexus.core", classLoader ), logger );
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

    public MavenEmbedderLogger getLogger()
    {
        return logger;
    }

    public void setLogger( MavenEmbedderLogger logger )
    {
        this.logger = logger;
    }

    public Model readModel( File model )
        throws XmlPullParserException, IOException
    {
        return modelReader.read( new FileReader( model ) );
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
            request = defaultsPopulator.populateDefaults( request );

            project = mavenProjectBuilder.buildWithDependencies( new File( request.getPomFile() ),
                                                                 request.getLocalRepository(), profileManager,
                                                                 request.getTransferListener() );
        }
        catch ( MavenEmbedderException e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }
        catch ( ProjectBuildingException e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }
        catch ( ArtifactResolutionException e )
        {
            return new DefaultMavenExecutionResult( project, Collections.singletonList( e ) );
        }
        catch ( ArtifactNotFoundException e )
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
    // Local Repository
    // ----------------------------------------------------------------------

    public static final String DEFAULT_LOCAL_REPO_ID = "local";

    public static final String DEFAULT_LAYOUT_ID = "default";

    public ArtifactRepository createLocalRepository( File localRepository )
        throws ComponentLookupException
    {
        return createLocalRepository( localRepository.getAbsolutePath(), DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( Settings settings )
    {
        return createLocalRepository( mavenTools.getLocalRepositoryPath( settings ), DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( String url,
                                                     String repositoryId )
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        return createRepository( url, repositoryId );
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

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    private void start()
        throws MavenEmbedderException
    {
        start( new DefaultMavenEmbedRequest() );
    }

    public void start( MavenEmbedRequest req )
        throws MavenEmbedderException
    {
        this.embedderRequest = req;

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
            if ( req.getContainerCustomizer() != null )
            {
                req.getContainerCustomizer().customize( container );
            }

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            modelWriter = new MavenJDOMWriter();

            maven = (Maven) container.lookup( Maven.ROLE );

            mavenTools = (MavenTools) container.lookup( MavenTools.ROLE );

            pluginDescriptorBuilder = new PluginDescriptorBuilder();

            profileManager = new DefaultProfileManager( container, req.getSystemProperties() );

            profileManager.explicitlyActivate( req.getActiveProfiles() );

            profileManager.explicitlyDeactivate( req.getInactiveProfiles() );

            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

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

            // These three things can be cached for a single session of the embedder
            settings = mavenTools.buildSettings( req.getUserSettingsFile(), req.getGlobalSettingsFile(), false );

            localRepository = createLocalRepository( settings );

            profileManager.loadSettingsProfiles( settings );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
        catch ( SettingsConfigurationException e )
        {
            throw new MavenEmbedderException( "Cannot create settings configuration", e );
        }
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void stop()
        throws MavenEmbedderException
    {
        try
        {
            container.release( mavenProjectBuilder );

            container.release( artifactRepositoryFactory );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Cannot stop the embedder.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Start of new embedder API
    // ----------------------------------------------------------------------

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        try
        {
            request = defaultsPopulator.populateDefaults( request );
        }
        catch ( MavenEmbedderException e )
        {
            return new DefaultMavenExecutionResult( Collections.singletonList( e ) );
        }

        return maven.execute( request );
    }
}
