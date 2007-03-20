package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleException;

/**
 * Signals an error during build-plan construction.
 * 
 * @author jdcasey
 *
 */
public class LifecyclePlannerException
    extends LifecycleException
{

    public LifecyclePlannerException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public LifecyclePlannerException( String message )
    {
        super( message );
    }

}
