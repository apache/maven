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
