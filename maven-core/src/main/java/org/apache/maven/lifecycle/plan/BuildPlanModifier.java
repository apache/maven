package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.model.LifecycleBindings;

/**
 * Modifies an existing set of lifecycle mojo bindings, in order to inject extra behavior, such as
 * forked executions, reporting, etc.
 */
public interface BuildPlanModifier extends ModifiablePlanElement
{

    /**
     * Inject any modifications into the given LifecycleBindings provided by the build plan. In some
     * cases, it may be necessary to regenerate the LifecycleBindings instance, so the altered instance
     * is returned separately.
     */
    LifecycleBindings modifyBindings( LifecycleBindings bindings )
        throws LifecyclePlannerException;
    
}
