package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class ForkPlanModifierTest
    extends TestCase
{

    public void testModifyBindings_AddTwoMojosAheadOfCompileMojo()
        throws LifecyclePlannerException
    {
        MojoBinding mojo = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" );

        List additions = new ArrayList();
        additions.add( newMojo( "group", "artifact", "clean" ) );
        additions.add( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();

        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );

        target.getBuildBinding().getCompile().addBinding( mojo );

        assertEquals( 1, target.getBuildBinding().getCompile().getBindings().size() );

        target = new ForkPlanModifier( mojo, additions ).modifyBindings( target );

        assertEquals( 6, target.getBuildBinding().getCompile().getBindings().size() );

        Iterator it = target.getBuildBinding().getCompile().getBindings().iterator();
        
        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.START_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );

        assertMojo( "group", "artifact", "clean", (MojoBinding) it.next() );

        assertMojo( "group", "artifact", "compile", (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.END_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );
        
        assertMojo( mojo.getGroupId(), mojo.getArtifactId(), mojo.getGoal(),
                    (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.CLEAR_FORKED_EXECUTION_GOAL, (MojoBinding) it.next() );

    }

    public void testModifyBindings_AddTwoMojosBetweenTwoExistingCompileMojos()
        throws LifecyclePlannerException
    {
        MojoBinding mojo = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" );
        MojoBinding mojo2 = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile2" );

        List additions = new ArrayList();
        additions.add( newMojo( "group", "artifact", "clean" ) );
        additions.add( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();

        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );

        target.getBuildBinding().getCompile().addBinding( mojo );
        target.getBuildBinding().getCompile().addBinding( mojo2 );

        assertEquals( 2, target.getBuildBinding().getCompile().getBindings().size() );

        target = new ForkPlanModifier( mojo2, additions ).modifyBindings( target );

        assertEquals( 7, target.getBuildBinding().getCompile().getBindings().size() );

        Iterator it = target.getBuildBinding().getCompile().getBindings().iterator();
        
        assertMojo( mojo.getGroupId(), mojo.getArtifactId(), mojo.getGoal(),
                    (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.START_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );

        assertMojo( "group", "artifact", "clean", (MojoBinding) it.next() );

        assertMojo( "group", "artifact", "compile", (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.END_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );
        
        assertMojo( mojo2.getGroupId(), mojo2.getArtifactId(), mojo2.getGoal(),
                    (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.CLEAR_FORKED_EXECUTION_GOAL, (MojoBinding) it.next() );

    }

    public void testModifyBindings_AddTwoNormalPlusTwoModifierModifiedMojosWithTwoExistingCompileMojos()
        throws LifecyclePlannerException
    {
        MojoBinding mojo = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile" );
        MojoBinding mojo2 = newMojo( "org.apache.maven.plugins", "maven-compiler-plugin", "compile2" );

        List modAdditions = new ArrayList();
        modAdditions.add( newMojo( "group2", "artifact", "clean" ) );
        modAdditions.add( newMojo( "group2", "artifact", "compile" ) );

        MojoBinding mojo3 = newMojo( "group", "artifact", "clean" );

        List additions = new ArrayList();
        additions.add( mojo3 );
        additions.add( newMojo( "group", "artifact", "compile" ) );

        LifecycleBindings target = new LifecycleBindings();

        assertEquals( 0, target.getBuildBinding().getCompile().getBindings().size() );

        target.getBuildBinding().getCompile().addBinding( mojo );
        target.getBuildBinding().getCompile().addBinding( mojo2 );

        assertEquals( 2, target.getBuildBinding().getCompile().getBindings().size() );

        BuildPlanModifier modder = new ForkPlanModifier( mojo, additions );
        modder.addModifier( new ForkPlanModifier( mojo3, modAdditions ) );

        target = modder.modifyBindings( target );

        assertEquals( 12, target.getBuildBinding().getCompile().getBindings().size() );

        Iterator it = target.getBuildBinding().getCompile().getBindings().iterator();
        
        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.START_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.START_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );

        assertMojo( "group2", "artifact", "clean", (MojoBinding) it.next() );

        assertMojo( "group2", "artifact", "compile", (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.END_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );
        
        assertMojo( mojo3.getGroupId(), mojo3.getArtifactId(), mojo3.getGoal(),
                    (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.CLEAR_FORKED_EXECUTION_GOAL, (MojoBinding) it.next() );

        assertMojo( "group", "artifact", "compile", (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.END_FORKED_EXECUTION_GOAL,
                    (MojoBinding) it.next() );
        
        assertMojo( mojo.getGroupId(), mojo.getArtifactId(), mojo.getGoal(),
                    (MojoBinding) it.next() );

        assertMojo( StateManagementUtils.GROUP_ID, StateManagementUtils.ARTIFACT_ID,
                    StateManagementUtils.CLEAR_FORKED_EXECUTION_GOAL, (MojoBinding) it.next() );

        assertMojo( mojo2.getGroupId(), mojo2.getArtifactId(), mojo2.getGoal(),
                    (MojoBinding) it.next() );

    }

    private void assertMojo( String groupId, String artifactId, String goal, MojoBinding binding )
    {
        assertEquals( groupId, binding.getGroupId() );
        assertEquals( artifactId, binding.getArtifactId() );
        assertEquals( goal, binding.getGoal() );
    }

    private MojoBinding newMojo( String groupId, String artifactId, String goal )
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setGoal( goal );

        return binding;
    }
}
