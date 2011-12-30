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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * Default PluginCache implementation. Assumes cached data does not change.
 */
@Component( role = PluginRealmCache.class )
public class DefaultPluginRealmCache
    implements PluginRealmCache
{

    protected static class CacheKey
        implements Key
    {

        private final Plugin plugin;

        private final WorkspaceRepository workspace;

        private final LocalRepository localRepo;

        private final List<RemoteRepository> repositories;

        private final ClassLoader parentRealm;

        private final Map<String, ClassLoader> foreignImports;

        private final DependencyFilter filter;

        private final int hashCode;

        public CacheKey( Plugin plugin, ClassLoader parentRealm, Map<String, ClassLoader> foreignImports,
                         DependencyFilter dependencyFilter, List<RemoteRepository> repositories,
                         RepositorySystemSession session )
        {
            this.plugin = plugin.clone();
            this.workspace = CacheUtils.getWorkspace( session );
            this.localRepo = session.getLocalRepository();
            this.repositories = new ArrayList<RemoteRepository>( repositories.size() );
            for ( RemoteRepository repository : repositories )
            {
                if ( repository.isRepositoryManager() )
                {
                    this.repositories.addAll( repository.getMirroredRepositories() );
                }
                else
                {
                    this.repositories.add( repository );
                }
            }
            this.parentRealm = parentRealm;
            this.foreignImports =
                ( foreignImports != null ) ? foreignImports : Collections.<String, ClassLoader> emptyMap();
            this.filter = dependencyFilter;

            int hash = 17;
            hash = hash * 31 + CacheUtils.pluginHashCode( plugin );
            hash = hash * 31 + hash( workspace );
            hash = hash * 31 + hash( localRepo );
            hash = hash * 31 + CacheUtils.repositoriesHashCode( repositories );
            hash = hash * 31 + hash( parentRealm );
            hash = hash * 31 + this.foreignImports.hashCode();
            hash = hash * 31 + hash( dependencyFilter );
            this.hashCode = hash;
        }

        @Override
        public String toString()
        {
            return plugin.getId();
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        private static int hash( Object obj )
        {
            return obj != null ? obj.hashCode() : 0;
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

            CacheKey that = (CacheKey) o;

            return parentRealm == that.parentRealm && CacheUtils.pluginEquals( plugin, that.plugin )
                && eq( workspace, that.workspace ) && eq( localRepo, that.localRepo )
                && CacheUtils.repositoriesEquals( this.repositories, that.repositories ) && eq( filter, that.filter )
                && eq( foreignImports, that.foreignImports );
        }

        private static <T> boolean eq( T s1, T s2 )
        {
            return s1 != null ? s1.equals( s2 ) : s2 == null;
        }

    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<Key, CacheRecord>();

    public Key createKey( Plugin plugin, ClassLoader parentRealm, Map<String, ClassLoader> foreignImports,
                          DependencyFilter dependencyFilter, List<RemoteRepository> repositories,
                          RepositorySystemSession session )
    {
        return new CacheKey( plugin, parentRealm, foreignImports, dependencyFilter, repositories, session );
    }

    public CacheRecord get( Key key )
    {
        return cache.get( key );
    }

    public CacheRecord put( Key key, ClassRealm pluginRealm, List<Artifact> pluginArtifacts )
    {
        if ( pluginRealm == null || pluginArtifacts == null )
        {
            throw new IllegalArgumentException();
        }

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate plugin realm for plugin " + key );
        }

        CacheRecord record = new CacheRecord( pluginRealm, pluginArtifacts );

        cache.put( key, record );

        return record;
    }

    public void flush()
    {
        cache.clear();
    }

    protected static int pluginHashCode( Plugin plugin )
    {
        return CacheUtils.pluginHashCode( plugin );
    }

    protected static boolean pluginEquals( Plugin a, Plugin b )
    {
        return CacheUtils.pluginEquals( a, b );
    }

    public void register( MavenProject project, CacheRecord record )
    {
        // default cache does not track plugin usage
    }

}
