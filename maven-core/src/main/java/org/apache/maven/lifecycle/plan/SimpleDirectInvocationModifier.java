package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.MojoBinding;
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
public class SimpleDirectInvocationModifier
    implements DirectInvocationModifier
{

    private final List reportBindings;
    private final MojoBinding targetBinding;

    public SimpleDirectInvocationModifier( MojoBinding targetBinding, List reportBindings )
    {
        this.targetBinding = targetBinding;
        this.reportBindings = reportBindings;
    }

    /**
     * Return a list containing forked-execution context control MojoBindings, the forked-execution
     * bindings themselves, and finally the binding that forked off a new execution branch.
     */
    public List getModifiedBindings( MavenProject project, LifecycleBindingManager bindingManager )
    {
        List result = new ArrayList();

        result.addAll( reportBindings );
        result.add( targetBinding );

        return result;
    }

    /**
     * Return the MojoBinding that forks execution to include the bindings in this modifier.
     */
    public MojoBinding getBindingToModify()
    {
        return targetBinding;
    }

}
