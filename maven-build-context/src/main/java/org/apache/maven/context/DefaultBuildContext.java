package org.apache.maven.context;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of BuildContext, for use with the DefaultBuildContextManager. This 
 * implementation uses a Map for its backing store, and if constructed with no parameters, will use
 * a LinkedHashMap.
 * 
 * @author jdcasey
 *
 */
public class DefaultBuildContext
    implements BuildContext
{
    
    private final Map contextMap;

    /**
     * Construct a new build context, using the supplied map as the backing store. NOTE: The 
     * supplied map will be copied.
     */
    public DefaultBuildContext( Map contextMap )
    {
        if ( contextMap == null )
        {
            throw new NullPointerException( "DefaultBuildContext requires a non-null contextMap parameter, or no parameter at all." );
        }
        
        this.contextMap = contextMap;
    }
    
    /**
     * Construct a new build context, using a new instance of LinkedHashMap.
     */
    public DefaultBuildContext()
    {
        this.contextMap = new LinkedHashMap();
    }

    /**
     * Remove the object mapped to 'key' from the build context. If there was such a mapping, return
     * the value mapped to the key.
     */
    public Object delete( Object key )
    {
        return contextMap.remove( key );
    }

    /**
     * Retrieve the object mapped to 'key', or null if the mapping doesn't exist. Mapping 'key' to
     * null should also be possible, but will be indistinguishable from a missing mapping.
     */
    public Object get( Object key )
    {
        return contextMap.get( key );
    }

    /**
     * Add a new data mapping to the build context.
     */
    public void put( Object key, Object value )
    {
        contextMap.put( key, value );
    }

    /**
     * Return the Map used to store data elements, for storage by the DefaultBuildContextManager.
     */
    Object getContextMap()
    {
        return contextMap;
    }

    /**
     * Add a new piece of managed data to the build context, using the key supplied by 
     * managedData.getStorageKey().
     * 
     * @deprecated Use store(..) instead
     */
    public void put( ManagedBuildData managedData )
    {
        store( managedData );
    }
    
    /**
     * Add a new piece of managed data to the build context, using the key supplied by 
     * managedData.getStorageKey().
     */
    public void store( ManagedBuildData managedData )
    {
        contextMap.put( managedData.getStorageKey(), managedData.getData() );
    }

    /**
     * Retrieve the data map for a given type of managed build data, and use this to restore this
     * instance's state to that which was stored in the build context.
     * 
     * @param managedData The managed data instance to restore from the build context.
     * @return true if the data was retrieved from the build context, false otherwise
     */
    public boolean retrieve( ManagedBuildData managedData )
    {
        Map dataMap = (Map) contextMap.get( managedData.getStorageKey() );
        
        if ( dataMap != null )
        {
            managedData.setData( dataMap );
            return true;
        }
        
        return false;
    }

}
