package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * Modifier that alters a build plan to substitute a set of MojoBindings in place of a single, 
 * direct-invocation MojoBinding. These bindings are not impacted by {@link BuildPlanModifier}s, 
 * since they don't exist in a {@link LifecycleBindings} instance.
 * 
 * @author jdcasey
 *
 */
public interface DirectInvocationModifier
{
    
    /**
     * The MojoBinding which should be modified.
     */
    MojoBinding getBindingToModify();
    
    /**
     * Return the list of MojoBindings which should replace the modified binding in the master
     * build plan.
     */
    List getModifiedBindings( MavenProject project, LifecycleBindingManager bindingManager );

}
