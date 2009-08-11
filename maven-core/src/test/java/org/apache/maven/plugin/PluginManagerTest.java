package org.apache.maven.plugin;

import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;

public class PluginManagerTest
    extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private DefaultPluginManager pluginManager;
    
    private String plexusVersion = "1.0-beta-3.0.7";
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        pluginManager = (DefaultPluginManager) lookup( PluginManager.class );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        pluginManager = null;
        super.tearDown();
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/plugin-manager";
    }

    private RepositoryRequest getRepositoryRequest( MavenSession session )
    {
        RepositoryRequest request = new DefaultRepositoryRequest();

        request.setLocalRepository( session.getLocalRepository() );
        request.setRemoteRepositories( session.getCurrentProject().getPluginArtifactRepositories() );

        return request;
    }

    public void testPluginLoading()
        throws Exception
    {
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.codehaus.plexus" );
        plugin.setArtifactId( "plexus-component-metadata" );
        plugin.setVersion( plexusVersion );
        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin( plugin, getRepositoryRequest( session ) );
        assertNotNull( pluginDescriptor );
    }
    
    public void testMojoDescriptorRetrieval()
        throws Exception
    {
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        String goal = "generate-metadata";
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.codehaus.plexus" );
        plugin.setArtifactId( "plexus-component-metadata" );
        plugin.setVersion( plexusVersion );
        
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, getRepositoryRequest( session ) );        
        assertNotNull( mojoDescriptor );
        assertEquals( "generate-metadata", mojoDescriptor.getGoal() );
        // igorf: plugin realm comes later
        // assertNotNull( mojoDescriptor.getRealm() );
        
        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
        assertNotNull( pluginDescriptor );
        assertEquals( "org.codehaus.plexus", pluginDescriptor.getGroupId() );
        assertEquals( "plexus-component-metadata", pluginDescriptor.getArtifactId() );
        assertEquals( plexusVersion, pluginDescriptor.getVersion() );
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
        //TODO: turn an equivalent back on when the RR plugin is released.
        
        /*

        This will not work until the RR plugin is released to get rid of the binding to the reporting exception which is a mistake.
        
        This happpens after removing the reporting API from the core:
        
        java.lang.NoClassDefFoundError: org/apache/maven/reporting/MavenReportException
        
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        String goal = "process";
        
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-remote-resources-plugin" );
        plugin.setVersion( "1.0-beta-2" );
        
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session.getCurrentProject(), session.getLocalRepository() );        
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-remote-resources-plugin", "1.0-beta-2" );
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
        pluginManager.executeMojo( session, mojoExecution );
        */
    }
    
    //TODO: this will be the basis of the customizable lifecycle execution so need to figure this out quickly.
    public void testSurefirePlugin()
        throws Exception
    {
        /*
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );
        String goal = "test";

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-surefire-plugin" );
        plugin.setVersion( "2.4.2" );

        // The project has already been fully interpolated so getting the raw mojoDescriptor is not going to have the processes configuration.
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session.getLocalRepository(), session.getCurrentProject().getPluginArtifactRepositories() );        
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-surefire-plugin", "2.4.2" );
        
        System.out.println( session.getCurrentProject().getBuild().getPluginsAsMap() );
        
        Xpp3Dom configuration = (Xpp3Dom) session.getCurrentProject().getBuild().getPluginsAsMap().get( plugin.getKey() ).getExecutions().get( 0 ).getConfiguration();
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, configuration );
        pluginManager.executeMojo( session, mojoExecution );
        */
    }
    
    public void testMojoConfigurationIsMergedCorrectly()
        throws Exception
    {
    }
    
    /**
     * The case where the user wants to specify an alternate version of the underlying tool. Common case
     * is in the Antlr plugin which comes bundled with a version of Antlr but the user often times needs
     * to use a specific version. We need to make sure the version that they specify takes precedence.
     */
    public void testMojoWhereInternallyStatedDependencyIsOverriddenByProject()
        throws Exception
    {
    }

    /** 
     * The case where you have a plugin in the current build that you want to be used on projects in
     * the current build.
     */
    public void testMojoThatIsPresentInTheCurrentBuild()
        throws Exception
    {
    }

    /**
     * This is the case where the Mojo wants to execute on every project and then do something at the end
     * with the results of each project.
     */
    public void testAggregatorMojo()
        throws Exception
    {
    }

    /**
     * This is the case where a Mojo needs the lifecycle run to a certain phase before it can do
     * anything useful.
     */
    public void testMojoThatRequiresExecutionToAGivenPhaseBeforeExecutingItself()
        throws Exception
    {
    }
    
    // test that mojo which does not require dependency resolution trigger no downloading of dependencies
    
    // test interpolation of basedir values in mojo configuration
    
    // test a build where projects use different versions of the same plugin
    
    public void testThatPluginDependencyThatHasSystemScopeIsResolved()
        throws Exception
    {
        /*
        File systemPath = new File( getBasedir(), "pom.xml" );

        Plugin plugin = new PluginBuilder( "org.apache.maven", "project-test", "1.0" )
            .addDependency( "org.apache.maven", "system-dependency", "1.0", Artifact.SCOPE_SYSTEM, systemPath.getAbsolutePath() )
            .get();        

        MavenProject pluginProject = new ProjectBuilder( "org.apache.maven", "project-test", "1.0" )
            .addPlugin( plugin )
            .addDependency( "junit", "junit", "3.8.1", Artifact.SCOPE_COMPILE )
            .get();        
        
        // i'm making this artifact which is assumed to come from a pom in the metadata processor, then it tries to create a POM artifact
        // and parse it for the dependencies and it blows up.
        //
        // we need to pass this through as is so it doesn't get parsed again.
        Artifact pluginArtifact = new ProjectArtifact( pluginProject );
        
        Set<Artifact> artifacts = pluginManager.getPluginArtifacts( pluginArtifact, plugin, getLocalRepository(), getPluginArtifactRepositories() );
        System.out.println( artifacts );
        */
        
        MavenSession session = createMavenSession( getProject( "project-contributing-system-scope-plugin-dep" ) );
        MavenProject project = session.getCurrentProject();
        Plugin plugin = project.getPlugin( "org.apache.maven.its.plugins:maven-it-plugin-class-loader" );                
        Artifact pluginArtifact = project.getPluginArtifactMap().get( "org.apache.maven.its.plugins:maven-it-plugin-class-loader" ); 
        
        /*
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( pluginArtifact )
            .setLocalRepository( getLocalRepository() )
            .setRemoteRepostories( getPluginArtifactRepositories() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );
        */
        
        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setLocalRepository( getLocalRepository() );
        repositoryRequest.setRemoteRepositories( getPluginArtifactRepositories() );
        
        List<Artifact> artifacts = pluginManager.getPluginArtifacts( pluginArtifact, plugin, repositoryRequest );   
        
        for ( Artifact a : artifacts )
        {
            if ( a.getGroupId().equals( "org.apache.maven.its.mng3586" ) && a.getArtifactId().equals( "tools" ) )
            {
                // The system scoped dependencies will be present in the classloader for the plugin
                return;
            }
        }
        
        fail( "Can't find the system scoped dependency in the plugin artifacts." );
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
}
