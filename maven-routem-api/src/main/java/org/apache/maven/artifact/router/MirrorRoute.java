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

import static org.codehaus.plexus.util.StringUtils.join;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// TODO: Needs to have a layout field!
public final class MirrorRoute
    implements Comparable<MirrorRoute>
{

    private final String id;

    private final String routeUrl;

    private final int weight;

    private final boolean enabled;
    
    private final Set<String> mirrorOfUrls;

    // NOTE: ONLY used during deserialization!
    MirrorRoute()
    {
        id = null;
        mirrorOfUrls = Collections.emptySet();
        routeUrl = null;
        weight = 0;
        enabled = false;
    }

    public MirrorRoute( final String id, final String routeUrl,
                        final int weight, final boolean enabled, final String... mirrorOfUrls )
    {
        if ( mirrorOfUrls.length < 1 )
        {
            throw new IllegalArgumentException( "Cannot construct a mirror route without at least one mirror-of URL." );
        }
        
        this.id = id;
        this.mirrorOfUrls = normalizeMirrorOfUrls( Arrays.asList( mirrorOfUrls ) );
        this.routeUrl = routeUrl;
        this.weight = weight;
        this.enabled = enabled;
    }

    public MirrorRoute( final String id, final String routeUrl,
                        final int weight, final boolean enabled, final Collection<String> mirrorOfUrls )
    {
        if ( mirrorOfUrls.size() < 1 )
        {
            throw new IllegalArgumentException( "Cannot construct a mirror route without at least one mirror-of URL." );
        }
        
        this.id = id;
        this.mirrorOfUrls = normalizeMirrorOfUrls( mirrorOfUrls );
        this.routeUrl = routeUrl;
        this.weight = weight;
        this.enabled = enabled;
    }

    private Set<String> normalizeMirrorOfUrls( Collection<String> src )
    {
        Set<String> result = new HashSet<String>( src.size() );
        for ( String srcItem : src )
        {
            String item = srcItem.toLowerCase();
            if ( item.endsWith( "/" ) )
            {
                item = item.substring( 0, item.length() - 1 );
            }
            
            result.add( item );
        }
        
        return result;
    }

    public String getId()
    {
        return id;
    }

    public String getRouteUrl()
    {
        return routeUrl;
    }

    public int getWeight()
    {
        return weight;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public int compareTo( final MirrorRoute o )
    {
        return o.weight - weight;
    }

    @Override
    public String toString()
    {
        return "mirror [id: " + id + ", weight: " + weight + ", mirror-of urls: " + join( mirrorOfUrls.iterator(), ", " ) + ", route-url: " + routeUrl
                        + ", enabled: " + enabled + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( routeUrl == null ) ? 0 : routeUrl.hashCode() );
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
        final MirrorRoute other = (MirrorRoute) obj;
        if ( routeUrl == null )
        {
            if ( other.routeUrl != null )
            {
                return false;
            }
        }
        else if ( !routeUrl.equals( other.routeUrl ) )
        {
            return false;
        }
        return true;
    }

    public Set<String> getMirrorOfUrls()
    {
        return mirrorOfUrls;
    }

    public boolean isMirrorOf( final String canonicalUrl )
    {
        String check = canonicalUrl.toLowerCase();
        if ( check.endsWith( "/" ) )
        {
            check = check.substring( 0, check.length() - 1 );
        }
        
        return mirrorOfUrls.contains( check );
    }

    public String getLayout()
    {
        return "default";
    }

}
