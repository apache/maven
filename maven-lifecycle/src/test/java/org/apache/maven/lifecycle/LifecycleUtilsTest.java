package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.CleanBinding;
import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class LifecycleUtilsTest
    extends TestCase
{

    public void testSetOrigin_ShouldSetMojoBindingOrigin()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setOrigin( "original" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getClean().addBinding( binding );

        LifecycleUtils.setOrigin( bindings, "changed" );

        assertEquals( "changed", binding.getOrigin() );
    }

    public void testCreateMojoBindingKey_NoExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        String key = LifecycleUtils.createMojoBindingKey( binding, false );

        assertEquals( "group:artifact:goal", key );
    }

    public void testCreateMojoBindingKey_WithExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "execution" );

        String key = LifecycleUtils.createMojoBindingKey( binding, true );

        assertEquals( "group:artifact:goal:execution", key );
    }

    public void testFindLifecycleBindingForPhase_ShouldFindMojoBindingInPhase()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getClean().addBinding( binding );

        LifecycleBinding result = LifecycleUtils.findLifecycleBindingForPhase( "clean", bindings );

        assertTrue( result instanceof CleanBinding );

        CleanBinding cb = (CleanBinding) result;
        Phase clean = cb.getClean();

        assertNotNull( clean );
        assertEquals( 1, clean.getBindings().size() );

        MojoBinding resultBinding = (MojoBinding) clean.getBindings().get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testFindLifecycleBindingForPhase_ShouldReturnNullForInvalidPhase()
    {
        LifecycleBindings bindings = new LifecycleBindings();

        LifecycleBinding result = LifecycleUtils.findLifecycleBindingForPhase( "dud", bindings );

        assertNull( result );
    }

    public void testFindMatchingMojoBinding_ShouldFindMatchWithoutExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding2 = LifecycleUtils.findMatchingMojoBinding( binding, bindings, false );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
    }

    public void testFindMatchingMojoBinding_ShouldFindMatchWithExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "non-default" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );

        assertNotNull( binding3 );
        assertEquals( "goal", binding3.getGoal() );
        assertEquals( "group", binding3.getGroupId() );
        assertEquals( "artifact", binding3.getArtifactId() );
        assertEquals( "non-default", binding3.getExecutionId() );
    }

    public void testFindMatchingMojoBinding_ShouldReturnNullNoMatchWithoutExecId()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        MojoBinding binding2 = newMojoBinding( "group2", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );

        assertNull( binding3 );
    }

    public void testFindMatchingMojoBinding_ShouldReturnNullWhenExecIdsDontMatch()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        // default executionId == 'default'

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "execution" );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding bb = new BuildBinding();

        Phase phase = new Phase();
        phase.addBinding( binding );

        bb.setCompile( phase );

        bindings.setBuildBinding( bb );

        MojoBinding binding3 = LifecycleUtils.findMatchingMojoBinding( binding2, bindings, true );

        assertNull( binding3 );
    }

    public void testRemoveMojoBinding_ThrowErrorWhenPhaseNotInLifecycleBinding()
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        String phase = "phase";

        try
        {
            LifecycleUtils.removeMojoBinding( phase, binding, cleanBinding, false );

            fail( "Should fail when phase doesn't exist in lifecycle binding." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testRemoveMojoBinding_DoNothingWhenMojoBindingNotInLifecycleBinding()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        Phase phase = new Phase();
        phase.addBinding( binding2 );

        cleanBinding.setClean( phase );

        String phaseName = "clean";

        LifecycleUtils.removeMojoBinding( phaseName, binding, cleanBinding, false );

        Phase result = cleanBinding.getClean();
        assertEquals( 1, result.getBindings().size() );
    }

    public void testRemoveMojoBinding_DoNothingWhenMojoPhaseIsNullInLifecycleBinding()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        Phase phase = new Phase();
        phase.addBinding( binding2 );

        cleanBinding.setPreClean( phase );

        String phaseName = "clean";

        LifecycleUtils.removeMojoBinding( phaseName, binding, cleanBinding, false );

        Phase result = cleanBinding.getPreClean();
        assertEquals( 1, result.getBindings().size() );
    }

    public void testRemoveMojoBinding_RemoveMojoBindingWhenFoundInLifecycleBinding()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        Phase phase = new Phase();
        phase.addBinding( binding );

        cleanBinding.setClean( phase );

        String phaseName = "clean";

        LifecycleUtils.removeMojoBinding( phaseName, binding, cleanBinding, false );

        Phase result = cleanBinding.getClean();
        assertEquals( 0, result.getBindings().size() );
    }

    public void testMergeBindings_SingleMojoCloneFromEachIsPresentInResult_EmptyDefaults()
    {
        LifecycleBindings bOrig = new LifecycleBindings();
        CleanBinding cbOrig = bOrig.getCleanBinding();
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( binding );

        LifecycleBindings bOrig2 = new LifecycleBindings();
        CleanBinding cbOrig2 = bOrig2.getCleanBinding();
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        cbOrig2.getClean().addBinding( binding2 );

        LifecycleBindings result = LifecycleUtils.mergeBindings( bOrig, bOrig2, new LifecycleBindings(), false, false );

        assertNotNull( result );
        
        CleanBinding cbResult = result.getCleanBinding();
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 2, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        
        assertNotSame( binding, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
        
        bResult = (MojoBinding) mojos.get( 1 );
        
        assertNotSame( binding2, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal2", bResult.getGoal() );
    }

    public void testMergeBindings_SingleMojoCloneIsPresentFromExistingAndNotFromDefaultsInResult()
    {
        LifecycleBindings bOrig = new LifecycleBindings();
        CleanBinding cbOrig = bOrig.getCleanBinding();
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( binding );

        LifecycleBindings bOrig2 = new LifecycleBindings();
        CleanBinding cbOrig2 = bOrig2.getCleanBinding();
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        cbOrig2.getClean().addBinding( binding2 );

        LifecycleBindings result = LifecycleUtils.mergeBindings( bOrig, new LifecycleBindings(), bOrig2, false, false );

        assertNotNull( result );
        
        CleanBinding cbResult = result.getCleanBinding();
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        
        assertNotSame( binding, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
    }

    public void testMergeBindings_MojoCloneIsPresentFromDefaultsInResult()
    {
        LifecycleBindings bOrig = new LifecycleBindings();
        CleanBinding cbOrig = bOrig.getCleanBinding();
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( binding );

        LifecycleBindings bOrig2 = new LifecycleBindings();
        BuildBinding bbOrig = bOrig2.getBuildBinding();
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        bbOrig.getCompile().addBinding( binding2 );

        LifecycleBindings result = LifecycleUtils.mergeBindings( bOrig, new LifecycleBindings(), bOrig2, false, false );

        assertNotNull( result );
        
        CleanBinding cbResult = result.getCleanBinding();
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        
        assertNotSame( binding, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
        
        BuildBinding bbResult = result.getBuildBinding();
        assertNotSame( bbOrig, bbResult );

        mojos = bbResult.getCompile().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        bResult = (MojoBinding) mojos.get( 0 );
        
        assertNotSame( binding, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal2", bResult.getGoal() );
    }

    public void testMergeBindings_MergeConfigsWithNewAsDominant_EmptyDefaults()
    {
        LifecycleBindings bOrig = new LifecycleBindings();
        CleanBinding cbOrig = bOrig.getCleanBinding();
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setOrigin( "non-default" );
        
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom child = new Xpp3Dom( "child" );
        child.setValue( "value" );
        config.addChild( child );
        
        binding.setConfiguration( config );
        
        cbOrig.getClean().addBinding( binding );

        LifecycleBindings bOrig2 = new LifecycleBindings();
        CleanBinding cbOrig2 = bOrig2.getCleanBinding();
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        
        Xpp3Dom config2 = new Xpp3Dom( "configuration" );
        
        Xpp3Dom child2 = new Xpp3Dom( "child" );
        child2.setValue( "value2" );
        
        Xpp3Dom child3 = new Xpp3Dom( "key" );
        child3.setValue( "val" );
        
        config2.addChild( child2 );
        config2.addChild( child3 );
        
        binding2.setConfiguration( config2 );
        
        cbOrig2.getClean().addBinding( binding2 );

        LifecycleBindings result = LifecycleUtils.mergeBindings( bOrig, bOrig2, new LifecycleBindings(), true, false );

        assertNotNull( result );
        
        CleanBinding cbResult = result.getCleanBinding();
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        
        assertNotSame( binding, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
        assertEquals( "non-default", bResult.getOrigin() );
        
        Xpp3Dom cResult = (Xpp3Dom) bResult.getConfiguration();
        
        assertNotNull( cResult );
        assertEquals( "value2", cResult.getChild( "child" ).getValue() );
        assertEquals( "val", cResult.getChild( "key" ).getValue() );
    }

    public void testMergeBindings_MergeConfigsWithExistingAsDominant_EmptyDefaults()
    {
        LifecycleBindings bOrig = new LifecycleBindings();
        CleanBinding cbOrig = bOrig.getCleanBinding();
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setOrigin( "non-default" );
        
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom child = new Xpp3Dom( "child" );
        child.setValue( "value" );
        config.addChild( child );
        
        binding.setConfiguration( config );
        
        cbOrig.getClean().addBinding( binding );

        LifecycleBindings bOrig2 = new LifecycleBindings();
        CleanBinding cbOrig2 = bOrig2.getCleanBinding();
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        
        Xpp3Dom config2 = new Xpp3Dom( "configuration" );
        
        Xpp3Dom child2 = new Xpp3Dom( "child" );
        child2.setValue( "value2" );
        
        Xpp3Dom child3 = new Xpp3Dom( "key" );
        child3.setValue( "val" );
        
        config2.addChild( child2 );
        config2.addChild( child3 );
        
        binding2.setConfiguration( config2 );
        
        cbOrig2.getClean().addBinding( binding2 );

        LifecycleBindings result = LifecycleUtils.mergeBindings( bOrig, bOrig2, new LifecycleBindings(), true, true );

        assertNotNull( result );
        
        CleanBinding cbResult = result.getCleanBinding();
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        
        assertNotSame( binding, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
        assertEquals( "non-default", bResult.getOrigin() );
        
        Xpp3Dom cResult = (Xpp3Dom) bResult.getConfiguration();
        
        assertNotNull( cResult );
        assertEquals( "value", cResult.getChild( "child" ).getValue() );
        assertEquals( "val", cResult.getChild( "key" ).getValue() );
    }

    public void testCloneBinding_SingleMojoCloneIsPresentInNewInstance()
    {
        CleanBinding cbOrig = new CleanBinding();
        MojoBinding bOrig = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( bOrig );

        LifecycleBinding result = LifecycleUtils.cloneBinding( cbOrig );

        assertNotNull( result );
        assertTrue( result instanceof CleanBinding );
        assertNotSame( cbOrig, result );

        List mojos = ( (CleanBinding) result ).getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        assertNotSame( bOrig, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
    }

    public void testCloneBinding_OrderIsPreservedBetweenTwoMojoBindingsInNewInstance()
    {
        CleanBinding cbOrig = new CleanBinding();
        MojoBinding bOrig = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( bOrig );

        MojoBinding bOrig2 = newMojoBinding( "group", "artifact", "goal2" );
        cbOrig.getClean().addBinding( bOrig2 );

        LifecycleBinding result = LifecycleUtils.cloneBinding( cbOrig );

        assertNotNull( result );
        assertTrue( result instanceof CleanBinding );
        assertNotSame( cbOrig, result );

        List mojos = ( (CleanBinding) result ).getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 2, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        assertNotSame( bOrig, bResult );

        assertEquals( bOrig.getGroupId(), bResult.getGroupId() );
        assertEquals( bOrig.getArtifactId(), bResult.getArtifactId() );
        assertEquals( bOrig.getGoal(), bResult.getGoal() );

        MojoBinding bResult2 = (MojoBinding) mojos.get( 1 );
        assertNotSame( bOrig2, bResult2 );

        assertEquals( bOrig2.getGroupId(), bResult2.getGroupId() );
        assertEquals( bOrig2.getArtifactId(), bResult2.getArtifactId() );
        assertEquals( bOrig2.getGoal(), bResult2.getGoal() );
    }

    public void testCloneBindings_SingleMojoCloneIsPresentInNewInstance()
    {
        LifecycleBindings bindings = new LifecycleBindings();

        CleanBinding cbOrig = bindings.getCleanBinding();
        MojoBinding bOrig = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( bOrig );

        LifecycleBindings result = LifecycleUtils.cloneBindings( bindings );

        assertNotNull( result );
        assertNotSame( bindings, result );

        CleanBinding cbResult = result.getCleanBinding();

        assertNotNull( cbResult );
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        assertNotSame( bOrig, bResult );

        assertEquals( "group", bResult.getGroupId() );
        assertEquals( "artifact", bResult.getArtifactId() );
        assertEquals( "goal", bResult.getGoal() );
    }

    public void testCloneBindings_OrderIsPreservedBetweenTwoMojoBindingsInNewInstance()
    {
        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cbOrig = bindings.getCleanBinding();
        MojoBinding bOrig = newMojoBinding( "group", "artifact", "goal" );
        cbOrig.getClean().addBinding( bOrig );

        MojoBinding bOrig2 = newMojoBinding( "group", "artifact", "goal2" );
        cbOrig.getClean().addBinding( bOrig2 );

        LifecycleBindings result = LifecycleUtils.cloneBindings( bindings );

        assertNotNull( result );
        assertNotSame( bindings, result );

        CleanBinding cbResult = result.getCleanBinding();

        assertNotNull( cbResult );
        assertNotSame( cbOrig, cbResult );

        List mojos = cbResult.getClean().getBindings();
        assertNotNull( mojos );
        assertEquals( 2, mojos.size() );

        MojoBinding bResult = (MojoBinding) mojos.get( 0 );
        assertNotSame( bOrig, bResult );

        assertEquals( bOrig.getGroupId(), bResult.getGroupId() );
        assertEquals( bOrig.getArtifactId(), bResult.getArtifactId() );
        assertEquals( bOrig.getGoal(), bResult.getGoal() );

        MojoBinding bResult2 = (MojoBinding) mojos.get( 1 );
        assertNotSame( bOrig2, bResult2 );

        assertEquals( bOrig2.getGroupId(), bResult2.getGroupId() );
        assertEquals( bOrig2.getArtifactId(), bResult2.getArtifactId() );
        assertEquals( bOrig2.getGoal(), bResult2.getGoal() );
    }

    public void testCloneMojoBinding_NullVersionIsPropagated()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        MojoBinding binding2 = LifecycleUtils.cloneMojoBinding( binding );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
        assertNull( binding.getVersion() );
        assertNull( binding2.getVersion() );
    }

    public void testCloneMojoBinding_ExecutionIdIsPropagated()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = LifecycleUtils.cloneMojoBinding( binding );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
        assertEquals( "non-default", binding2.getExecutionId() );
    }

    public void testCloneMojoBinding_VersionIsPropagated()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setVersion( "version" );

        MojoBinding binding2 = LifecycleUtils.cloneMojoBinding( binding );

        assertNotNull( binding2 );
        assertEquals( "goal", binding2.getGoal() );
        assertEquals( "group", binding2.getGroupId() );
        assertEquals( "artifact", binding2.getArtifactId() );
        assertEquals( "version", binding2.getVersion() );
        assertEquals( "default", binding2.getExecutionId() );
    }

    public void testAddMojoBinding_LifecycleBinding_AddOneMojoBindingToEmptyLifecycle()
        throws NoSuchPhaseException
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleUtils.addMojoBinding( "clean", binding, cleanBinding );

        Phase clean = cleanBinding.getClean();
        assertEquals( 1, clean.getBindings().size() );
    }

    public void testAddMojoBinding_LifecycleBinding_ThrowErrorWhenPhaseDoesntExist()
    {
        CleanBinding cleanBinding = new CleanBinding();

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        try
        {
            LifecycleUtils.addMojoBinding( "compile", binding, cleanBinding );

            fail( "Should fail because compile phase isn't in the clean lifecycle." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testAddMojoBinding_LifecycleBindings_AddOneMojoBindingToEmptyLifecycle()
        throws LifecycleSpecificationException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();

        LifecycleUtils.addMojoBinding( "clean", binding, bindings );

        CleanBinding cleanBinding = bindings.getCleanBinding();
        assertNotNull( cleanBinding );

        Phase clean = cleanBinding.getClean();
        assertNotNull( clean );
        assertEquals( 1, clean.getBindings().size() );
    }

    public void testAddMojoBinding_LifecycleBindings_ThrowErrorWhenPhaseDoesntExist()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        try
        {
            LifecycleUtils.addMojoBinding( "dud", binding, new LifecycleBindings() );

            fail( "Should fail because dud phase isn't in the any lifecycle." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected
        }
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_FailWithInvalidStopPhase()
    {
        try
        {
            LifecycleUtils.getMojoBindingListForLifecycle( "dud", new CleanBinding() );

            fail( "Should fail when asked for an invalid phase." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_RetrieveMojoBindingInStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_RetrieveMojoBindingInPreviousPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getPreClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_RetrieveTwoMojoBindings()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBinding_DontRetrieveMojoBindingsInPhaseAfterStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        MojoBinding binding3 = newMojoBinding( "group", "artifact", "goal3" );

        CleanBinding cleanBinding = new CleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );
        cleanBinding.getPostClean().addBinding( binding3 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", cleanBinding );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_RetrieveMojoBindingInStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_FailWithInvalidStopPhase()
    {
        try
        {
            LifecycleUtils.getMojoBindingListForLifecycle( "dud", new LifecycleBindings() );

            fail( "Should fail when asked for an invalid phase." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_RetrieveMojoBindingInPreviousPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        bindings.getCleanBinding().getPreClean().addBinding( binding );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        MojoBinding resultBinding = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_RetrieveTwoMojoBindings()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cleanBinding = bindings.getCleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testGetMojoBindingListForLifecycle_LifecycleBindings_DontRetrieveMojoBindingsInPhaseAfterStopPhase()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal2" );
        MojoBinding binding3 = newMojoBinding( "group", "artifact", "goal3" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cleanBinding = bindings.getCleanBinding();
        cleanBinding.getClean().addBinding( binding );
        cleanBinding.getPreClean().addBinding( binding2 );
        cleanBinding.getPostClean().addBinding( binding3 );

        List result = LifecycleUtils.getMojoBindingListForLifecycle( "clean", bindings );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        MojoBinding resultBinding2 = (MojoBinding) result.get( 0 );

        assertEquals( "group", resultBinding2.getGroupId() );
        assertEquals( "artifact", resultBinding2.getArtifactId() );
        assertEquals( "goal2", resultBinding2.getGoal() );

        MojoBinding resultBinding = (MojoBinding) result.get( 1 );

        assertEquals( "group", resultBinding.getGroupId() );
        assertEquals( "artifact", resultBinding.getArtifactId() );
        assertEquals( "goal", resultBinding.getGoal() );
    }

    public void testIsMojoBindingPresent_ReturnFalseWhenMojoBindingIsMissing_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        assertFalse( LifecycleUtils.isMojoBindingPresent( binding, new ArrayList(), false ) );
    }

    public void testIsMojoBindingPresent_ReturnFalseWhenMojoBindingIsMissing_WithExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        assertFalse( LifecycleUtils.isMojoBindingPresent( binding, new ArrayList(), true ) );
    }

    public void testIsMojoBindingPresent_ReturnTrueWhenMojoBindingExecIdDoesntMatch_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );

        List mojos = new ArrayList();
        mojos.add( binding );

        assertTrue( LifecycleUtils.isMojoBindingPresent( binding2, mojos, false ) );
    }

    public void testIsMojoBindingPresent_ReturnFalseWhenMojoBindingExecIdDoesntMatch_WithExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );

        List mojos = new ArrayList();
        mojos.add( binding );

        assertFalse( LifecycleUtils.isMojoBindingPresent( binding2, mojos, true ) );
    }

    public void testFindPhaseForMojoBinding_ReturnNullIfBindingNotFound_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding, bindings, false );

        assertNull( phase );
    }

    public void testFindPhaseForMojoBinding_ReturnPhaseContainingBinding_WithoutExecIdCompare()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase cleanPhase = bindings.getCleanBinding().getClean();
        cleanPhase.addBinding( binding );

        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding, bindings, false );

        assertNotNull( phase );
        assertSame( cleanPhase, phase );
    }

    public void testFindPhaseForMojoBinding_ReturnPhaseContainingSimilarBindingWithOtherExecId_WithoutExecIdCompare()
    {
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "non-default" );

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase cleanPhase = bindings.getCleanBinding().getClean();
        cleanPhase.addBinding( binding );

        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding2, bindings, false );

        assertNotNull( phase );
        assertSame( cleanPhase, phase );
    }

    public void testFindPhaseForMojoBinding_ReturnNullWhenBindingExecIdsDontMatch_WithExecIdCompare()
    {
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "goal" );
        binding2.setExecutionId( "non-default" );

        MojoBinding binding = newMojoBinding( "group", "artifact", "goal" );

        LifecycleBindings bindings = new LifecycleBindings();
        Phase cleanPhase = bindings.getCleanBinding().getClean();
        cleanPhase.addBinding( binding );

        Phase phase = LifecycleUtils.findPhaseForMojoBinding( binding2, bindings, true );

        assertNull( phase );
    }

    public void testRemoveMojoBinding_ReturnLifecycleWithoutMojo_WithoutExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBinding( "clean", binding2, cb, false );

        assertEquals( 0, cb.getClean().getBindings().size() );
    }

    public void testRemoveMojoBinding_ReturnLifecycleWithoutMojo_WithExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBinding( "clean", binding2, cb, true );

        assertEquals( 0, cb.getClean().getBindings().size() );
    }

    public void testRemoveMojoBinding_DontRemoveMojoIfExecIdDoesntMatch_WithExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBinding( "clean", binding2, cb, true );

        assertEquals( 1, cb.getClean().getBindings().size() );
    }

    public void testRemoveMojoBinding_FailOnInvalidPhaseName()
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        CleanBinding cb = new CleanBinding();

        try
        {
            LifecycleUtils.removeMojoBinding( "dud", binding, cb, false );

            fail( "Should fail because phase does not exist in the clean lifecycle." );
        }
        catch ( NoSuchPhaseException e )
        {
            // expected
        }
    }

    public void testRemoveMojoBindings_LifecycleBinding_RemoveOneMojo_WithoutExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding2 ), cb, false );

        assertEquals( 0, cb.getClean().getBindings().size() );

    }

    public void testRemoveMojoBindings_LifecycleBinding_RemoveOneMojo_WithExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding2 ), cb, true );

        assertEquals( 0, cb.getClean().getBindings().size() );

    }

    public void testRemoveMojoBindings_LifecycleBinding_RemoveTwoMojos_WithoutExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean2" );
        MojoBinding binding3 = newMojoBinding( "group", "artifact", "clean" );
        MojoBinding binding4 = newMojoBinding( "group", "artifact", "clean4" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding2 );
        cb.getClean().addBinding( binding3 );
        cb.getClean().addBinding( binding4 );

        assertEquals( 3, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding ), cb, false );

        List cleanBindings = cb.getClean().getBindings();

        assertEquals( 2, cleanBindings.size() );

        assertEquals( binding2.getGoal(), ( (MojoBinding) cleanBindings.get( 0 ) ).getGoal() );
        assertEquals( binding4.getGoal(), ( (MojoBinding) cleanBindings.get( 1 ) ).getGoal() );
    }

    public void testRemoveMojoBindings_LifecycleBinding_DontRemoveIfNoExecIdMatch_WithExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        CleanBinding cb = new CleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding2 ), cb, true );

        assertEquals( 1, cb.getClean().getBindings().size() );

    }

    public void testRemoveMojoBindings_LifecycleBindings_RemoveOneMojo_WithoutExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cb = bindings.getCleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding2 ), bindings, false );

        assertEquals( 0, cb.getClean().getBindings().size() );

    }

    public void testRemoveMojoBindings_LifecycleBindings_RemoveOneMojo_WithExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );
        binding.setExecutionId( "non-default" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cb = bindings.getCleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding2 ), bindings, true );

        assertEquals( 0, cb.getClean().getBindings().size() );

    }

    public void testRemoveMojoBindings_LifecycleBindings_RemoveTwoMojos_WithoutExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );
        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean2" );
        MojoBinding binding3 = newMojoBinding( "group", "artifact", "clean" );
        MojoBinding binding4 = newMojoBinding( "group", "artifact", "clean4" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cb = bindings.getCleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding2 );
        cb.getClean().addBinding( binding3 );
        cb.getClean().addBinding( binding4 );

        assertEquals( 3, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding ), bindings, false );

        List cleanBindings = cb.getClean().getBindings();

        assertEquals( 2, cleanBindings.size() );

        assertEquals( binding2.getGoal(), ( (MojoBinding) cleanBindings.get( 0 ) ).getGoal() );
        assertEquals( binding4.getGoal(), ( (MojoBinding) cleanBindings.get( 1 ) ).getGoal() );
    }

    public void testRemoveMojoBindings_LifecycleBindings_DontRemoveIfNoExecIdMatch_WithExecIdCompare()
        throws NoSuchPhaseException
    {
        MojoBinding binding = newMojoBinding( "group", "artifact", "clean" );

        MojoBinding binding2 = newMojoBinding( "group", "artifact", "clean" );
        binding2.setExecutionId( "non-default" );

        LifecycleBindings bindings = new LifecycleBindings();
        CleanBinding cb = bindings.getCleanBinding();

        assertEquals( 0, cb.getClean().getBindings().size() );

        cb.getClean().addBinding( binding );

        assertEquals( 1, cb.getClean().getBindings().size() );

        LifecycleUtils.removeMojoBindings( Collections.singletonList( binding2 ), bindings, true );

        assertEquals( 1, cb.getClean().getBindings().size() );

    }
    
    public void testIsValidPhaseName_ReturnTrueForPhaseInCleanLifecycle()
    {
        assertTrue( LifecycleUtils.isValidPhaseName( "clean" ) );
    }

    public void testIsValidPhaseName_ReturnTrueForPhaseInBuildLifecycle()
    {
        assertTrue( LifecycleUtils.isValidPhaseName( "compile" ) );
    }

    public void testIsValidPhaseName_ReturnTrueForPhaseInSiteLifecycle()
    {
        assertTrue( LifecycleUtils.isValidPhaseName( "site" ) );
    }

    public void testIsValidPhaseName_ReturnFalseForInvalidPhaseName()
    {
        assertFalse( LifecycleUtils.isValidPhaseName( "dud" ) );
    }

    private MojoBinding newMojoBinding( String groupId, String artifactId, String goal )
    {
        MojoBinding binding = new MojoBinding();
        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setGoal( goal );

        return binding;
    }

}
