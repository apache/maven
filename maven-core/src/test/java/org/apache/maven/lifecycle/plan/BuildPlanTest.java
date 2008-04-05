package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import junit.framework.TestCase;

public class BuildPlanTest
    extends TestCase
{

    public void testRender_OneDirectInvocationWithForkedPhase_NoLifecycleOverlay()
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        List check = new ArrayList();

        check.add( StateManagementUtils.createStartForkedExecutionMojoBinding() );

        MojoBinding mb = new MojoBinding();
        mb.setGroupId( "test" );
        mb.setArtifactId( "test-plugin" );
        mb.setVersion( "1" );
        mb.setGoal( "validate" );

        check.add( mb );

        BuildBinding binding = new BuildBinding();
        binding.getValidate().addBinding( mb );

        mb = new MojoBinding();
        mb.setGroupId( "test" );
        mb.setArtifactId( "test-plugin" );
        mb.setVersion( "1" );
        mb.setGoal( "generate-sources" );

        check.add( mb );

        check.add( StateManagementUtils.createEndForkedExecutionMojoBinding() );

        binding.getGenerateSources().addBinding( mb );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setBuildBinding( binding );

        List tasks = Collections.singletonList( "eclipse:eclipse" );

        MojoBinding eclipseBinding = new MojoBinding();
        eclipseBinding.setGroupId( "org.apache.maven.plugins" );
        eclipseBinding.setArtifactId( "maven-eclipse-plugin" );
        eclipseBinding.setVersion( "2.3" );
        eclipseBinding.setGoal( "eclipse" );

        check.add( eclipseBinding );

        check.add( StateManagementUtils.createClearForkedExecutionMojoBinding() );

        BuildPlan plan = new BuildPlan( new LifecycleBindings(), new HashSet(), tasks );

        plan.addDirectInvocationBinding( "eclipse:eclipse", eclipseBinding );

        plan.addForkedExecution( eclipseBinding, new BuildPlan( bindings,
                                                                new HashSet(),
                                                                Collections.singletonList( "generate-sources" ) ) );

        List executionPlan = plan.renderExecutionPlan( new Stack() );

        assertBindings( check, executionPlan );
    }

    public void testRender_MojoBoundToPackagePhaseAndForkingPackagePhaseGetsFilteredOut()
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        MojoBinding mb = new MojoBinding();
        mb.setGroupId( "test" );
        mb.setArtifactId( "test-plugin" );
        mb.setVersion( "1" );
        mb.setGoal( "validate" );

        BuildBinding binding = new BuildBinding();
        binding.getValidate().addBinding( mb );

        MojoBinding mb2 = new MojoBinding();
        mb2.setGroupId( "test" );
        mb2.setArtifactId( "test-plugin" );
        mb2.setVersion( "1" );
        mb2.setGoal( "generate-sources" );

        binding.getGenerateSources().addBinding( mb2 );

        MojoBinding assemblyBinding = new MojoBinding();
        assemblyBinding.setGroupId( "org.apache.maven.plugins" );
        assemblyBinding.setArtifactId( "maven-assembly-plugin" );
        assemblyBinding.setVersion( "2.1" );
        assemblyBinding.setGoal( "assembly" );

        binding.getCreatePackage().addBinding( assemblyBinding );

        List check = new ArrayList();

        check.add( mb );
        check.add( mb2 );
        check.add( StateManagementUtils.createStartForkedExecutionMojoBinding() );
        check.add( mb );
        check.add( mb2 );
        check.add( StateManagementUtils.createEndForkedExecutionMojoBinding() );
        check.add( assemblyBinding );
        check.add( StateManagementUtils.createClearForkedExecutionMojoBinding() );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setBuildBinding( binding );

        List tasks = Collections.singletonList( "package" );

        BuildPlan plan = new BuildPlan( bindings, new HashSet(), tasks );

        plan.addForkedExecution( assemblyBinding, plan.copy( "package" ) );

        List executionPlan = plan.renderExecutionPlan( new Stack() );

        assertBindings( check, executionPlan );
    }

    private void assertBindings( final List check, final List executionPlan )
    {
        assertNotNull( executionPlan );

        System.out.println( "\n\nExpected execution plan:\n" + String.valueOf( check ).replace( ',', '\n' ) );
        System.out.println( "\nActual execution plan:\n" + String.valueOf( executionPlan ).replace( ',', '\n' ) );

        assertEquals( "Execution plan does not contain the expected number of mojo bindings.", check.size(),
                      executionPlan.size() );
        for ( int i = 0; i < check.size(); i++ )
        {
            MojoBinding checkBinding = (MojoBinding) check.get( i );
            MojoBinding realBinding = (MojoBinding) executionPlan.get( i );

            assertEquals( "Expected mojo binding does not match execution plan.",
                          MojoBindingUtils.createMojoBindingKey( checkBinding, true ),
                          MojoBindingUtils.createMojoBindingKey( realBinding, true ) );
        }
    }
}
