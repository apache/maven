package org.apache.maven.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
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
        List<MojoExecution> executionPlan = lifecycleExecutor.calculateExecutionPlan( session, "resources:resources" ).getExecutions();
        assertEquals( 1, executionPlan.size() );
        MojoExecution mojoExecution = executionPlan.get( 0 );
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
        List<MojoExecution> executionPlan = lifecycleExecutor.calculateExecutionPlan( session, "clean" ).getExecutions();
        assertEquals( 1, executionPlan.size() );
        MojoExecution mojoExecution = executionPlan.get( 0 );
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
        List<MojoExecution> executionPlan = lifecycleExecutor.calculateExecutionPlan( session, "clean", "install" ).getExecutions();        
                        
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
        assertEquals( 10, executionPlan.size() );
                
        assertEquals( "clean:clean", executionPlan.get( 0 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:resources", executionPlan.get( 1 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:compile", executionPlan.get( 2 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-metadata", executionPlan.get( 3 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:testResources", executionPlan.get( 4 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:testCompile", executionPlan.get( 5 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-test-metadata", executionPlan.get( 6 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "surefire:test", executionPlan.get( 7 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "jar:jar", executionPlan.get( 8 ).getMojoDescriptor().getFullGoalName() );                
        assertEquals( "install:install", executionPlan.get( 9 ).getMojoDescriptor().getFullGoalName() );                
    }

    // We need to take in multiple lifecycles
    public void testCalculationOfBuildPlanWithMultipleExecutionsOfModello()
        throws Exception
    {
        File pom = getProject( "project-with-multiple-executions" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-multiple-executions", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0.1", session.getCurrentProject().getVersion() );
        
        MavenExecutionPlan plan = lifecycleExecutor.calculateExecutionPlan( session, "clean", "install" );
        
        assertTrue( plan.getRequiredResolutionScopes().contains( Artifact.SCOPE_COMPILE ) );
        assertTrue( plan.getRequiredResolutionScopes().contains( Artifact.SCOPE_RUNTIME ) );
        assertTrue( plan.getRequiredResolutionScopes().contains( Artifact.SCOPE_TEST ) );
        
        List<MojoExecution> executions = plan.getExecutions();        
        
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
        //[14] jar:jar
        //[15] plugin:addPluginArtifactMetadata        
        //[16] install:install
        //
        
        assertEquals( 16, executions.size() );        
                
        assertEquals( "clean:clean", executions.get( 0 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-writer", executions.get( 1 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:java", executions.get( 2 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-reader", executions.get( 3 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-writer", executions.get( 4 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:java", executions.get( 5 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "modello:xpp3-reader", executions.get( 6 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plugin:descriptor", executions.get( 7 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:resources", executions.get( 8 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:compile", executions.get( 9 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:testResources", executions.get( 10 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:testCompile", executions.get( 11 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "surefire:test", executions.get( 12 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "jar:jar", executions.get( 13 ).getMojoDescriptor().getFullGoalName() );                
        assertEquals( "plugin:addPluginArtifactMetadata", executions.get( 14 ).getMojoDescriptor().getFullGoalName() );                
        assertEquals( "install:install", executions.get( 15 ).getMojoDescriptor().getFullGoalName() );
        
        assertEquals( "src/main/mdo/remote-resources.mdo", new MojoExecutionXPathContainer( executions.get( 1 ) ).getValue( "configuration/models[1]/model" ) );
        assertEquals( "src/main/mdo/supplemental-model.mdo", new MojoExecutionXPathContainer( executions.get( 4 ) ).getValue( "configuration/models[1]/model" ) );
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
        List<MojoExecution> executionPlan = lifecycleExecutor.calculateExecutionPlan( session, "clean:clean" ).getExecutions();
        assertEquals( 1, executionPlan.size() );
        MojoExecution mojoExecution = executionPlan.get( 0 );
        assertNotNull( mojoExecution );
        assertEquals( "org.apache.maven.plugins", mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId() );
        assertEquals( "maven-clean-plugin", mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId() );
        assertEquals( "2.3", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion() );
    }
    
    public void testLifecycleQueryingUsingADefaultLifecyclePhase()
        throws Exception
    {   
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenSession session = createMavenSession( pom );
        assertEquals( "project-with-additional-lifecycle-elements", session.getCurrentProject().getArtifactId() );
        assertEquals( "1.0", session.getCurrentProject().getVersion() );
        List<MojoExecution> executionPlan = lifecycleExecutor.calculateExecutionPlan( session, "package" ).getExecutions();
        
        //[01] resources:resources
        //[02] compiler:compile
        //[03] plexus-component-metadata:generate-metadata
        //[04] resources:testResources
        //[05] compiler:testCompile
        //[06] plexus-component-metadata:generate-test-metadata
        //[07] surefire:test
        //[08] jar:jar
        //
        assertEquals( 8, executionPlan.size() );
                
        assertEquals( "resources:resources", executionPlan.get( 0 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:compile", executionPlan.get( 1 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-metadata", executionPlan.get( 2 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "resources:testResources", executionPlan.get( 3 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "compiler:testCompile", executionPlan.get( 4 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "plexus-component-metadata:generate-test-metadata", executionPlan.get( 5 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "surefire:test", executionPlan.get( 6 ).getMojoDescriptor().getFullGoalName() );
        assertEquals( "jar:jar", executionPlan.get( 7 ).getMojoDescriptor().getFullGoalName() );        
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
        MojoDescriptor mojoDescriptor =
            lifecycleExecutor.getMojoDescriptor( "org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process",
                                                 session, session.getCurrentProject() );
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
