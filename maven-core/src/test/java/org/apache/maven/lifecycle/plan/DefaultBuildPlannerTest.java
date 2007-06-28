package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.plan.testutils.TestPluginLoader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class DefaultBuildPlannerTest
    extends PlexusTestCase
{

    private DefaultBuildPlanner buildPlanner;

    private TestPluginLoader pluginLoader;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        buildPlanner = (DefaultBuildPlanner) lookup( BuildPlanner.class.getName(), "default" );
        pluginLoader = (TestPluginLoader) lookup( PluginLoader.class.getName(), "default" );
    }

    public void test_constructBuildPlan_ForkedPhaseFromMojoBoundInThatPhase()
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException
    {
        Model model = new Model();

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-assembly-plugin" );
        plugin.setVersion( "1" );

        build.addPlugin( plugin );

        PluginExecution exec = new PluginExecution();
        exec.setId( "assembly" );
        exec.setPhase( "package" );
        exec.addGoal( "assembly" );

        plugin.addExecution( exec );

        PluginDescriptor pd = TestPluginLoader.createPluginDescriptor( plugin.getArtifactId(), "assembly",
                                                                       plugin.getGroupId(), plugin.getVersion() );
        MojoDescriptor md = TestPluginLoader.createMojoDescriptor( pd, "assembly" );
        md.setExecutePhase( "package" );

        pluginLoader.addPluginDescriptor( pd );

        MavenProject project = new MavenProject( model );

        BuildPlan plan = buildPlanner.constructBuildPlan( Collections.singletonList( "package" ), project );

        List rendered = plan.renderExecutionPlan( new Stack() );

        List checkIds = new ArrayList();

        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:resources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:compile" );
        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:testResources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:testCompile" );
        checkIds.add( "org.apache.maven.plugins:maven-surefire-plugin:test" );
        checkIds.add( "org.apache.maven.plugins:maven-jar-plugin:jar" );
        checkIds.add( "org.apache.maven.plugins.internal:maven-state-management:2.1:start-fork" );
        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:resources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:compile" );
        checkIds.add( "org.apache.maven.plugins:maven-resources-plugin:testResources" );
        checkIds.add( "org.apache.maven.plugins:maven-compiler-plugin:testCompile" );
        checkIds.add( "org.apache.maven.plugins:maven-surefire-plugin:test" );
        checkIds.add( "org.apache.maven.plugins:maven-jar-plugin:jar" );
        checkIds.add( "org.apache.maven.plugins.internal:maven-state-management:2.1:end-fork" );
        checkIds.add( "org.apache.maven.plugins:maven-assembly-plugin:1:assembly" );
        checkIds.add( "org.apache.maven.plugins.internal:maven-state-management:2.1:clear-fork-context" );

        assertBindingIds( rendered, checkIds );
    }

    private void assertBindingIds( List bindings, List checkIds )
    {
        assertEquals( checkIds.size(), bindings.size() );

        for ( int i = 0; i < bindings.size(); i++ )
        {
            MojoBinding binding = (MojoBinding) bindings.get( i );
            String checkId = (String) checkIds.get( i );

            assertEquals( checkId, MojoBindingUtils.toString( binding ) );
        }
    }

}
