package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.project.MavenProject;

import java.util.Collections;
import java.util.List;

/**
 * Constructs a build plan using a LifecycleBindings instance, but only allowing one phase for that
 * lifecycle, as opposed to the parent class which allows a list of tasks (some of which may not be
 * lifecycle phases at all).
 * 
 * This build plan cannot produce direct-invocation MojoBindings.
 * 
 * @author jdcasey
 *
 */
public class SubLifecycleBuildPlan
    implements BuildPlan
{
    
    private LifecycleBuildPlan delegate;
    private final String phase;
    
    public SubLifecycleBuildPlan( String phase, LifecycleBindings bindings )
    {
        this.phase = phase;
        delegate = new LifecycleBuildPlan( Collections.singletonList( phase ), bindings );
    }

    /**
     * Retrieve the build plan binding list from the delegate {@link LifecycleBuildPlan} instance,
     * and return them.
     */
    public List getPlanMojoBindings(MavenProject project, LifecycleBindingManager bindingManager)
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        return delegate.getPlanMojoBindings(project, bindingManager);
    }

    /**
     * Return a list containing a single item: the lifecycle phase that this plan is concerned with
     * accomplishing.
     */
    public List getTasks()
    {
        return Collections.singletonList( phase );
    }

    /**
     * Add a build-plan modifier to the delegate {@link LifecycleBuildPlan} instance.
     */
    public void addModifier( BuildPlanModifier planModifier )
    {
        delegate.addModifier( planModifier );
    }

    /**
     * Return true if the delegate {@link LifecycleBuildPlan} instance contains build-plan modifiers.
     */
    public boolean hasModifiers()
    {
        return delegate.hasModifiers();
    }

}
