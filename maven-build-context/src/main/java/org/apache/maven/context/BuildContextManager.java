package org.apache.maven.context;

/**
 * Manager interface used to store, read, and clear the BuildContext out of the container.
 * 
 * @author jdcasey
 */
public interface BuildContextManager
{
    
    String ROLE = BuildContextManager.class.getName();
    
    /**
     * Create a new instance of BuildContext
     */
    BuildContext newUnstoredInstance();
    
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
    
}
