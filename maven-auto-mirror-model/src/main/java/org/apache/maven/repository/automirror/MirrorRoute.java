/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.maven.repository.automirror;

public class MirrorRoute
    implements Comparable<MirrorRoute>
{

    private final String id;

    private final String mirrorUrl;

    private final int weight;

    private final boolean enabled;

    private final String url;

    MirrorRoute()
    {
        id = null;
        url = null;
        mirrorUrl = null;
        weight = 0;
        enabled = false;
    }

    public MirrorRoute( final String id, final String url, final String mirrorUrl, final int weight,
                        final boolean enabled )
    {
        this.id = id;
        this.url = url;
        this.mirrorUrl = mirrorUrl;
        this.weight = weight;
        this.enabled = enabled;
    }

    public String getId()
    {
        return id;
    }

    public String getMirrorUrl()
    {
        return mirrorUrl;
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
        return "mirror [id: " + id + ", weight: " + weight + ", url: " + url + ", mirror-url: " + mirrorUrl
                        + ", enabled: " + enabled + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( mirrorUrl == null ) ? 0 : mirrorUrl.hashCode() );
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
        if ( mirrorUrl == null )
        {
            if ( other.mirrorUrl != null )
            {
                return false;
            }
        }
        else if ( !mirrorUrl.equals( other.mirrorUrl ) )
        {
            return false;
        }
        return true;
    }

    public String getUrl()
    {
        return url;
    }

}
