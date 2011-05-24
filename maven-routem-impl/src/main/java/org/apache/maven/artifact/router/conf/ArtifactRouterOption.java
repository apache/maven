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

package org.apache.maven.artifact.router.conf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum ArtifactRouterOption
{

    update( "Update routes" ), 
    offline( "Offline. Use only stored routes." ), 
    clear( "Clear stored routes." ), 
    unset( "Unset defaults." );

    private String description;

    private ArtifactRouterOption( String description )
    {
        this.description = description;
    }

    public static String list( String separator )
    {
        StringBuilder sb = new StringBuilder();
        for ( ArtifactRouterOption opt : values() )
        {
            sb.append( separator ).append( opt.name().toLowerCase() ).append( " - " ).append( opt.description );
        }

        return sb.toString();
    }

    public static Set<ArtifactRouterOption> parse( String... values )
    {
        Set<ArtifactRouterOption> options = new HashSet<ArtifactRouterOption>();

        Map<String, ArtifactRouterOption> allOptions = new HashMap<String, ArtifactRouterOption>();
        for ( ArtifactRouterOption value : values() )
        {
            allOptions.put( value.name(), value );
        }

        all: for ( String value : values )
        {
            String[] parts = value.split( "[|,]" );
            for ( String part : parts )
            {
                ArtifactRouterOption opt = allOptions.remove( part.toLowerCase().trim() );
                if ( opt != null )
                {
                    options.add( opt );
                }

                if ( allOptions.isEmpty() )
                {
                    break all;
                }
            }
        }

        return options;
    }

}
