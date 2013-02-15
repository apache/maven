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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.graph.DependencyFilter;

/**
 * Default project realm cache implementation. Assumes cached data does not change.
 */
@Component( role = ProjectRealmCache.class )
public class DefaultProjectRealmCache
    implements ProjectRealmCache
{

    private static class CacheKey
    {

        private final List<? extends ClassRealm> extensionRealms;

        private final int hashCode;

        public CacheKey( List<? extends ClassRealm> extensionRealms )
        {
            this.extensionRealms = ( extensionRealms != null ) ? extensionRealms : Collections.<ClassRealm> emptyList();

            this.hashCode = this.extensionRealms.hashCode();
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( o == this )
            {
                return true;
            }

            if ( !( o instanceof CacheKey ) )
            {
                return false;
            }

            CacheKey other = (CacheKey) o;

            return extensionRealms.equals( other.extensionRealms );
        }

    }

    private final Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();

    public CacheRecord get( List<? extends ClassRealm> extensionRealms )
    {
        return cache.get( new CacheKey( extensionRealms ) );
    }

    public CacheRecord put( List<? extends ClassRealm> extensionRealms, ClassRealm projectRealm,
                            DependencyFilter extensionArtifactFilter )
    {
        if ( projectRealm == null )
        {
            throw new NullPointerException();
        }

        CacheKey key = new CacheKey( extensionRealms );

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate project realm for extensions " + extensionRealms );
        }

        CacheRecord record = new CacheRecord( projectRealm, extensionArtifactFilter );

        cache.put( key, record );

        return record;
    }

    public void flush()
    {
        cache.clear();
    }

    public void register( MavenProject project, CacheRecord record )
    {
        // default cache does not track record usage
    }

}
