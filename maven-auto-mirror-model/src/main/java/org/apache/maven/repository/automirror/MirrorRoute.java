package org.apache.maven.repository.automirror;

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

public class MirrorRoute
    implements Comparable<MirrorRoute>
{

    private final String id;

    private final String myUrl;

    private final int weight;

    private final boolean enabled;

    private final Set<String> mirrorOfUrls;

    // NOTE: ONLY used during deserialization!
    MirrorRoute()
    {
        id = null;
        mirrorOfUrls = Collections.emptySet();
        myUrl = null;
        weight = 0;
        enabled = false;
    }

    public MirrorRoute( final String id, final String myUrl, final int weight,
                        final boolean enabled, final String... mirrorOfUrls )
    {
        if ( mirrorOfUrls.length < 1 )
        {
            throw new IllegalArgumentException( "Cannot construct a mirror route without at least one mirror-of URL." );
        }
        
        this.id = id;
        this.mirrorOfUrls = toLowerCaseSet( Arrays.asList( mirrorOfUrls ) );
        this.myUrl = myUrl;
        this.weight = weight;
        this.enabled = enabled;
    }

    public MirrorRoute( final String id, final String myUrl, final int weight,
                        final boolean enabled, final Collection<String> mirrorOfUrls )
    {
        if ( mirrorOfUrls.size() < 1 )
        {
            throw new IllegalArgumentException( "Cannot construct a mirror route without at least one mirror-of URL." );
        }
        
        this.id = id;
        this.mirrorOfUrls = toLowerCaseSet( mirrorOfUrls );
        this.myUrl = myUrl;
        this.weight = weight;
        this.enabled = enabled;
    }

    private Set<String> toLowerCaseSet( Collection<String> src )
    {
        Set<String> result = new HashSet<String>( src.size() );
        for ( String srcItem : src )
        {
            result.add( srcItem.toLowerCase() );
        }
        
        return result;
    }

    public String getId()
    {
        return id;
    }

    public String getRouteUrl()
    {
        return myUrl;
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
        return "mirror [id: " + id + ", weight: " + weight + ", mirror-of urls: " + join( mirrorOfUrls.iterator(), ", " ) + ", mirror-url: " + myUrl
                        + ", enabled: " + enabled + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( myUrl == null ) ? 0 : myUrl.hashCode() );
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
        if ( myUrl == null )
        {
            if ( other.myUrl != null )
            {
                return false;
            }
        }
        else if ( !myUrl.equals( other.myUrl ) )
        {
            return false;
        }
        return true;
    }

    public Set<String> getMirrorOfUrls()
    {
        return mirrorOfUrls;
    }

    public boolean isMirrorOf( String canonicalUrl )
    {
        return mirrorOfUrls.contains( canonicalUrl.toLowerCase() );
    }

}
