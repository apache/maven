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
     * @deprecated Use {@link #store(ManagedBuildData)} instead.
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
