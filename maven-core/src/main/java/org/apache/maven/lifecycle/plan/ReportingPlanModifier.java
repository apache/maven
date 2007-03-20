package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Inject the MojoBindings necessary to execute and make available the report instances that another
 * mojo in the build plan needs.
 * 
 * @author jdcasey
 *
 */
public class ReportingPlanModifier
    implements BuildPlanModifier
{
    
    private List planModifiers = new ArrayList();
    private final MojoBinding targetMojoBinding;
    private final List reportBindings;

    public ReportingPlanModifier( MojoBinding mojoBinding, List reportBindings )
    {
        this.targetMojoBinding = mojoBinding;
        this.reportBindings = reportBindings;
    }

    /**
     * Find the mojo that requested reports, and inject the reporting MojoBinding instances just
     * ahead of it in the lifecycle bindings.
     */
    public LifecycleBindings modifyBindings( LifecycleBindings bindings )
        throws LifecyclePlannerException
    {
        Phase phase = LifecycleUtils.findPhaseForMojoBinding( targetMojoBinding, bindings, true );

        String modificationKey = LifecycleUtils.createMojoBindingKey( targetMojoBinding, true );

        if ( phase == null )
        {
            throw new LifecyclePlannerException( "Failed to modify plan. No phase found containing mojoBinding: "
                + modificationKey );
        }

        int insertionIndex = -1;
        List phaseBindings = phase.getBindings();

        for ( int i = 0; i < phaseBindings.size(); i++ )
        {
            MojoBinding candidate = (MojoBinding) phaseBindings.get( i );

            String key = LifecycleUtils.createMojoBindingKey( candidate, true );
            if ( key.equals( modificationKey ) )
            {
                insertionIndex = i;
                break;
            }
        }
        
        phaseBindings.addAll( insertionIndex, reportBindings );
        phase.setBindings( phaseBindings );
        
        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            BuildPlanModifier modifier = (BuildPlanModifier) it.next();
            
            modifier.modifyBindings( bindings );
        }
        
        return bindings;
    }

    /**
     * Add further lifecycle modifications to this report-injecting modifier.
     */
    public void addModifier( BuildPlanModifier planModifier )
    {
        planModifiers.add( planModifier );
    }

    /**
     * Return true if this report-injecting modifier contains further modifications for the lifecycle.
     */
    public boolean hasModifiers()
    {
        return !planModifiers.isEmpty();
    }

}
