package org.apache.maven.project;

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

import org.apache.maven.building.Source;
import org.apache.maven.model.building.ModelCache;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple model cache used to accelerate model building during a reactor build.
 *
 * @author Benjamin Bentmann
 */
class ReactorModelCache
    implements ModelCache
{

    private final Map<Object, Object> models = new ConcurrentHashMap<>( 256 );

    @Override
    public Object get( String groupId, String artifactId, String version, String tag )
    {
        return models.get( new GavCacheKey( groupId, artifactId, version, tag ) );
    }

    @Override
    public void put( String groupId, String artifactId, String version, String tag, Object data )
    {
        models.put( new GavCacheKey( groupId, artifactId, version, tag ), data );
    }

    @Override
    public Object get( Source source, String tag )
    {
        return models.get( new SourceCacheKey( source, tag ) );
    }

    @Override
    public void put( Source source, String tag, Object data )
    {
        models.put( new SourceCacheKey( source, tag ), data );
    }

    private static final class GavCacheKey
    {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String tag;

        private final int hashCode;

        GavCacheKey( String groupId, String artifactId, String version, String tag )
        {
            this.groupId = ( groupId != null ) ? groupId : "";
            this.artifactId = ( artifactId != null ) ? artifactId : "";
            this.version = ( version != null ) ? version : "";
            this.tag = ( tag != null ) ? tag : "";
            this.hashCode = Objects.hash( groupId, artifactId, version, tag );
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }

            if ( !( obj instanceof GavCacheKey ) )
            {
                return false;
            }

            GavCacheKey that = (GavCacheKey) obj;

            return artifactId.equals( that.artifactId ) && groupId.equals( that.groupId )
                && version.equals( that.version ) && tag.equals( that.tag );
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }
    }
    
    private static final class SourceCacheKey
    {
        private final Source source;
        
        private final String tag;
        
        private final int hashCode;

        SourceCacheKey( Source source, String tag )
        {
            this.source = source;
            this.tag = tag;
            this.hashCode = Objects.hash( source, tag );
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj ) 
            {
                return true;
            }
            if ( !( obj instanceof SourceCacheKey ) )
            {
                return false;
            }
            
            SourceCacheKey other = (SourceCacheKey) obj;
            if ( !Objects.equals( this.source, other.source ) )
            {
                    return false;
            }
            
            if ( !Objects.equals( this.tag, other.tag ) )
            {
                    return false;
            }

            return true;
        }
    }

}
