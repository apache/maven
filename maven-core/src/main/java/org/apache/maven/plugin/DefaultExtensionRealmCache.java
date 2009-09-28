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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Default extension realm cache implementation. Assumes cached data does not change.
 */
@Component( role = ExtensionRealmCache.class )
public class DefaultExtensionRealmCache
    implements ExtensionRealmCache
{

    private static class CacheKey
    {

        private final Plugin extension;

        private final List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();

        private final int hashCode;

        public CacheKey( Plugin extension, RepositoryRequest repositoryRequest )
        {
            this.extension = extension.clone();
            this.repositories.add( repositoryRequest.getLocalRepository() );
            this.repositories.addAll( repositoryRequest.getRemoteRepositories() );

            int hash = 17;
            hash = hash * 31 + extensionHashCode( extension );
            hash = hash * 31 + repositories.hashCode();
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

            return extensionEquals( extension, other.extension ) && eq( repositories, other.repositories );
        }

    }

    private final Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();

    public CacheRecord get( Plugin extension, RepositoryRequest repositoryRequest )
    {
        return cache.get( new CacheKey( extension, repositoryRequest ) );
    }

    public void put( Plugin extension, RepositoryRequest repositoryRequest, ClassRealm extensionRealm,
                     List<Artifact> extensionArtifacts, ExtensionDescriptor extensionDescriptor )
    {
        if ( extensionRealm == null || extensionArtifacts == null )
        {
            throw new NullPointerException();
        }

        CacheKey key = new CacheKey( extension, repositoryRequest );

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate extension realm for extension " + extension.getId() );
        }

        CacheRecord record = new CacheRecord( extensionRealm, extensionArtifacts, extensionDescriptor );
        cache.put( key, record );
    }

    public void flush()
    {
        cache.clear();
    }

    protected static int extensionHashCode( Plugin extension )
    {
        int hash = 17;

        hash = hash * 31 + extension.getGroupId().hashCode();
        hash = hash * 31 + extension.getArtifactId().hashCode();
        hash = hash * 31 + extension.getVersion().hashCode();

        for ( Dependency dependency : extension.getDependencies() )
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

    private static boolean extensionEquals( Plugin a, Plugin b )
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

    public void register( MavenProject project, ClassRealm extensionRealm )
    {
        // default cache does not track extension usage
    }

}
