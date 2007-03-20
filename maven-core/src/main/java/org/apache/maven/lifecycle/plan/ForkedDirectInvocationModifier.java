package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Inject a list of MojoBindings in place of the forking binding, bracketing the forked bindings with
 * special mojos to control the forked-execution context.
 * 
 * @author jdcasey
 *
 */
public class ForkedDirectInvocationModifier
    implements DirectInvocationModifier
{

    private final List forkedBindings;
    private final MojoBinding forkingBinding;

    public ForkedDirectInvocationModifier( MojoBinding forkingBinding, List forkedBindings )
    {
        this.forkingBinding = forkingBinding;
        this.forkedBindings = forkedBindings;
    }

    /**
     * Return a list containing forked-execution context control MojoBindings, the forked-execution
     * bindings themselves, and finally the binding that forked off a new execution branch.
     */
    public List getModifiedBindings( MavenProject project, LifecycleBindingManager bindingManager )
    {
        List result = new ArrayList();

        result.add( StateManagementUtils.createStartForkedExecutionMojoBinding() );
        result.addAll( forkedBindings );
        result.add( StateManagementUtils.createEndForkedExecutionMojoBinding() );
        result.add( forkingBinding );
        result.add( StateManagementUtils.createClearForkedExecutionMojoBinding() );

        return result;
    }

    /**
     * Return the MojoBinding that forks execution to include the bindings in this modifier.
     */
    public MojoBinding getBindingToModify()
    {
        return forkingBinding;
    }

}
