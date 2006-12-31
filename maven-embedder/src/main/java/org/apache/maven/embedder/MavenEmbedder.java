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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author Jason van Zyl
 */
public class MavenEmbedder
{
    public static final String userHome = System.getProperty( "user.home" );

    private PlexusContainer container;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private WagonManager wagonManager;

    private MavenXpp3Reader modelReader;

    private MavenXpp3Writer modelWriter;

    private ProfileManager profileManager;

    private PluginDescriptorBuilder pluginDescriptorBuilder;

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    private Maven maven;

    private MavenTools mavenTools;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private Settings settings;

    private ArtifactRepository localRepository;

    private ClassLoader classLoader;

    private ClassWorld classWorld;

    private MavenEmbedderLogger logger;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------

    private boolean started = false;

    private MavenEmbedRequest embedderRequest;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public void setClassWorld( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
    }

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
        checkStarted();
        return modelReader.read( new FileReader( model ) );
    }

    public void writeModel( Writer writer,
                            Model model )
        throws IOException
    {
        checkStarted();
        modelWriter.write( writer, model );
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException
    {
        checkStarted();
        return mavenProjectBuilder.build( mavenProject, localRepository, profileManager );
    }

    /** @deprecated */
    public MavenProject readProjectWithDependencies( File mavenProject,
                                                     TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        checkStarted();
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager,
                                                          transferListener );
    }

    /** @deprecated */
    public MavenProject readProjectWithDependencies( File mavenProject )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        checkStarted();
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager );
    }

    private MavenExecutionRequest populateMavenExecutionRequestWithDefaults( MavenExecutionRequest request )
    {
        // Local repository    	    	
        if ( request.getLocalRepository() == null )
        {
            request.setLocalRepository( localRepository );

            File localRepository = new File( request.getLocalRepository().getBasedir() );

            if ( !localRepository.exists() )
            {
                localRepository.mkdirs();
            }
        }

        // Settings

        if ( request.getSettings() == null )
        {
            request.setSettings( settings );
        }

        /*
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBasedir( baseDirectory )
            .setGoals( goals )
            .setLocalRepositoryPath( localRepositoryPath ) // default: ~/.m2/repository
            .setReactorFailureBehavior( reactorFailureBehaviour ) // default: fail fast
            .setRecursive( recursive ) // default: false
            .setUseReactor( useReactor ) // default: true
            .setShowErrors( showErrors ) // default: false
            .setInteractiveMode( interactive ) // default: false
            .setLoggingLevel( loggingLevel ) // default: info
            .setSettings( settings ) // default: ~/.m2/settings.xml
            .setTransferListener( transferListener ) // default: batch mode which goes along with interactive
            .setOffline( offline ) // default: false
            .setUpdateSnapshots( updateSnapshots ) // default: false
            .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
            .setGlobalChecksumPolicy( globalChecksumPolicy ); // default: warn
        */

        // Classify settings as being used for the life of session or request

        if ( request.getLocalRepository() == null )
        {
        }


        return request;
    }

    /**
     * This method is used to grab the list of dependencies that belong to a project so that a UI
     * can be populated. For example, a list of libraries that are used by an Eclipse, Netbeans, or
     * IntelliJ project.
     */
    // Not well formed exceptions to point people at errors
    // line number in the originating POM so that errors can be shown
    // Need to walk down the tree of dependencies and find all the errors and report in the result
    // validate the request
    // for dependency errors: identifier, path
    // unable to see why you can't get a resource from the repository
    // short message or error id
    // completely obey the same settings used by the CLI, should work exactly the same as the
    //   command line. right now they are very different
    public MavenExecutionResult readProjectWithDependencies( MavenExecutionRequest request )
    {
        MavenProject project = null;

        request = populateMavenExecutionRequestWithDefaults( request );

        try
        {
            project = mavenProjectBuilder.buildWithDependencies( new File( request.getPomFile() ),
                                                                 request.getLocalRepository(), profileManager,
                                                                 request.getTransferListener() );
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
        checkStarted();
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifactWithClassifier( String groupId,
                                                  String artifactId,
                                                  String version,
                                                  String type,
                                                  String classifier )
    {
        checkStarted();
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public void resolve( Artifact artifact,
                         List remoteRepositories,
                         ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        checkStarted();
        artifactResolver.resolve( artifact, remoteRepositories, localRepository );
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List getAvailablePlugins()
    {
        List plugins = new ArrayList();

        plugins.add( makeMockPlugin( "org.apache.maven.plugins", "maven-jar-plugin", "Maven Jar Plug-in" ) );

        plugins.add( makeMockPlugin( "org.apache.maven.plugins", "maven-compiler-plugin", "Maven Compiler Plug-in" ) );

        return plugins;
    }

    public PluginDescriptor getPluginDescriptor( SummaryPluginDescriptor summaryPluginDescriptor )
        throws MavenEmbedderException
    {
        PluginDescriptor pluginDescriptor;

        try
        {
            InputStream is =
                classLoader.getResourceAsStream( "/plugins/" + summaryPluginDescriptor.getArtifactId() + ".xml" );

            pluginDescriptor = pluginDescriptorBuilder.build( new InputStreamReader( is ) );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Error retrieving plugin descriptor.", e );
        }

        return pluginDescriptor;
    }

    private SummaryPluginDescriptor makeMockPlugin( String groupId,
                                                    String artifactId,
                                                    String name )
    {
        return new SummaryPluginDescriptor( groupId, artifactId, name );
    }

    // ----------------------------------------------------------------------
    // Execution of phases/goals
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Lifecycle information
    // ----------------------------------------------------------------------

    public List getLifecyclePhases()
        throws MavenEmbedderException
    {
        checkStarted();
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
        checkStarted();
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

    public void start()
        throws MavenEmbedderException
    {
        start( new DefaultMavenEmbedRequest() );
    }

    public void start( MavenEmbedRequest req )
        throws MavenEmbedderException
    {
        this.embedderRequest = req;

        // ----------------------------------------------------------------------
        // Set the maven.home system property which is need by components like
        // the plugin registry builder.
        // ----------------------------------------------------------------------

        if ( classWorld == null && classLoader == null )
        {
            throw new IllegalStateException(
                "A classWorld or classloader must be specified using setClassLoader|World(ClassLoader)." );
        }

        if ( classWorld == null )
        {
            classWorld = new ClassWorld();

            try
            {
                classWorld.newRealm( "plexus.core", classLoader );
            }
            catch ( DuplicateRealmException e )
            {
                // won't happen
            }
        }

        try
        {
            container = new DefaultPlexusContainer( null, null, null, classWorld );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Error starting Maven embedder.", e );
        }

        if ( logger != null )
        {
            container.setLoggerManager( new MavenEmbedderLoggerManager( new PlexusLoggerAdapter( logger ) ) );
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

            modelWriter = new MavenXpp3Writer();

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

            wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

            started = true;
            
            // ----------------------------------------------------------------------------
            // Settings
            //
            // If the settings file and the global settings file are null then we will use
            // the defaults that Maven provides.
            // ----------------------------------------------------------------------------

            if ( req.getUserSettingsFile() == null )
            {
                req.setUserSettingsFile( mavenTools.getUserSettingsPath( null ) );
            }

            if ( req.getGlobalSettingsFile() == null )
            {
                req.setGlobalSettingsFile( mavenTools.getGlobalSettingsPath() );
            }

            settings = mavenTools.buildSettings( req.getUserSettingsFile(), req.getGlobalSettingsFile(), null );

            resolveParameters( settings );

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

    /**
     * MKLEINT: copied from DefaultMaven. the wagonManager was not injected with proxy info
     * when called in non-execute mode..
     *
     * @todo [BP] this might not be required if there is a better way to pass
     * them in. It doesn't feel quite right.
     * @todo [JC] we should at least provide a mapping of protocol-to-proxy for
     * the wagons, shouldn't we?
     */
    private void resolveParameters( Settings settings )
        throws SettingsConfigurationException
    {

        Proxy proxy = settings.getActiveProxy();

        if ( proxy != null )
        {
            if ( proxy.getHost() == null )
            {
                throw new SettingsConfigurationException( "Proxy in settings.xml has no host" );
            }
            wagonManager.addProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                                   proxy.getPassword(), proxy.getNonProxyHosts() );
        }

        for ( Iterator i = settings.getServers().iterator(); i.hasNext(); )
        {
            Server server = (Server) i.next();

            wagonManager.addAuthenticationInfo( server.getId(), server.getUsername(), server.getPassword(),
                                                server.getPrivateKey(), server.getPassphrase() );

            wagonManager.addPermissionInfo( server.getId(), server.getFilePermissions(),
                                            server.getDirectoryPermissions() );

            if ( server.getConfiguration() != null )
            {
                wagonManager.addConfiguration( server.getId(), (Xpp3Dom) server.getConfiguration() );
            }
        }

        for ( Iterator i = settings.getMirrors().iterator(); i.hasNext(); )
        {
            Mirror mirror = (Mirror) i.next();

            wagonManager.addMirror( mirror.getId(), mirror.getMirrorOf(), mirror.getUrl() );
        }
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void stop()
        throws MavenEmbedderException
    {
        started = false;
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

    public void execute( MavenExecutionRequest request )
        throws MavenExecutionException
    {
        checkStarted();

        maven.execute( request );
    }

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        checkStarted();
        return mavenTools.buildSettings( userSettingsPath, globalSettingsPath, interactive, offline, usePluginRegistry,
                                         pluginUpdateOverride );
    }

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        checkStarted();
        return mavenTools.buildSettings( userSettingsPath, globalSettingsPath, interactive, offline, usePluginRegistry,
                                         Boolean.valueOf( pluginUpdateOverride ) );
    }

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   Boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        checkStarted();
        return mavenTools.buildSettings( userSettingsPath, globalSettingsPath, pluginUpdateOverride );
    }

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        checkStarted();
        return mavenTools.buildSettings( userSettingsPath, globalSettingsPath, Boolean.valueOf( pluginUpdateOverride ) );
    }

    public File getUserSettingsPath( String optionalSettingsPath )
    {
        checkStarted();
        return mavenTools.getUserSettingsPath( optionalSettingsPath );
    }

    public File getGlobalSettingsPath()
    {
        checkStarted();
        return mavenTools.getGlobalSettingsPath();
    }

    public String getLocalRepositoryPath( Settings settings )
    {
        checkStarted();
        return mavenTools.getLocalRepositoryPath( settings );
    }

    private void checkStarted()
    {
        if ( !started )
        {
            throw new IllegalStateException(
                "The embedder is not started, you need to call start() on the embedder prior to calling this method" );
        }
    }
}
