package org.apache.maven.lifecycle;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.MavenPluginDiscoverer;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;

public class LifecycleExecutorTest
    extends PlexusTestCase
{
    @Requirement
    private MavenProjectBuilder projectBuilder;
    
    @Requirement
    private RepositorySystem repositorySystem;
    
    @Requirement
    private PluginManager pluginManager;
    
    @Requirement
    private DefaultLifecycleExecutor lifecycleExecutor;

    protected void setUp()
        throws Exception
    {
        //!!jvz need these injected into the test cases as this is a pita.
        projectBuilder = lookup( MavenProjectBuilder.class );        
        repositorySystem = lookup( RepositorySystem.class );        
        pluginManager = lookup( PluginManager.class );        
        lifecycleExecutor = (DefaultLifecycleExecutor) lookup( LifecycleExecutor.class );
    }

    public void testLifecyclePhases()
    {
        assertNotNull( lifecycleExecutor.getLifecyclePhases() );
    }
    
    public void testMojoExecution()
        throws Exception
    {
        // - find the plugin [extension point: any client may wish to do whatever they choose]
        // - load the plugin into a classloader [extension point: we want to take them from a repository, some may take from disk or whatever]
        // - configure the plugin [extension point]
        // - execute the plugin    
        
        // Our test POM and this is actually the Maven POM so not the best idea.
        File pom = new File( getBasedir(), "src/test/pom.xml" );
        File targetPom = new File( getBasedir(), "target/lifecycle-executor/pom-plugin.xml" );
        FileUtils.copyFile( pom, targetPom );
        if ( !targetPom.getParentFile().exists() )
        {
            targetPom.getParentFile().mkdirs();
        }
        
        ArtifactRepository localRepository = repositorySystem.createLocalRepository( new File( "/Users/jvanzyl/.m2/repository" ) ); 
        
        Repository repository = new Repository();
        repository.setUrl( "http://repo1.maven.org/maven2" );
        repository.setId( "central" );
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( repositorySystem.buildArtifactRepository( repository ) ) );
        
        MavenProject project = projectBuilder.build( targetPom, configuration );              
        assertEquals( "maven", project.getArtifactId() );
        assertEquals( "3.0-SNAPSHOT", project.getVersion() );
                
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setProjectPresent( true )
            .setPluginGroups( Arrays.asList( new String[]{ "org.apache.maven.plugins"} ) )
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( repositorySystem.buildArtifactRepository( repository ) ) )
            .setProperties( new Properties() );
                                      
        MavenSession session = new MavenSession( getContainer(), request, null, null );
        session.setCurrentProject( project );
              
        String pluginArtifactId = "remote-resources";
        String goal = "process";
        MojoDescriptor mojoDescriptor = lifecycleExecutor.getMojoDescriptor( pluginArtifactId + ":" + goal, session, project );        
        
        PluginDescriptor pd = mojoDescriptor.getPluginDescriptor();
        assertNotNull( pd );
        assertEquals( "org.apache.maven.plugins", pd.getGroupId() );
        assertEquals( "maven-remote-resources-plugin", pd.getArtifactId() );
        assertEquals( "1.0", pd.getVersion() );        
        
        MojoExecution me = new MojoExecution( mojoDescriptor );       
               
        // Need some xpath action in here. Make sure the mojoExecution configuration is intact
                
        // Now the magical mojo descriptor is complete and I can execute the mojo.
        pluginManager.executeMojo( project, me, session );        
    }
    
    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );        
        containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
    }    
}
