package org.apache.maven.project.artifact;

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


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = MavenMetadataCache.class )
public class DefaultMavenMetadataCache
    implements MavenMetadataCache
{

    public static class CacheKey 
    {
        private final Artifact artifact;
        private final List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
        private final int hashCode;

        public CacheKey( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        {
            this.artifact = ArtifactUtils.copyArtifact( artifact );
            this.repositories.add( localRepository );
            this.repositories.addAll( remoteRepositories );

            int hash = 17;
            hash = hash * 31 + artifactHashCode( artifact );
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
            
            if ( !(o instanceof CacheKey) )
            {
                return false;
            }
            
            CacheKey other = (CacheKey) o;
            
            return artifactEquals( artifact, other.artifact ) && repositories.equals( other.repositories );
        }
    }

    private static int artifactHashCode( Artifact a )
    {
        int result = 17;
        result = 31 * result + a.getGroupId().hashCode();
        result = 31 * result + a.getArtifactId().hashCode();
        result = 31 * result + a.getType().hashCode();
        if ( a.getVersion() != null )
        {
            result = 31 * result + a.getVersion().hashCode();
        }
        result = 31 * result + ( a.getClassifier() != null ? a.getClassifier().hashCode() : 0 );
        result = 31 * result + ( a.getScope() != null ? a.getScope().hashCode() : 0 );
        result = 31 * result + ( a.getDependencyFilter() != null? a.getDependencyFilter().hashCode() : 0 );
        result = 31 * result + ( a.isOptional() ? 1 : 0 );
        return result;
    }

    private static boolean artifactEquals( Artifact a1, Artifact a2 )
    {
        if ( a1 == a2 )
        {
            return true;
        }
        
        return eq( a1.getGroupId(), a2.getGroupId() )
            && eq( a1.getArtifactId(), a2.getArtifactId() )
            && eq( a1.getType(), a2.getType() )
            && eq( a1.getVersion(), a2.getVersion() )
            && eq( a1.getClassifier(), a2.getClassifier() )
            && eq( a1.getScope(), a2.getScope() )
            && eq( a1.getDependencyFilter(), a2.getDependencyFilter() )
            && a1.isOptional() == a2.isOptional();
    }

    private static <T> boolean eq( T s1, T s2 )
    {
        return s1 != null? s1.equals( s2 ): s2 == null;
    }

    public class CacheRecord
    {
        private Artifact pomArtifact;
        private List<Artifact> artifacts;
        private List<ArtifactRepository> remoteRepositories;

        private long length;
        private long timestamp;

        CacheRecord(Artifact pomArtifact, Set<Artifact> artifacts, List<ArtifactRepository> remoteRepositories)
        {
            this.pomArtifact = ArtifactUtils.copyArtifact( pomArtifact );
            this.artifacts = ArtifactUtils.copyArtifacts( artifacts, new ArrayList<Artifact>() );
            this.remoteRepositories = new ArrayList<ArtifactRepository>( remoteRepositories );


            File pomFile = pomArtifact.getFile();
            if ( pomFile != null && pomFile.canRead() )
            {
                this.length = pomFile.length();
                this.timestamp = pomFile.lastModified();
            }
            else
            {
                this.length = -1;
                this.timestamp = -1;
            }
        }
        
        public Artifact getArtifact()
        {
            return pomArtifact;
        }

        public List<Artifact> getArtifacts()
        {
            return artifacts;
        }

        public List<ArtifactRepository> getRemoteRepositories()
        {
            return remoteRepositories;
        }

        public boolean isStale()
        {
            File pomFile = pomArtifact.getFile();
            if ( pomFile != null && pomFile.canRead() )
            {
                return length != pomFile.length() || timestamp != pomFile.lastModified();
            }

            return length != -1 || timestamp != -1;
        }
    }

    protected Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();

    public ResolutionGroup get( Artifact artifact, ArtifactRepository localRepository,
                                List<ArtifactRepository> remoteRepositories )
    {
        CacheKey cacheKey = new CacheKey( artifact, localRepository, remoteRepositories );

        CacheRecord cacheRecord = cache.get( cacheKey );

        if ( cacheRecord != null && !cacheRecord.isStale() )
        {
            Artifact pomArtifact = ArtifactUtils.copyArtifact( cacheRecord.getArtifact() );
            Set<Artifact> artifacts = ArtifactUtils.copyArtifacts( cacheRecord.getArtifacts(), new LinkedHashSet<Artifact>() );
            return new ResolutionGroup( pomArtifact, artifacts , cacheRecord.getRemoteRepositories() );
        }

        cache.remove( cacheKey );

        return null;
    }

    public void put( Artifact artifact, ArtifactRepository localRepository,
                     List<ArtifactRepository> remoteRepositories, ResolutionGroup result )
    {
        CacheKey cacheKey = new CacheKey( artifact, localRepository, remoteRepositories );
        CacheRecord cacheRecord = new CacheRecord( result.getPomArtifact(), result.getArtifacts(), result.getResolutionRepositories() );

        cache.put( cacheKey, cacheRecord );
    }

    public void flush()
    {
        cache.clear();
    }
}
