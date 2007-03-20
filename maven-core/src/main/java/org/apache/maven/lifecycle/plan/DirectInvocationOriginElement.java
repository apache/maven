package org.apache.maven.lifecycle.plan;

/**
 * Instantiates MojoBindings for direct invocation, which may be subject to modification.
 * 
 * @author jdcasey
 *
 */
public interface DirectInvocationOriginElement
{
    
    /**
     * Add a new direct-invocation binding modifier.
     */
    void addDirectInvocationModifier( DirectInvocationModifier modifier );

}
