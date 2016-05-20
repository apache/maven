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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * @author Igor Fedorenko
 * @author Benjamin Bentmann
 * @author Anton Tanasenko
 */
@Component( role = ProjectArtifactsCache.class )
public class DefaultProjectArtifactsCache
    implements ProjectArtifactsCache
{

    protected static class CacheKey
        implements Key
    {

        private final String groupId;
        
        private final String artifactId;
        
        private final String version;
        
        private final Set<String> dependencyArtifacts;

        private final WorkspaceRepository workspace;

        private final LocalRepository localRepo;

        private final List<RemoteRepository> repositories;
        
        private final Set<String> collect;
        
        private final Set<String> resolve;
        
        private boolean aggregating;

        private final int hashCode;

        public CacheKey( MavenProject project, List<RemoteRepository> repositories,
            Collection<String> scopesToCollect, Collection<String> scopesToResolve, boolean aggregating,
            RepositorySystemSession session )
        {
            
            groupId = project.getGroupId();
            artifactId = project.getArtifactId();
            version = project.getVersion();
            
            Set<String> deps = new HashSet<>();
            if ( project.getDependencyArtifacts() != null )
            {
              for ( Artifact dep: project.getDependencyArtifacts() )
              {
                deps.add( dep.toString() );
              }
            }
            dependencyArtifacts = Collections.unmodifiableSet( deps );
            
            workspace = CacheUtils.getWorkspace( session );
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
            collect = scopesToCollect == null
                ? Collections.<String>emptySet() 
                : Collections.unmodifiableSet( new HashSet<>( scopesToCollect ) );
            resolve = scopesToResolve == null 
                ? Collections.<String>emptySet() 
                : Collections.unmodifiableSet( new HashSet<>( scopesToResolve ) );
            this.aggregating = aggregating;

            int hash = 17;
            hash = hash * 31 + hash( groupId );
            hash = hash * 31 + hash( artifactId );
            hash = hash * 31 + hash( version );
            hash = hash * 31 + hash( dependencyArtifacts );
            hash = hash * 31 + hash( workspace );
            hash = hash * 31 + hash( localRepo );
            hash = hash * 31 + CacheUtils.repositoriesHashCode( repositories );
            hash = hash * 31 + hash( collect );
            hash = hash * 31 + hash( resolve );
            hash = hash * 31 + hash( aggregating );
            this.hashCode = hash;
        }

        @Override
        public String toString()
        {
            return groupId + ":" + artifactId + ":" + version;
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

            return eq( groupId, that.groupId ) && eq( artifactId, that.artifactId ) && eq( version, that.version ) 
                && eq( dependencyArtifacts, that.dependencyArtifacts )
                && eq( workspace, that.workspace ) && eq( localRepo, that.localRepo ) 
                && CacheUtils.repositoriesEquals( repositories, that.repositories ) && eq( collect, that.collect ) 
                && eq( resolve, that.resolve ) && aggregating == that.aggregating;
        }

        private static <T> boolean eq( T s1, T s2 )
        {
            return s1 != null ? s1.equals( s2 ) : s2 == null;
        }

    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<>();

    public Key createKey( MavenProject project, Collection<String> scopesToCollect,
        Collection<String> scopesToResolve, boolean aggregating, RepositorySystemSession session )
    {
        return new CacheKey( project, project.getRemoteProjectRepositories(), scopesToCollect, scopesToResolve, 
            aggregating, session );
    }

    public CacheRecord get( Key key )
        throws LifecycleExecutionException
    {
        CacheRecord cacheRecord = cache.get( key );

        if ( cacheRecord != null && cacheRecord.exception != null )
        {
            throw cacheRecord.exception;
        }

        return cacheRecord;
    }

    public CacheRecord put( Key key, Set<Artifact> projectArtifacts )
    {
        Validate.notNull( projectArtifacts, "projectArtifacts cannot be null" );

        assertUniqueKey( key );

        CacheRecord record =
            new CacheRecord( Collections.unmodifiableSet( new HashSet<>( projectArtifacts ) ) );

        cache.put( key, record );

        return record;
    }

    protected void assertUniqueKey( Key key )
    {
        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate artifact resolution result for project " + key );
        }
    }

    public CacheRecord put( Key key, LifecycleExecutionException exception )
    {
        Validate.notNull( exception, "exception cannot be null" );

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
