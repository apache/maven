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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * @author Igor Fedorenko
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultPluginArtifactsCache
    implements PluginArtifactsCache
{
    /**
     * CacheKey
     */
    protected static class CacheKey
        implements Key
    {
        private final Plugin plugin;

        private final WorkspaceRepository workspace;

        private final LocalRepository localRepo;

        private final List<RemoteRepository> repositories;

        private final DependencyFilter filter;

        private final int hashCode;

        public CacheKey( Plugin plugin, DependencyFilter extensionFilter, List<RemoteRepository> repositories,
                         RepositorySystemSession session )
        {
            this.plugin = plugin.clone();
            workspace = RepositoryUtils.getWorkspace( session );
            this.localRepo = session.getLocalRepository();
            this.repositories = new ArrayList<>( repositories.size() );
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
            this.filter = extensionFilter;

            int hash = 17;
            hash = hash * 31 + CacheUtils.pluginHashCode( plugin );
            hash = hash * 31 + Objects.hashCode( workspace );
            hash = hash * 31 + Objects.hashCode( localRepo );
            hash = hash * 31 + RepositoryUtils.repositoriesHashCode( repositories );
            hash = hash * 31 + Objects.hashCode( extensionFilter );
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

            return CacheUtils.pluginEquals( plugin, that.plugin )
                && Objects.equals( workspace, that.workspace )
                && Objects.equals( localRepo, that.localRepo )
                && RepositoryUtils.repositoriesEquals( repositories, that.repositories )
                && Objects.equals( filter, that.filter );
        }
    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<>();

    public Key createKey( Plugin plugin, DependencyFilter extensionFilter, List<RemoteRepository> repositories,
                          RepositorySystemSession session )
    {
        return new CacheKey( plugin, extensionFilter, repositories, session );
    }

    public CacheRecord get( Key key )
        throws PluginResolutionException
    {
        CacheRecord cacheRecord = cache.get( key );

        if ( cacheRecord != null && cacheRecord.getException() != null )
        {
            throw cacheRecord.getException();
        }

        return cacheRecord;
    }

    public CacheRecord put( Key key, List<Artifact> pluginArtifacts )
    {
        Objects.requireNonNull( pluginArtifacts, "pluginArtifacts cannot be null" );

        assertUniqueKey( key );

        CacheRecord record =
            new CacheRecord( Collections.unmodifiableList( new ArrayList<>( pluginArtifacts ) ) );

        cache.put( key, record );

        return record;
    }

    protected void assertUniqueKey( Key key )
    {
        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate artifact resolution result for plugin " + key );
        }
    }

    public CacheRecord put( Key key, PluginResolutionException exception )
    {
        Objects.requireNonNull( exception, "exception cannot be null" );

        assertUniqueKey( key );

        CacheRecord record = new CacheRecord( exception );

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

    public void register( MavenProject project, Key cacheKey, CacheRecord record )
    {
        // default cache does not track record usage
    }

}
