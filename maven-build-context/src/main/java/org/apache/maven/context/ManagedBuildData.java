package org.apache.maven.context;

import java.util.Map;

/**
 * Management interface for things that are meant to be stored/retrieved from the Maven BuildContext
 * natively. Such things need to give the BuildContextManager a key for mapping it into the context.
 * 
 * @author jdcasey
 *
 */
public interface ManagedBuildData
{
    
    /**
     * Retrieve the context key under which this instance of managed data should be stored in the
     * BuildContext instance.
     * 
     * @return The BuildContext mapping key.
     */
    String getStorageKey();

    /**
     * Provides a way for a complex object to serialize itself to primitives for storage in the
     * build context, so the same class loaded from different classloaders (as in the case of plugin
     * dependencies) don't incur a ClassCastException when trying to retrieve stored information.
     * 
     * @return The object's data in primitive (shared classes, like Strings) form, keyed for
     *   retrieval.
     */
    Map getData();
    
    /**
     * Restore the object's state from the primitives stored in the build context.
     * @param data The map of primitives that were stored in the build context
     */
    void setData( Map data );

}
