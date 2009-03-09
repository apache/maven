package org.apache.maven.lifecycle;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.MavenPluginDiscoverer;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.realm.DefaultMavenRealmManager;
import org.apache.maven.realm.MavenRealmManager;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.console.ConsoleLogger;

public class LifecycleExecutorTest
    extends PlexusTestCase
{
    private MavenProjectBuilder projectBuilder;
    
    private RepositorySystem repositorySystem;
    
    private PluginManager pluginManager;
    
    private DefaultLifecycleExecutor lifecycleExecutor;

    protected void setUp()
        throws Exception
    {
        projectBuilder = lookup( MavenProjectBuilder.class );        
        repositorySystem = lookup( RepositorySystem.class );        
        pluginManager = lookup( PluginManager.class );        
        lifecycleExecutor = (DefaultLifecycleExecutor) lookup( LifecycleExecutor.class );
    }

    public void testMojoExecution()
        throws Exception
    {
        // - find the plugin [extension point: any client may wish to do whatever they choose]
        // - load the plugin into a classloader [extension point: we want to take them from a repository, some may take from disk or whatever]
        // - configure the plugin [extension point]
        // - execute the plugin    
        
        File pom = new File( getBasedir(), "src/test/pom.xml" );
       
        // For testing I want to use my standard local repository and settings.
        
        ArtifactRepository localRepository = repositorySystem.createLocalRepository( new File( "/Users/jvanzyl/.m2/repository" ) ); 
        
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository )
            .setRemoteRepositories( null );
        
        MavenProject project = projectBuilder.build( pom, configuration );
        
        // now i want to grab the configuration for the remote resources plugin
        
        assertEquals( "maven", project.getArtifactId() );
        
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-remote-resources-plugin" );
        // The version should be specified in the POM.
        
        MavenRealmManager realmManager = new DefaultMavenRealmManager( getContainer(), new ConsoleLogger( 0, "logger" ) );        
        MavenSession session = new MavenSession( localRepository, realmManager );
        
        PluginDescriptor pd = pluginManager.loadPlugin( plugin, project, session );        
        assertNotNull( pd );
        assertEquals( "org.apache.maven.plugins", pd.getGroupId() );
        assertEquals( "maven-remote-resources-plugin", pd.getArtifactId() );
        assertEquals( "1.0", pd.getVersion() );        
        
        MojoDescriptor mojoDescriptor = pd.getMojo( "process" );
        assertNotNull( mojoDescriptor );
        System.out.println( "configuration >>> " + mojoDescriptor.getConfiguration() );
    }
    
    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );        
        containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
    }    
}
