package org.apache.maven.plugin;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.annotations.Requirement;

public class PluginManagerTest
    extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private PluginManager pluginManager;
    
    private String plexusVersion = "1.0-beta-3.0.7-SNAPSHOT";
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        pluginManager = lookup( PluginManager.class );
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }
                
    public void testPluginLoading()
        throws Exception
    {
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.codehaus.plexus" );
        plugin.setArtifactId( "plexus-component-metadata" );
        plugin.setVersion( plexusVersion );
        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin( plugin, session.getCurrentProject(), session );
        assertNotNull( pluginDescriptor );
        assertNotNull( pluginDescriptor.getClassRealm() );
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
        
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session );        
        assertNotNull( mojoDescriptor );
        assertEquals( "generate-metadata", mojoDescriptor.getGoal() );
        assertNotNull( mojoDescriptor.getRealm() );
        mojoDescriptor.getRealm().display();
        
        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
        assertNotNull( pluginDescriptor );
        assertEquals( "org.codehaus.plexus", pluginDescriptor.getGroupId() );
        assertEquals( "plexus-component-metadata", pluginDescriptor.getArtifactId() );
        assertEquals( plexusVersion, pluginDescriptor.getVersion() );
        assertNotNull( pluginDescriptor.getClassRealm() );
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
        String goal = "process";
        
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-remote-resources-plugin" );
        plugin.setVersion( "1.1" );
        
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session );
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-remote-resources-plugin", "1.1" );
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
        pluginManager.executeMojo( session, mojoExecution );
    }

    public void testSurefirePlugin()
        throws Exception
    {
        MavenSession session = createMavenSession( getProject( "project-with-inheritance" ) );       
        String goal = "test";

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-surefire-plugin" );
        plugin.setVersion( "2.4.2" );
        
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( plugin, goal, session );                
        assertPluginDescriptor( mojoDescriptor, "org.apache.maven.plugins", "maven-surefire-plugin", "2.4.2" );
        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
        pluginManager.executeMojo( session, mojoExecution );
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
