package org.apache.maven.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
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
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( session, "resources:resources" );
        assertEquals( 1, lifecyclePlan.size() );
        MojoExecution mojoExecution = lifecyclePlan.get( 0 );
        assertNotNull( mojoExecution );
        assertEquals( "org.apache.maven.plugins", mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId() );
        assertEquals( "maven-resources-plugin", mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId() );
        assertEquals( "2.3", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion() );
    }

    public void testCalculationOfBuildPlanWithIndividualTaskOfTheCleanLifecycle()
        throws Exception
    {
        // We are doing something like "mvn clean:clean" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( session, "clean" );
        assertEquals( 1, lifecyclePlan.size() );
        MojoExecution mojoExecution = lifecyclePlan.get( 0 );
        assertNotNull( mojoExecution );
        assertEquals( "org.apache.maven.plugins", mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId() );
        assertEquals( "maven-clean-plugin", mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId() );
        assertEquals( "2.3", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion() );
    }

    // We need to take in multiple lifecycles
    public void testCalculationOfBuildPlanTasksOfTheCleanLifecycleAndTheInstallLifecycle()
        throws Exception
    {
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( session, "clean", "install" );        
                        
        //[01] clean:clean
        //[02] resources:resources
        //[03] compiler:compile
        //[04] plexus-component-metadata:generate-metadata
        //[05] resources:testResources
        //[06] compiler:testCompile
        //[07] plexus-component-metadata:generate-test-metadata
        //[08] surefire:test
        //[09] jar:jar
        //[10] install:install
        //
        assertEquals( 10, lifecyclePlan.size() );
                
        assertEquals( "clean:clean", lifecyclePlan.get( 0 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:resources", lifecyclePlan.get( 1 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:compile", lifecyclePlan.get( 2 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-metadata", lifecyclePlan.get( 3 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:testResources", lifecyclePlan.get( 4 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:testCompile", lifecyclePlan.get( 5 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-test-metadata", lifecyclePlan.get( 6 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "surefire:test", lifecyclePlan.get( 7 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "jar:jar", lifecyclePlan.get( 8 ).getMojoDescriptor().getFullGoalName() );                
        assertEquals( "install:install", lifecyclePlan.get( 9 ).getMojoDescriptor().getFullGoalName() );                
    }

    // We need to take in multiple lifecycles
    public void testCalculationOfBuildPlanWithMultipleExecutionsOfModello()
        throws Exception
    {
        File pom = getProject( "project-with-multiple-executions" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-multiple-executions", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0.1", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( session, "clean", "install" );        
        
        //[01] clean:clean
        //[02] modello:xpp3-writer
        //[03] modello:java
        //[04] modello:xpp3-reader
        //[05] modello:xpp3-writer
        //[06] modello:java
        //[07] modello:xpp3-reader
        //[08] plugin:descriptor        
        //[09] resources:resources
        //[10] compiler:compile
        //[11] resources:testResources
        //[12] compiler:testCompile
        //[13] surefire:test
        //[14] plugin:addPluginArtifactMetadata        
        //[15] jar:jar
        //[16] install:install
        //
        
        assertEquals( 16, lifecyclePlan.size() );        
                
        assertEquals( "clean:clean", lifecyclePlan.get( 0 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-writer", lifecyclePlan.get( 1 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:java", lifecyclePlan.get( 2 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-reader", lifecyclePlan.get( 3 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-writer", lifecyclePlan.get( 4 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:java", lifecyclePlan.get( 5 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-reader", lifecyclePlan.get( 6 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plugin:descriptor", lifecyclePlan.get( 7 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:resources", lifecyclePlan.get( 8 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:compile", lifecyclePlan.get( 9 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:testResources", lifecyclePlan.get( 10 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:testCompile", lifecyclePlan.get( 11 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "surefire:test", lifecyclePlan.get( 12 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plugin:addPluginArtifactMetadata", lifecyclePlan.get( 13 ).getMojoDescriptor().getFullGoalName() );                
        assertEquals( "jar:jar", lifecyclePlan.get( 14 ).getMojoDescriptor().getFullGoalName() );                
        assertEquals( "install:install", lifecyclePlan.get( 15 ).getMojoDescriptor().getFullGoalName() );
        
        assertEquals( "src/main/mdo/remote-resources.mdo", new MojoExecutionXPathContainer( lifecyclePlan.get( 1 ) ).getValue( "configuration/models[1]/model" ) );
        assertEquals( "src/main/mdo/supplemental-model.mdo", new MojoExecutionXPathContainer( lifecyclePlan.get( 4 ) ).getValue( "configuration/models[1]/model" ) );
    }        
    
    public void testCalculationOfBuildPlanWithIndividualTaskOfTheCleanCleanGoal()
        throws Exception
    {
        // We are doing something like "mvn clean:clean" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( session, "clean:clean" );
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
        List<MojoExecution> lifecyclePlan = lifecycleExecutor.calculateBuildPlan( session, "package" );
        
        //[01] resources:resources
        //[02] compiler:compile
        //[03] plexus-component-metadata:generate-metadata
        //[04] resources:testResources
        //[05] compiler:testCompile
        //[06] plexus-component-metadata:generate-test-metadata
        //[07] surefire:test
        //[08] jar:jar
        //
        assertEquals( 8, lifecyclePlan.size() );
                
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
    
    // Prefixes
    
    public void testFindingPluginPrefixforCleanClean()
        throws Exception
    {
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        Plugin plugin = lifecycleExecutor.findPluginForPrefix( "clean", session );
        assertNotNull( plugin );
    }
}
