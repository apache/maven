package org.apache.maven.project.artifact;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

/**
 * DefaultMavenMetadataCache
 */
@Named
@Singleton
public class DefaultMavenMetadataCache
    implements MavenMetadataCache
{

    protected final Map<CacheKey, CacheRecord> cache = new ConcurrentHashMap<>();

    /**
     * CacheKey
     */
    public static class CacheKey
    {
        private final Artifact artifact;
        private final long pomHash;
        private final boolean resolveManagedVersions;
        private final List<ArtifactRepository> repositories = new ArrayList<>();
        private final int hashCode;

        public CacheKey( Artifact artifact, boolean resolveManagedVersions, ArtifactRepository localRepository,
                         List<ArtifactRepository> remoteRepositories )
        {
            File file = artifact.getFile();
            this.artifact = ArtifactUtils.copyArtifact( artifact );
            if ( "pom".equals( artifact.getType() ) && file != null )
            {
                pomHash = file.getPath().hashCode() + file.lastModified();
            }
            else
            {
                pomHash = 0;
            }
            this.resolveManagedVersions = resolveManagedVersions;
            this.repositories.add( localRepository );
            this.repositories.addAll( remoteRepositories );

            int hash = 17;
            hash = hash * 31 + artifactHashCode( artifact );
            hash = hash * 31 + ( resolveManagedVersions ? 1 : 2 );
            hash = hash * 31 + repositoriesHashCode( repositories );
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

            return pomHash == other.pomHash && artifactEquals( artifact, other.artifact )
                && resolveManagedVersions == other.resolveManagedVersions
                && repositoriesEquals( repositories, other.repositories );
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
        result = 31 * result + ( a.getDependencyFilter() != null ? a.getDependencyFilter().hashCode() : 0 );
        result = 31 * result + ( a.isOptional() ? 1 : 0 );
        return result;
    }

    private static boolean artifactEquals( Artifact a1, Artifact a2 )
    {
        if ( a1 == a2 )
        {
            return true;
        }

        return Objects.equals( a1.getGroupId(), a2.getGroupId() )
            && Objects.equals( a1.getArtifactId(), a2.getArtifactId() )
            && Objects.equals( a1.getType(), a2.getType() )
            && Objects.equals( a1.getVersion(), a2.getVersion() )
            && Objects.equals( a1.getClassifier(), a2.getClassifier() )
            && Objects.equals( a1.getScope(), a2.getScope() )
            && Objects.equals( a1.getDependencyFilter(), a2.getDependencyFilter() )
            && a1.isOptional() == a2.isOptional();
    }

    private static int repositoryHashCode( ArtifactRepository repository )
    {
        int result = 17;
        result = 31 * result + ( repository.getId() != null ? repository.getId().hashCode() : 0 );
        return result;
    }

    private static int repositoriesHashCode( List<ArtifactRepository> repositories )
    {
        int result = 17;
        for ( ArtifactRepository repository : repositories )
        {
            result = 31 * result + repositoryHashCode( repository );
        }
        return result;
    }

    private static boolean repositoryEquals( ArtifactRepository r1, ArtifactRepository r2 )
    {
        if ( r1 == r2 )
        {
            return true;
        }

        return Objects.equals( r1.getId(), r2.getId() )
            && Objects.equals( r1.getUrl(), r2.getUrl() )
            && repositoryPolicyEquals( r1.getReleases(), r2.getReleases() )
            && repositoryPolicyEquals( r1.getSnapshots(), r2.getSnapshots() );
    }

    private static boolean repositoryPolicyEquals( ArtifactRepositoryPolicy p1, ArtifactRepositoryPolicy p2 )
    {
        if ( p1 == p2 )
        {
            return true;
        }

        return p1.isEnabled() == p2.isEnabled() && Objects.equals( p1.getUpdatePolicy(), p2.getUpdatePolicy() );
    }

    private static boolean repositoriesEquals( List<ArtifactRepository> r1, List<ArtifactRepository> r2 )
    {
        if ( r1.size() != r2.size() )
        {
            return false;
        }

        for ( Iterator<ArtifactRepository> it1 = r1.iterator(), it2 = r2.iterator(); it1.hasNext(); )
        {
            if ( !repositoryEquals( it1.next(), it2.next() ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * CacheRecord
     */
    public class CacheRecord
    {
        private Artifact pomArtifact;
        private Artifact relocatedArtifact;
        private List<Artifact> artifacts;
        private Map<String, Artifact> managedVersions;
        private List<ArtifactRepository> remoteRepositories;

        private long length;
        private long timestamp;

        CacheRecord( Artifact pomArtifact, Artifact relocatedArtifact, Set<Artifact> artifacts,
                     Map<String, Artifact> managedVersions, List<ArtifactRepository> remoteRepositories )
        {
            this.pomArtifact = ArtifactUtils.copyArtifact( pomArtifact );
            this.relocatedArtifact = ArtifactUtils.copyArtifactSafe( relocatedArtifact );
            this.artifacts = ArtifactUtils.copyArtifacts( artifacts, new ArrayList<>() );
            this.remoteRepositories = new ArrayList<>( remoteRepositories );

            this.managedVersions = managedVersions;
            if ( managedVersions != null )
            {
                this.managedVersions =
                    ArtifactUtils.copyArtifacts( managedVersions, new LinkedHashMap<>() );
            }

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

        public Artifact getRelocatedArtifact()
        {
            return relocatedArtifact;
        }

        public List<Artifact> getArtifacts()
        {
            return artifacts;
        }

        public Map<String, Artifact> getManagedVersions()
        {
            return managedVersions;
        }

        public List<ArtifactRepository> getRemoteRepositories()
        {
            return remoteRepositories;
        }

        public boolean isStale()
        {
            File pomFile = pomArtifact.getFile();
            if ( pomFile != null )
            {
                if ( pomFile.canRead() )
                {
                    return length != pomFile.length() || timestamp != pomFile.lastModified();
                }
                else
                {
                    // if the POM didn't exist, retry if any repo is configured to always update
                    boolean snapshot = pomArtifact.isSnapshot();
                    for ( ArtifactRepository repository : remoteRepositories )
                    {
                        ArtifactRepositoryPolicy policy =
                            snapshot ? repository.getSnapshots() : repository.getReleases();
                        if ( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( policy.getUpdatePolicy() ) )
                        {
                            return true;
                        }
                    }
                }
            }

            return length != -1 || timestamp != -1;
        }
    }


    public ResolutionGroup get( Artifact artifact, boolean resolveManagedVersions, ArtifactRepository localRepository,
                                List<ArtifactRepository> remoteRepositories )
    {
        CacheKey cacheKey = newCacheKey( artifact, resolveManagedVersions, localRepository, remoteRepositories );

        CacheRecord cacheRecord = cache.get( cacheKey );

        if ( cacheRecord != null && !cacheRecord.isStale() )
        {
            Artifact pomArtifact = ArtifactUtils.copyArtifact( cacheRecord.getArtifact() );
            Artifact relocatedArtifact = ArtifactUtils.copyArtifactSafe( cacheRecord.getRelocatedArtifact() );
            Set<Artifact> artifacts =
                ArtifactUtils.copyArtifacts( cacheRecord.getArtifacts(), new LinkedHashSet<>() );
            Map<String, Artifact> managedVersions = cacheRecord.getManagedVersions();
            if ( managedVersions != null )
            {
                managedVersions = ArtifactUtils.copyArtifacts( managedVersions, new LinkedHashMap<>() );
            }
            return new ResolutionGroup( pomArtifact, relocatedArtifact, artifacts, managedVersions,
                                        cacheRecord.getRemoteRepositories() );
        }

        cache.remove( cacheKey );

        return null;
    }

    public void put( Artifact artifact, boolean resolveManagedVersions, ArtifactRepository localRepository,
                     List<ArtifactRepository> remoteRepositories, ResolutionGroup result )
    {
        put( newCacheKey( artifact, resolveManagedVersions, localRepository, remoteRepositories ), result );
    }

    protected CacheKey newCacheKey( Artifact artifact, boolean resolveManagedVersions,
                                    ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
    {
        return new CacheKey( artifact, resolveManagedVersions, localRepository, remoteRepositories );
    }

    protected void put( CacheKey cacheKey, ResolutionGroup result )
    {
        CacheRecord cacheRecord =
            new CacheRecord( result.getPomArtifact(), result.getRelocatedArtifact(), result.getArtifacts(),
                             result.getManagedVersions(), result.getResolutionRepositories() );

        cache.put( cacheKey, cacheRecord );
    }

    public void flush()
    {
        cache.clear();
    }
}
