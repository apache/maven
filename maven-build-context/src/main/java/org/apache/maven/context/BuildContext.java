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
     * 
     * @deprecated Use store(..) instead.
     */
    void put( ManagedBuildData managedData );
    
    /**
     * Add a new piece of managed build data to the build context. Managed data elements supply their
     * own storage key.
     */
    void store( ManagedBuildData managedData );
    
    /**
     * Retrieve the data map for a given type of managed build data, and use this to restore this
     * instance's state to that which was stored in the build context.
     * 
     * @param managedData The managed data instance to restore from the build context.
     * @return true if the data was retrieved from the build context, false otherwise
     */
    boolean retrieve( ManagedBuildData managedData );
    
}
