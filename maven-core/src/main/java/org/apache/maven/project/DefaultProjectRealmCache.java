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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.aether.graph.DependencyFilter;

/**
 * Default project realm cache implementation. Assumes cached data does not change.
 */
@Named
@Singleton
public class DefaultProjectRealmCache
    implements ProjectRealmCache, Disposable
{
    /**
     * CacheKey
     */
    protected static class CacheKey
        implements Key
    {

        private final List<? extends ClassRealm> extensionRealms;

        private final int hashCode;

        public CacheKey( List<? extends ClassRealm> extensionRealms )
        {
            this.extensionRealms = ( extensionRealms != null )
                                       ? Collections.unmodifiableList( extensionRealms )
                                       : Collections.<ClassRealm>emptyList();

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

        @Override
        public String toString()
        {
            return extensionRealms.toString();
        }
    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<>();

    @Override
    public Key createKey( List<? extends ClassRealm> extensionRealms )
    {
        return new CacheKey( extensionRealms );
    }

    public CacheRecord get( Key key )
    {
        return cache.get( key );
    }

    public CacheRecord put( Key key, ClassRealm projectRealm, DependencyFilter extensionArtifactFilter )
    {
        Objects.requireNonNull( projectRealm, "projectRealm cannot be null" );

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate project realm for extensions " + key );
        }

        CacheRecord record = new CacheRecord( projectRealm, extensionArtifactFilter );

        cache.put( key, record );

        return record;
    }

    public void flush()
    {
        for ( CacheRecord record : cache.values() )
        {
            ClassRealm realm = record.getRealm();
            try
            {
                realm.getWorld().disposeRealm( realm.getId() );
            }
            catch ( NoSuchRealmException e )
            {
                // ignore
            }
        }
        cache.clear();
    }

    public void register( MavenProject project, Key key, CacheRecord record )
    {
        // default cache does not track record usage
    }

    @Override
    public void dispose()
    {
        flush();
    }

}
