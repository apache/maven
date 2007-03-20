package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construct a list of MojoBinding instances that accomplish all of the tasks specified. For lifecycle
 * phases, construct a list of mojos to execute up to and including the specified phase, and add them
 * to the list. For direct invocations, construct a new MojoBinding instance and add it to the list.
 * 
 * All of these bindings are subject to lifecycle modifications due to forking, reporting, or other
 * factors, and also to forked-execution modification of direct invocations.
 * 
 * @author jdcasey
 *
 */
public class LifecycleBuildPlan
    implements BuildPlan, DirectInvocationOriginElement
{

    private final List tasks;

    private final LifecycleBindings lifecycleBindings;

    private List planModifiers = new ArrayList();

    private Map directInvocationModifiers = new HashMap();

    public LifecycleBuildPlan( List tasks, LifecycleBindings lifecycleBindings )
    {
        this.tasks = tasks;
        this.lifecycleBindings = lifecycleBindings;
    }

    /**
     * Build the master execution list necessary to accomplish the specified tasks, given the
     * specified set of mojo bindings to different parts of the lifecycle.
     */
    public List getPlanMojoBindings( MavenProject project, LifecycleBindingManager bindingManager )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        LifecycleBindings cloned = BuildPlanUtils.modifyPlanBindings( lifecycleBindings, planModifiers );

        return bindingManager.assembleMojoBindingList( tasks, cloned, directInvocationModifiers, project );
    }

    /**
     * Retrieve the set of tasks that this build plan is responsible for.
     */
    public List getTasks()
    {
        return tasks;
    }

    /**
     * Add a new build-plan modifier to inject reporting, forked-execution, or other altered behavior
     * into the "vanilla" lifecycle that was specified at instance construction.
     */
    public void addModifier( BuildPlanModifier planModifier )
    {
        planModifiers.add( planModifier );
    }

    /**
     * Return true if build-plan modifiers exist (these are lifecycle-only modifiers, not direct
     * invocation modifiers).
     */
    public boolean hasModifiers()
    {
        return !planModifiers.isEmpty();
    }

    /**
     * Add a new modifier for a direct-invocation MojoBinding in the build plan resulting from this
     * instance.
     */
    public void addDirectInvocationModifier( DirectInvocationModifier modifier )
    {
        directInvocationModifiers.put( LifecycleUtils.createMojoBindingKey( modifier.getBindingToModify(), true ), modifier );
    }

}
