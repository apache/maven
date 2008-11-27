package org.apache.maven.artifact.versioning;

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
import java.util.Map;
import java.util.Iterator;

public class ManagedVersionMap
    extends HashMap
{
    public ManagedVersionMap( Map map )
    {
        super();
        if ( map != null )
        {
            putAll( map );
        }
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer( "ManagedVersionMap (" + size() + " entries)\n" );
        Iterator iter = keySet().iterator();
        while ( iter.hasNext() )
        {
            String key = (String) iter.next();
            buffer.append( key ).append( "=" ).append( get( key ) );
            if ( iter.hasNext() )
            {
                buffer.append( "\n" );
            }
        }
        return buffer.toString();
    }
}
