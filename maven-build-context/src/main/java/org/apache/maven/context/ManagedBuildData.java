package org.apache.maven.context;

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

}
