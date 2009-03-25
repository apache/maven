package org.apache.maven.lifecycle;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractCoreMavenComponentTest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.event.DefaultEventMonitor;
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
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;

public class LifecycleExecutorTest
    extends AbstractCoreMavenComponentTest
{
    @Requirement
    private PluginManager pluginManager;

    @Requirement
    private DefaultLifecycleExecutor lifecycleExecutor;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        pluginManager = lookup( PluginManager.class );
        lifecycleExecutor = (DefaultLifecycleExecutor) lookup( LifecycleExecutor.class );
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }
        
    // -----------------------------------------------------------------------------------------------
    // 
    // -----------------------------------------------------------------------------------------------    
    
    public void testLifecyclePhases()
    {
        assertNotNull( lifecycleExecutor.getLifecyclePhases() );
    }

    // -----------------------------------------------------------------------------------------------
    // Tests which exercise the lifecycle executor when it is dealing with default lifecycle phases.
    // -----------------------------------------------------------------------------------------------
    
    public void testLifecycleQueryingUsingADefaultLifecyclePhase()
        throws Exception
    {   
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0-SNAPSHOT", session.getCurrentProject().getVersion() );
        List<MojoDescriptor> lifecyclePlan = lifecycleExecutor.calculateLifecyclePlan( "package", session );
        
        // resources:resources
        // compiler:compile
        // plexus-component-metadata:generate-metadata
        // resources:testResources
        // compiler:testCompile
        // plexus-component-metadata:generate-test-metadata
        // surefire:test
        // jar:jar
        
        assertEquals( "resources:resources", lifecyclePlan.get( 0 ).getFullGoalName() );
        assertEquals( "compiler:compile", lifecyclePlan.get( 1 ).getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-metadata", lifecyclePlan.get( 2 ).getFullGoalName() );
        assertEquals( "resources:testResources", lifecyclePlan.get( 3 ).getFullGoalName() );
        assertEquals( "compiler:testCompile", lifecyclePlan.get( 4 ).getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-test-metadata", lifecyclePlan.get( 5 ).getFullGoalName() );
        assertEquals( "surefire:test", lifecyclePlan.get( 6 ).getFullGoalName() );
        assertEquals( "jar:jar", lifecyclePlan.get( 7 ).getFullGoalName() );        
    }    
    
    public void testLifecycleExecutionUsingADefaultLifecyclePhase()
        throws Exception
    {
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );        
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0-SNAPSHOT", session.getCurrentProject().getVersion() );                                
        lifecycleExecutor.execute( session );
    }
    
    // -----------------------------------------------------------------------------------------------
    // Tests which exercise the lifecycle executor when it is dealing with individual goals.
    // -----------------------------------------------------------------------------------------------
    
    //TODO: These two tests display a lack of symmetry with respect to the input which is a free form string and the
    //      mojo descriptor which comes back. All the free form parsing needs to be done somewhere else, this is
    //      really the function of the CLI, and then the pre-processing of that output still needs to be fed into
    //      a hinting process which helps flesh out the full specification of the plugin. The plugin manager should
    //      only deal in concrete terms -- all version finding mumbo jumbo is a customization to base functionality
    //      the plugin manager provides.
    
    public void testRemoteResourcesPlugin()
        throws Exception
    {
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        String pluginArtifactId = "remote-resources";
        String goal = "process";
        MojoDescriptor mojoDescriptor = lifecycleExecutor.getMojoDescriptor( pluginArtifactId + ":" + goal, session, session.getCurrentProject() );
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-remote-resources-plugin", "1.0" );
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
        pluginManager.executeMojo( session.getCurrentProject(), mojoExecution, session );
    }

    public void testSurefirePlugin()
        throws Exception
    {
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        String pluginArtifactId = "surefire";
        String goal = "test";
        MojoDescriptor mojoDescriptor = lifecycleExecutor.getMojoDescriptor( pluginArtifactId + ":" + goal, session, session.getCurrentProject() );
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-surefire-plugin", "2.4.2" );
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
        pluginManager.executeMojo( session.getCurrentProject(), mojoExecution, session );
    }
    
    // -----------------------------------------------------------------------------------------------
    // Testing help
    // -----------------------------------------------------------------------------------------------

    protected void assertPluginDescriptor( MojoDescriptor mojoDescriptor, String groupId, String artifactId, String version )
    {
        assertNotNull( mojoDescriptor );        
        PluginDescriptor pd = mojoDescriptor.getPluginDescriptor();
        assertNotNull( pd );
        assertEquals( groupId, pd.getGroupId() );
        assertEquals( artifactId, pd.getArtifactId() );
        assertEquals( version, pd.getVersion() );        
    }
    
    /**
     * We need to customize the standard Plexus container with the plugin discovery listener which
     * is what looks for the META-INF/maven/plugin.xml resources that enter the system when a
     * Maven plugin is loaded.
     * 
     * We also need to customize the Plexus container with a standard plugin discovery listener
     * which is the MavenPluginCollector. When a Maven plugin is discovered the MavenPluginCollector
     * collects the plugin descriptors which are found. 
     */
    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );
        containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
    }
    
    // - remove the event monitor, just default or get rid of it
    // layer the creation of a project builder configuration with a request, but this will need to be
    // a Maven subclass because we don't want to couple maven to the project builder which we need to
    // separate.
    protected MavenSession createMavenSession( File pom )
        throws Exception
    {
        ArtifactRepository localRepository = repositorySystem.createDefaultLocalRepository();
        ArtifactRepository remoteRepository = repositorySystem.createDefaultRemoteRepository();
        
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setProjectPresent( true )
            .setPluginGroups( Arrays.asList( new String[] { "org.apache.maven.plugins" } ) )
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( remoteRepository ) )
            .setGoals( Arrays.asList( new String[] { "package" } ) )   
            // This is wrong
            .addEventMonitor( new DefaultEventMonitor( new ConsoleLogger( 0, "" ) ) )
            .setProperties( new Properties() );

        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( remoteRepository ) );

        MavenProject project = projectBuilder.build( pom, configuration );        
                        
        MavenSession session = new MavenSession( getContainer(), request, project );
        
        return session;
    }
}
