package org.apache.maven.lifecycle.plan;

/**
 * Any element of a build plan that contains or handles a LifecycleBindings instance which is subject
 * to modification.
 *  
 * @author jdcasey
 *
 */
public interface ModifiablePlanElement
{

    /**
     * Add a new lifecycle modifier to this build-plan element.
     */
    void addModifier( BuildPlanModifier planModifier );

    /**
     * Return true if this element has lifecycle modifiers
     */
    boolean hasModifiers();
    
}
