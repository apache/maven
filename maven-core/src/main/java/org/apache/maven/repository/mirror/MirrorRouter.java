package org.apache.maven.repository.mirror;

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

import org.apache.maven.repository.automirror.MirrorRoute;
import org.apache.maven.repository.automirror.MirrorRoutingTable;

import java.util.Collections;
import java.util.Map;

public class MirrorRouter
{
    
    private MirrorRoutingTable routingTable;
    
    private Map<String, MirrorRoute> selectedRoutes;
    
    public MirrorRouter( MirrorRoutingTable routingTable, Map<String, MirrorRoute> selectedRoutes )
    {
        this.routingTable = routingTable;
        this.selectedRoutes = selectedRoutes;
    }

    public MirrorRouter()
    {
        routingTable = new MirrorRoutingTable();
        selectedRoutes = Collections.emptyMap();
    }

    public synchronized MirrorRoute getMirror( String canonicalUrl )
    {
        MirrorRoute route = selectedRoutes.get( canonicalUrl );
        if ( route == null )
        {
            route = routingTable.getMirror( canonicalUrl );
            if ( route != null )
            {
                selectedRoutes.put( canonicalUrl, route );
            }
        }
        
        return route;
    }
    
    public Map<String, MirrorRoute> getSelectedRoutes()
    {
        return selectedRoutes;
    }

}
