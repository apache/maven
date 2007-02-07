package org.apache.maven.context;

import org.codehaus.plexus.context.Context;

/**
 * Manager interface used to store, read, and clear the BuildContext out of the container.
 * 
 * @author jdcasey
 */
public interface BuildContextManager
{
    
    String ROLE = BuildContextManager.class.getName();
    
    /**
     * Read the BuildContext from the container. If it doesn't already exist, optionally create it.
     */
    BuildContext readBuildContext( boolean create );
    
    /**
     * Store the BuildContext in the container context.
     */
    void storeBuildContext( BuildContext context );
    
    /**
     * Clear the contents of the BuildContext, both in the current instance, and in the container
     * context.
     */
    void clearBuildContext();
    
    /**
     * Re-orient this BuildContextManager to use the given Plexus Context instance, returning
     * the original Context instance so it can be restored later. This can be important when the 
     * BuildContextManager is instantiated inside a Maven plugin, but the plugin needs to use the
     * context associated with the core of Maven (in case multiple contexts are used).
     */
    Context reorientToContext( Context context );
    
}
