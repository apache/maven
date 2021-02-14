package org.apache.maven.repository.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.building.Source;
import org.apache.maven.model.building.ModelCache;
import org.eclipse.aether.RepositorySystemSession;

/**
 * A model builder cache backed by the repository system cache.
 *
 * @author Benjamin Bentmann
 */
public class DefaultModelCache
    implements ModelCache
{

    private static final String KEY = DefaultModelCache.class.getName();

    private final Map<Object, Object> cache;

    public static ModelCache newInstance( RepositorySystemSession session )
    {
        Map<Object, Object> cache;
        if ( session.getCache() == null )
        {
            cache = new ConcurrentHashMap<>();
        }
        else
        {
            cache = ( Map ) session.getCache().get( session, KEY );
            if ( cache == null )
            {
                cache = new ConcurrentHashMap<>();
                session.getCache().put( session, KEY, cache );
            }
        }
        return new DefaultModelCache( cache );
    }

    private DefaultModelCache( Map<Object, Object> cache )
    {
        this.cache = cache;
    }

    public Object get( Source path, String tag )
    {
        return get( new SourceCacheKey( path, tag ) );
    }

    public void put( Source path, String tag, Object data )
    {
        put( new SourceCacheKey( path, tag ), data );
    }

    public Object get( String groupId, String artifactId, String version, String tag )
    {
        return get( new GavCacheKey( groupId, artifactId, version, tag ) );
    }

    public void put( String groupId, String artifactId, String version, String tag, Object data )
    {
        put( new GavCacheKey( groupId, artifactId, version, tag ), data );
    }

    protected Object get( Object key )
    {
        return cache.get( key );
    }

    protected void put( Object key, Object data )
    {
        cache.put( key, data );
    }

    static class GavCacheKey
    {

        private final String gav;

        private final String tag;

        private final int hash;

        GavCacheKey( String groupId, String artifactId, String version, String tag )
        {
            this( gav( groupId, artifactId, version ), tag );
        }

        GavCacheKey( String gav, String tag )
        {
            this.gav = gav;
            this.tag = tag;
            this.hash = Objects.hash( gav, tag );
        }

        private static String gav( String groupId, String artifactId, String version )
        {
            StringBuilder sb = new StringBuilder();
            if ( groupId != null )
            {
                sb.append( groupId );
            }
            sb.append( ":" );
            if ( artifactId != null )
            {
                sb.append( artifactId );
            }
            sb.append( ":" );
            if ( version != null )
            {
                sb.append( version );
            }
            return sb.toString();
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( null == obj || !getClass().equals( obj.getClass() ) )
            {
                return false;
            }
            GavCacheKey that = (GavCacheKey) obj;
            return Objects.equals( this.gav, that.gav ) && Objects.equals( this.tag, that.tag );
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public String toString()
        {
            return "GavCacheKey{"
                    + "gav='" + gav + '\''
                    + ", tag='" + tag + '\''
                    + '}';
        }
    }

    private static final class SourceCacheKey
    {
        private final Source source;

        private final String tag;

        private final int hash;

        SourceCacheKey( Source source, String tag )
        {
            this.source = source;
            this.tag = tag;
            this.hash = Objects.hash( source, tag );
        }

        @Override
        public String toString()
        {
            return "SourceCacheKey{"
                    + "source=" + source
                    + ", tag='" + tag + '\''
                    + '}';
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( null == obj || !getClass().equals( obj.getClass() ) )
            {
                return false;
            }
            SourceCacheKey that = (SourceCacheKey) obj;
            return Objects.equals( this.source, that.source ) && Objects.equals( this.tag, that.tag );
        }

        @Override
        public int hashCode()
        {
            return hash;
        }
    }
}
