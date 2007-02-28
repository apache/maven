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
