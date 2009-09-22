package org.apache.maven.plugin;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Default PluginCache implementation. Assumes cached data does not change.
 */
@Component( role = PluginRealmCache.class )
public class DefaultPluginRealmCache
    implements PluginRealmCache
{

    protected static class CacheKey
    {
        private final Plugin plugin;

        private final List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();

        private final ClassLoader parentRealm;

        private final List<String> parentImports;

        private final int hashCode;

        public CacheKey( Plugin plugin, ClassLoader parentRealm, List<String> parentImports, ArtifactRepository localRepository,
                         List<ArtifactRepository> remoteRepositories )
        {
            this.plugin = plugin.clone();
            this.repositories.add( localRepository );
            this.repositories.addAll( remoteRepositories );
            this.parentRealm = parentRealm;
            this.parentImports = ( parentImports != null ) ? parentImports : Collections.<String> emptyList();

            int hash = 17;
            hash = hash * 31 + pluginHashCode( plugin );
            hash = hash * 31 + repositories.hashCode();
            hash = hash * 31 + ( parentRealm != null ? parentRealm.hashCode() : 0 );
            hash = hash * 31 + this.parentImports.hashCode();
            this.hashCode = hash;
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

            return parentRealm == other.parentRealm && pluginEquals( plugin, other.plugin )
                && eq( repositories, other.repositories );
        }
    }

    protected final Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();

    public CacheRecord get( Plugin plugin, ClassLoader parentRealm, List<String> parentImports,
                            ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
    {
        return cache.get( new CacheKey( plugin, parentRealm, parentImports, localRepository, remoteRepositories ) );
    }

    public void put( Plugin plugin, ClassLoader parentRealm, List<String> parentImports,
                     ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                     ClassRealm pluginRealm, List<Artifact> pluginArtifacts )
    {
        if ( pluginRealm == null || pluginArtifacts == null )
        {
            throw new NullPointerException();
        }

        CacheKey key = new CacheKey( plugin, parentRealm, parentImports, localRepository, remoteRepositories );

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate plugin realm for plugin " + plugin.getId() );
        }

        CacheRecord record = new CacheRecord( pluginRealm, pluginArtifacts );
        cache.put( key, record );
    }

    public void flush()
    {
        cache.clear();
    }

    protected static int pluginHashCode( Plugin plugin )
    {
        int hash = 17;

        hash = hash * 31 + plugin.getGroupId().hashCode();
        hash = hash * 31 + plugin.getArtifactId().hashCode();
        hash = hash * 31 + plugin.getVersion().hashCode();

        hash = hash * 31 + ( plugin.isExtensions() ? 1 : 0 );

        for ( Dependency dependency : plugin.getDependencies() )
        {
            hash = hash * 31 + dependency.getGroupId().hashCode();
            hash = hash * 31 + dependency.getArtifactId().hashCode();
            hash = hash * 31 + dependency.getVersion().hashCode();
            hash = hash * 31 + dependency.getType().hashCode();
            hash = hash * 31 + ( dependency.getClassifier() != null ? dependency.getClassifier().hashCode() : 0 );
            hash = hash * 31 + ( dependency.getScope() != null ? dependency.getScope().hashCode() : 0 );

            for ( Exclusion exclusion : dependency.getExclusions() )
            {
                hash = hash * 31 + exclusion.getGroupId().hashCode();
                hash = hash * 31 + exclusion.getArtifactId().hashCode();
            }
        }

        return hash;
    }

    protected static boolean pluginEquals( Plugin a, Plugin b )
    {
        return eq( a.getGroupId(), b.getGroupId() ) //
            && eq( a.getArtifactId(), b.getArtifactId() ) //
            && eq( a.getVersion(), b.getVersion() ) // 
            && a.isExtensions() == b.isExtensions() //
            && dependenciesEquals( a.getDependencies(), b.getDependencies() );
    }

    private static boolean dependenciesEquals( List<Dependency> a, List<Dependency> b )
    {
        if ( a.size() != b.size() )
        {
            return false;
        }

        Iterator<Dependency> aI = a.iterator();
        Iterator<Dependency> bI = b.iterator();

        while ( aI.hasNext() )
        {
            Dependency aD = aI.next();
            Dependency bD = bI.next();

            boolean r = eq( aD.getGroupId(), bD.getGroupId() ) //
                && eq( aD.getArtifactId(), bD.getArtifactId() ) //
                && eq( aD.getVersion(), bD.getVersion() ) // 
                && eq( aD.getType(), bD.getType() ) //
                && eq( aD.getClassifier(), bD.getClassifier() ) //
                && eq( aD.getScope(), bD.getScope() );

            r &= exclusionsEquals( aD.getExclusions(), bD.getExclusions() );

            if ( !r )
            {
                return false;
            }
        }

        return true;
    }

    private static boolean exclusionsEquals( List<Exclusion> a, List<Exclusion> b )
    {
        if ( a.size() != b.size() )
        {
            return false;
        }

        Iterator<Exclusion> aI = a.iterator();
        Iterator<Exclusion> bI = b.iterator();

        while ( aI.hasNext() )
        {
            Exclusion aD = aI.next();
            Exclusion bD = bI.next();

            boolean r = eq( aD.getGroupId(), bD.getGroupId() ) //
                && eq( aD.getArtifactId(), bD.getArtifactId() );

            if ( !r )
            {
                return false;
            }
        }

        return true;
    }

    private static <T> boolean eq( T s1, T s2 )
    {
        return s1 != null ? s1.equals( s2 ) : s2 == null;
    }

}
