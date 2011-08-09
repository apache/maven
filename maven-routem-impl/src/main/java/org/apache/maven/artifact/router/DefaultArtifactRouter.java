/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.artifact.router;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultArtifactRouter implements ArtifactRouter
{

    private final List<MirrorRoute> mirrors;

    private final Map<GroupPattern, GroupRoute> groups;
    
    private final ArtifactRouteSelector routeSelector;

    public DefaultArtifactRouter( ArtifactRouteSelector routeSelector, Collection<MirrorRoute> mirrors, Collection<GroupRoute> groups )
    {
        this.routeSelector = routeSelector;
        List<MirrorRoute> m = new ArrayList<MirrorRoute>();
        if ( mirrors != null )
        {
            for ( MirrorRoute mirror : mirrors )
            {
                if ( !m.contains( mirror ) )
                {
                    m.add( mirror );
                }
            }
        }
        Collections.sort( m );

        this.mirrors = m;

        Map<GroupPattern, GroupRoute> g = new HashMap<GroupPattern, GroupRoute>();
        if ( groups != null )
        {
            for ( GroupRoute route : groups )
            {
                for ( GroupPattern pattern : route.getGroupPatterns() )
                {
                    g.put( pattern, route );
                }
            }
        }
        this.groups = g;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.router.ArtifactRouter#selectSingleMirror(java.lang.String)
     */
    public MirrorRoute selectSingleMirror( final String canonicalUrl )
    {
        List<MirrorRoute> routes = getAllMirrors( canonicalUrl );
        if ( routes == null || routes.isEmpty() )
        {
            return null;
        }
        
        return routeSelector == null ? routes.get( 0 ) : routeSelector.select( routes );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.router.ArtifactRouter#getAllMirrors(java.lang.String)
     */
    public synchronized List<MirrorRoute> getAllMirrors( final String canonicalUrl )
    {
        List<MirrorRoute> routes = new ArrayList<MirrorRoute>();
        for ( MirrorRoute mirror : mirrors )
        {
            if ( mirror.isEnabled() && mirror.isMirrorOf( canonicalUrl ) )
            {
                routes.add( mirror );
            }
        }

        Collections.sort( routes );

        return routes;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.router.ArtifactRouter#getGroup(java.lang.String)
     */
    public GroupRoute getGroup( String groupId )
    {
        List<GroupPattern> matches = new ArrayList<GroupPattern>();
        for ( GroupPattern p : this.groups.keySet() )
        {
            if ( p.implies( groupId ) )
            {
                matches.add( p );
            }
        }

        if ( matches.isEmpty() )
        {
            return GroupRoute.DEFAULT;
        }
        else
        {
            Collections.sort( matches );
            GroupPattern pattern = matches.get( 0 );

            return groups.get( pattern );
        }
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.router.ArtifactRouter#contains(org.apache.maven.artifact.router.MirrorRoute)
     */
    public boolean contains( final MirrorRoute o )
    {
        return mirrors.contains( o );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( mirrors == null ) ? 0 : mirrors.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactRouter other = (ArtifactRouter) obj;
        if ( mirrors == null )
        {
            if ( other.getMirrors() != null )
            {
                return false;
            }
        }
        else if ( !mirrors.equals( other.getMirrors() ) )
        {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.router.ArtifactRouter#getMirrors()
     */
    public List<MirrorRoute> getMirrors()
    {
        return mirrors;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.artifact.router.ArtifactRouter#containsMirrorOf(java.lang.String)
     */
    public boolean containsMirrorOf( final String canonicalUrl )
    {
        for ( final MirrorRoute mirror : mirrors )
        {
            if ( mirror.isEnabled() && mirror.isMirrorOf( canonicalUrl ) )
            {
                return true;
            }
        }

        return false;
    }

}
