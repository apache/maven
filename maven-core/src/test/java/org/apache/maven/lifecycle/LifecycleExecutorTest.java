package org.apache.maven.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class LifecycleExecutorTest
    extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private DefaultLifecycleExecutor lifecycleExecutor;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        lifecycleExecutor = (DefaultLifecycleExecutor) lookup( LifecycleExecutor.class );
        lookup( ExceptionHandler.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        lifecycleExecutor = null;
        super.tearDown();
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
    
    public void testCalculationOfBuildPlanWithIndividualTaskWherePluginIsSpecifiedInThePom()
        throws Exception
    {
        // We are doing something like "mvn resources:resources" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( "resources:resources", session );
        assertEquals( 1, lifecyclePlan.size() );
        MojoExecution mojoExecution = lifecyclePlan.get( 0 );
        assertNotNull( mojoExecution );
        assertEquals( "org.apache.maven.plugins", mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId() );
        assertEquals( "maven-resources-plugin", mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId() );
        assertEquals( "2.3", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion() );
    }

    public void testCalculationOfBuildPlanWithIndividualTaskWherePluginIsNotSpecifiedInThePom()
        throws Exception
    {
        // We are doing something like "mvn clean:clean" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( "clean:clean", session );
        assertEquals( 1, lifecyclePlan.size() );
        MojoExecution mojoExecution = lifecyclePlan.get( 0 );
        assertNotNull( mojoExecution );
        assertEquals( "org.apache.maven.plugins", mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId() );
        assertEquals( "maven-clean-plugin", mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId() );
        assertEquals( "2.2", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion() );
    }
        
    public void testLifecycleQueryingUsingADefaultLifecyclePhase()
        throws Exception
    {   
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( "package", session );
        
        // resources:resources
        // compiler:compile
        // plexus-component-metadata:generate-metadata
        // resources:testResources
        // compiler:testCompile
        // plexus-component-metadata:generate-test-metadata
        // surefire:test
        // jar:jar
                
        assertEquals( "resources:resources", lifecyclePlan.get( 0 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:compile", lifecyclePlan.get( 1 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-metadata", lifecyclePlan.get( 2 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:testResources", lifecyclePlan.get( 3 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:testCompile", lifecyclePlan.get( 4 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-test-metadata", lifecyclePlan.get( 5 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "surefire:test", lifecyclePlan.get( 6 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "jar:jar", lifecyclePlan.get( 7 ).getMojoDescriptor().getFullGoalName() );        
    }    
        
    public void testLifecyclePluginsRetrievalForDefaultLifecycle()
        throws Exception
    {
        List<Plugin> plugins = new ArrayList<Plugin>( lifecycleExecutor.getPluginsBoundByDefaultToAllLifecycles( "jar" ) );  
                
        assertEquals( 8, plugins.size() );
    }
    
    public void testPluginConfigurationCreation()
        throws Exception
    {
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        MojoDescriptor mojoDescriptor = lifecycleExecutor.getMojoDescriptor( "org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process", session );
        Xpp3Dom dom = lifecycleExecutor.convert( mojoDescriptor );
        System.out.println( dom );
    }

    public void testPluginPrefixRetrieval()
        throws Exception
    {
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        Plugin plugin = lifecycleExecutor.findPluginForPrefix( "resources", session );
        assertEquals( "org.apache.maven.plugins", plugin.getGroupId() );
        assertEquals( "maven-resources-plugin", plugin.getArtifactId() );
    }    
}
