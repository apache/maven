package org.apache.maven.lifecycle.mapping;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Lifecycle mapping for a POM.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultLifecycleMapping
    implements LifecycleMapping
{
    private List lifecycles;

    private Map lifecycleMap;

    /** @deprecated use lifecycles instead */
    private Map phases;
    
    public List getOptionalMojos( String lifecycle )
    {
        if ( lifecycleMap == null )
        {
            lifecycleMap = new HashMap();

            if ( lifecycles != null )
            {
                for ( Iterator i = lifecycles.iterator(); i.hasNext(); )
                {
                    Lifecycle l = (Lifecycle) i.next();
                    lifecycleMap.put( l.getId(), l );
                }
            }
        }
        Lifecycle l = (Lifecycle) lifecycleMap.get( lifecycle );

        if ( l != null )
        {
            return l.getOptionalMojos();
        }
        else
        {
            return null;
        }
    }

    public Map getPhases( String lifecycle )
    {
        if ( lifecycleMap == null )
        {
            lifecycleMap = new HashMap();

            if ( lifecycles != null )
            {
                for ( Iterator i = lifecycles.iterator(); i.hasNext(); )
                {
                    Lifecycle l = (Lifecycle) i.next();
                    lifecycleMap.put( l.getId(), l );
                }
            }
        }
        Lifecycle l = (Lifecycle) lifecycleMap.get( lifecycle );

        Map mappings = null;
        if ( l == null )
        {
            if ( "default".equals( lifecycle ) )
            {
                mappings = phases;
            }
        }
        else
        {
            mappings = l.getPhases();
        }

        return mappings;
    }

}
