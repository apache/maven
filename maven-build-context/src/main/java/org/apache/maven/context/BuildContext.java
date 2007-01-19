package org.apache.maven.context;

/**
 * Basic data bus for Maven builds, through which the various subsystems can communicate with one
 * another without causing bloat in the APIs.
 * 
 * @author jdcasey
 *
 */
public interface BuildContext
{

    /**
     * Add a new piece of data to the build context.
     */
    void put( Object key, Object value );
    
    /**
     * Retrieve something previously stored in the build context, or null if the key doesn't exist.
     */
    Object get( Object key );
    
    /**
     * Remove a mapped data element from the build context, returning the Object removed, if any.
     */
    Object delete( Object key );
    
    /**
     * Add a new piece of managed build data to the build context. Managed data elements supply their
     * own storage key.
     */
    void put( ManagedBuildData managedData );
    
}
