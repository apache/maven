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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.BuildFailureException;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Properties;
import java.util.Date;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class MavenEmbedder
{
    public static final String userHome = System.getProperty( "user.home" );

    // ----------------------------------------------------------------------
    // Embedder
    // ----------------------------------------------------------------------

    private Embedder embedder;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private MavenSettingsBuilder settingsBuilder;

    private LifecycleExecutor lifecycleExecutor;

    private WagonManager wagonManager;

    private MavenXpp3Reader modelReader;

    private MavenXpp3Writer modelWriter;

    private ProfileManager profileManager;

    private PluginDescriptorBuilder pluginDescriptorBuilder;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private Settings settings;

    private ArtifactRepository localRepository;

    private File localRepositoryDirectory;

    private ClassLoader classLoader;

    private MavenEmbedderLogger logger;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------

    private boolean pluginUpdateOverride;

    private boolean checkLatestPluginVersion;

    private boolean interactiveMode;

    private boolean usePluginRegistry;

    private boolean offline;

    private boolean updateSnapshots;

    private String globalChecksumPolicy;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void setPluginUpdateOverride( boolean pluginUpdateOverride )
    {
        this.pluginUpdateOverride = pluginUpdateOverride;
    }

    public boolean isPluginUpdateOverride()
    {
        return pluginUpdateOverride;
    }

    public void setCheckLatestPluginVersion( boolean checkLatestPluginVersion )
    {
        this.checkLatestPluginVersion = checkLatestPluginVersion;
    }

    public boolean isCheckLatestPluginVersion()
    {
        return checkLatestPluginVersion;
    }

    public void setInteractiveMode( boolean interactiveMode )
    {
        this.interactiveMode = interactiveMode;
    }

    public boolean isInteractiveMode()
    {
        return interactiveMode;
    }

    public void setUsePluginRegistry( boolean usePluginRegistry )
    {
        this.usePluginRegistry = usePluginRegistry;
    }

    public boolean isUsePluginRegistry()
    {
        return usePluginRegistry;
    }

    public void setOffline( boolean offline )
    {
        this.offline = offline;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public void setUpdateSnapshots( boolean updateSnapshots )
    {
        this.updateSnapshots = updateSnapshots;
    }

    public boolean isUpdateSnapshots()
    {
        return updateSnapshots;
    }

    public void setGlobalChecksumPolicy( String globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;
    }

    public String getGlobalChecksumPolicy()
    {
        return globalChecksumPolicy;
    }

    /**
     * Set the classloader to use with the maven embedder.
     *
     * @param classLoader
     */
    public void setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public void setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
    }

    public File getLocalRepositoryDirectory()
    {
        return localRepositoryDirectory;
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

    // ----------------------------------------------------------------------
    // Embedder Client Contract
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Model
    // ----------------------------------------------------------------------

    public Model readModel( File model )
        throws XmlPullParserException, FileNotFoundException, IOException
    {
        return modelReader.read( new FileReader( model ) );
    }

    public void writeModel( Writer writer, Model model )
        throws IOException
    {
        modelWriter.write( writer, model );
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException
    {
        return mavenProjectBuilder.build( mavenProject, localRepository, profileManager );
    }

    public MavenProject readProjectWithDependencies( File mavenProject, TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager, transferListener );
    }

    public MavenProject readProjectWithDependencies( File mavenProject )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager );
    }

    public List collectProjects( File basedir, String[] includes, String[] excludes )
        throws MojoExecutionException
    {
        List projects = new ArrayList();

        List poms = getPomFiles( basedir, includes, excludes );

        for ( Iterator i = poms.iterator(); i.hasNext(); )
        {
            File pom = (File) i.next();

            try
            {
                MavenProject p = readProject( pom );

                projects.add( p );

            }
            catch ( ProjectBuildingException e )
            {
                throw new MojoExecutionException( "Error loading " + pom, e );
            }
        }

        return projects;
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
            InputStream is = classLoader.getResourceAsStream( "/plugins/" + summaryPluginDescriptor.getArtifactId() + ".xml" );

            pluginDescriptor = pluginDescriptorBuilder.build( new InputStreamReader( is ) );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MavenEmbedderException( "Error retrieving plugin descriptor.", e );
        }

        return pluginDescriptor;
    }

    private SummaryPluginDescriptor makeMockPlugin( String groupId, String artifactId, String name )
    {
        return new SummaryPluginDescriptor( groupId, artifactId, name );
    }

    // ----------------------------------------------------------------------
    // Execution of phases/goals
    // ----------------------------------------------------------------------

    // TODO: should we allow the passing in of a settings object so that everything can be taken from the client env

    // TODO: transfer listener
    // TODO: logger

    public void execute( MavenProject project,
                         List goals,
                         EventMonitor eventMonitor,
                         TransferListener transferListener,
                         Properties properties,
                         File executionRootDirectory )
        throws CycleDetectedException, LifecycleExecutionException, BuildFailureException, DuplicateProjectException
    {
        execute( Collections.singletonList( project ), goals, eventMonitor, transferListener, properties, executionRootDirectory );
    }

    public void execute( List projects,
                         List goals,
                         EventMonitor eventMonitor,
                         TransferListener transferListener,
                         Properties properties,
                         File executionRootDirectory )
        throws CycleDetectedException, LifecycleExecutionException, BuildFailureException, DuplicateProjectException
    {
        ReactorManager rm = new ReactorManager( projects );

        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        eventDispatcher.addEventMonitor( eventMonitor );

        // If this option is set the exception seems to be hidden ...

        //rm.setFailureBehavior( ReactorManager.FAIL_AT_END );

        rm.setFailureBehavior( ReactorManager.FAIL_FAST );

        MavenSession session = new MavenSession( embedder.getContainer(),
                                                 settings,
                                                 localRepository,
                                                 eventDispatcher,
                                                 rm,
                                                 goals,
                                                 executionRootDirectory.getAbsolutePath(),
                                                 properties,
                                                 new Date() );

        session.setUsingPOMsFromFilesystem( true );

        if ( transferListener != null )
        {
            wagonManager.setDownloadMonitor( transferListener );
        }

        // ----------------------------------------------------------------------
        // Maven should not be using system properties internally but because
        // it does for now I'll just take properties that are handed to me
        // and set them so that the plugin expression evaluator will work
        // as expected.
        // ----------------------------------------------------------------------

        if ( properties != null )
        {
            for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();

                String value = properties.getProperty( key );

                System.setProperty( key, value );
            }
        }

        lifecycleExecutor.execute( session, rm, session.getEventDispatcher() );
    }

    // ----------------------------------------------------------------------
    // Lifecycle information
    // ----------------------------------------------------------------------

    public List getLifecyclePhases()
        throws MavenEmbedderException
    {
        List phases = new ArrayList();

        ComponentDescriptor descriptor = embedder.getContainer().getComponentDescriptor( LifecycleExecutor.ROLE );

        PlexusConfiguration configuration = descriptor.getConfiguration();

        PlexusConfiguration[] phasesConfigurations = configuration.getChild( "lifecycles" ).getChild( 0 ).getChild( "phases" ).getChildren( "phase" );        

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
    // Internal utility code
    // ----------------------------------------------------------------------

    private ArtifactRepository createLocalRepository( Settings settings )
        throws ComponentLookupException
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "default" );

        String url = settings.getLocalRepository();

        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( offline )
        {
            settings.setOffline( true );

            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && updateSnapshots )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( globalChecksumPolicy );

        return localRepository;
    }

    private RuntimeInfo createRuntimeInfo( Settings settings )
    {
        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        if ( pluginUpdateOverride )
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.TRUE );
        }
        else
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.FALSE );
        }

        return runtimeInfo;
    }

    private List getPomFiles( File basedir, String[] includes, String[] excludes )
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( basedir );

        scanner.setIncludes( includes );

        scanner.setExcludes( excludes );

        scanner.scan();

        List poms = new ArrayList();

        for ( int i = 0; i < scanner.getIncludedFiles().length; i++ )
        {
            poms.add( new File( basedir, scanner.getIncludedFiles()[i] ) );
        }

        return poms;
    }

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    public void start()
        throws MavenEmbedderException
    {
        // ----------------------------------------------------------------------
        // Set the maven.home system property which is need by components like
        // the plugin registry builder.
        // ----------------------------------------------------------------------

        if ( classLoader == null )
        {
            throw new IllegalStateException( "A classloader must be specified using setClassLoader(ClassLoader)." );
        }

        embedder = new Embedder();

        if ( logger != null )
        {
            embedder.setLoggerManager( new MavenEmbedderLoggerManager( new PlexusLoggerAdapter( logger ) ) );
        }

        try
        {
            ClassWorld classWorld = new ClassWorld();

            classWorld.newRealm( "plexus.core", classLoader );

            embedder.start( classWorld );

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            modelWriter = new MavenXpp3Writer();

            pluginDescriptorBuilder = new PluginDescriptorBuilder();

            profileManager = new DefaultProfileManager( embedder.getContainer() );

            mavenProjectBuilder = (MavenProjectBuilder) embedder.lookup( MavenProjectBuilder.ROLE );

            artifactRepositoryFactory = (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

            lifecycleExecutor = (LifecycleExecutor) embedder.lookup( LifecycleExecutor.ROLE );

            wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );

            // ----------------------------------------------------------------------
            // If an explicit local repository has not been set then we will use the
            // setting builder to use the maven defaults to help us find one.
            // ----------------------------------------------------------------------

            if ( localRepositoryDirectory == null )
            {
                settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );

                try
                {
                    settings = settingsBuilder.buildSettings();
                }
                catch ( IOException e )
                {
                    throw new MavenEmbedderException( "Error creating settings.", e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MavenEmbedderException( "Error creating settings.", e );
                }
            }
            else
            {
                settings = new Settings();

                settings.setLocalRepository( localRepositoryDirectory.getAbsolutePath() );
            }

            settings.setRuntimeInfo( createRuntimeInfo( settings ) );

            settings.setOffline( offline );

            settings.setUsePluginRegistry( usePluginRegistry );

            settings.setInteractiveMode( interactiveMode );

            localRepository = createLocalRepository( settings );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Cannot start Plexus embedder.", e );
        }
        catch ( DuplicateRealmException e )
        {
            throw new MavenEmbedderException( "Cannot create Classworld realm for the embedder.", e );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
    }

    public void stop()
        throws MavenEmbedderException
    {
        try
        {
            embedder.release( mavenProjectBuilder );

            embedder.release( artifactRepositoryFactory );

            embedder.release( settingsBuilder );

            embedder.release( lifecycleExecutor );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Cannot stop the embedder.", e );
        }
    }
}
