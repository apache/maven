package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Inject a list of forked-execution bindings at the point where the modification point is bound to
 * the supplied LifecycleBindings, bracketed by special mojo bindings to control the forked-execution
 * context.
 * 
 * @author jdcasey
 *
 */
public class ForkPlanModifier
    implements BuildPlanModifier
{

    private final MojoBinding modificationPoint;
    private List planModifiers = new ArrayList();

    private final List mojoBindings;

    public ForkPlanModifier( MojoBinding modificationPoint, List mojoBindings )
    {
        this.modificationPoint = modificationPoint;
        this.mojoBindings = mojoBindings;
    }

    /**
     * Retrieve the MojoBinding which serves as the injection point for the forked bindings.
     */
    public MojoBinding getModificationPoint()
    {
        return modificationPoint;
    }

    /**
     * Modify the LifeycleBindings from a BuildPlan by locating the modification point MojoBinding,
     * and prepending the forked-execution bindings in the plan, bracketed by mojos that control the
     * forked-execution context.
     */
    public LifecycleBindings modifyBindings( LifecycleBindings bindings )
        throws LifecyclePlannerException
    {
        Phase phase = LifecycleUtils.findPhaseForMojoBinding( getModificationPoint(), bindings, true );

        String modificationKey = LifecycleUtils.createMojoBindingKey( getModificationPoint(), true );

        if ( phase == null )
        {
            throw new LifecyclePlannerException( "Failed to modify plan. No phase found containing mojoBinding: "
                + modificationKey );
        }

        int stopIndex = -1;
        int insertionIndex = -1;
        List phaseBindings = phase.getBindings();

        for ( int i = 0; i < phaseBindings.size(); i++ )
        {
            MojoBinding candidate = (MojoBinding) phaseBindings.get( i );

            String key = LifecycleUtils.createMojoBindingKey( candidate, true );
            if ( key.equals( modificationKey ) )
            {
                insertionIndex = i;
                stopIndex = i + 1;
                break;
            }
        }
        
        phaseBindings.add( stopIndex, StateManagementUtils.createClearForkedExecutionMojoBinding() );
        
        phaseBindings.add( insertionIndex, StateManagementUtils.createEndForkedExecutionMojoBinding() );
        phaseBindings.addAll( insertionIndex, mojoBindings );
        phaseBindings.add( insertionIndex, StateManagementUtils.createStartForkedExecutionMojoBinding() );
        
        phase.setBindings( phaseBindings );
        
        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            BuildPlanModifier modifier = (BuildPlanModifier) it.next();
            
            modifier.modifyBindings( bindings );
        }

        return bindings;
    }

    /**
     * Add a new modifier to further adjust the LifecycleBindings which are modified here.
     */
    public void addModifier( BuildPlanModifier planModifier )
    {
        planModifiers.add( planModifier );
    }

    /**
     * Return true if this modifier itself has modifiers.
     */
    public boolean hasModifiers()
    {
        return !planModifiers.isEmpty();
    }

}
