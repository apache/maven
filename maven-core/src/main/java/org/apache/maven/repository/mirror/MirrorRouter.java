/*
 *  Copyright (C) 2011 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.repository.mirror;

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
