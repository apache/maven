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

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the BuildContextManager, which handles instances of DefaultBuildContext.
 * 
 * @plexus.component role="org.apache.maven.context.BuildContextManager" role-hint="default"
 * @author jdcasey
 */
public class DefaultBuildContextManager
    implements BuildContextManager
{
    public static final String ROLE_HINT = "default";
    
    protected static final String BUILD_CONTEXT_MAP_KEY = ROLE + ":" + ROLE_HINT + ":contextMap";
    
    // NOTE: this needs to be static so it can be found by new ctxMgr instances.
    private static InheritableThreadLocal tl = new InheritableThreadLocal();
    
    public DefaultBuildContextManager()
    {
        // used for plexus initialization
    }
    
    /**
     * @deprecated Using ThreadLocal now, not container context, for thread safety.
     */
    public DefaultBuildContextManager( Context context )
    {
        // obsolete, does nothing...
    }
    
    /**
     * Clear the contents of the build context inside the container context.
     */
    public void clearBuildContext()
    {
        tl.set( null );
    }

    /**
     * Retrieve the current BuildContext out of the container context.
     * 
     * @param create Whether to create the BuildContext if it doesn't exist in the container
     */
    public BuildContext readBuildContext( boolean create )
    {
        Map contextMap = getContextContainerMap( create );
        
        if ( contextMap == null && !create )
        {
            return null;
        }
        else
        {
            return new DefaultBuildContext( contextMap );
        }
    }

    /**
     * Store the given BuildContext inside the container.
     */
    public void storeBuildContext( BuildContext context )
    {
        if ( context instanceof DefaultBuildContext )
        {
            tl.set( ((DefaultBuildContext)context).getContextMap() );
        }
        else
        {
            throw new IllegalArgumentException( this.getClass().getName() + " does not know how to store a context of type: " + context.getClass().getName() );
        }
    }

    protected Map getContextContainerMap( boolean create )
    {
        Map containerMap = (Map) tl.get();

        if ( containerMap == null && create )
        {
            containerMap = new HashMap();
            tl.set( containerMap );
        }

        return containerMap;
    }

    protected void clearContextContainerMap()
    {
        Map containerMap = getContextContainerMap( false );

        if ( containerMap != null )
        {
            containerMap.clear();
        }
    }

    /**
     * @deprecated Using ThreadLocal now, not container context, for thread safety.
     */
    public Context reorientToContext( Context context )
    {
        // obsolete, does nothing...
        return context;
    }

}
