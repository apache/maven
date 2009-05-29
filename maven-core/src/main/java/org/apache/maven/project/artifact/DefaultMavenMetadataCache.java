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
import java.util.Collection;
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
        Artifact artifact;
        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();

        CacheKey( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        {
            this.artifact = ArtifactUtils.copyArtifact( artifact );
            this.repositories.add( localRepository );
            this.repositories.addAll( remoteRepositories );
        }

        @Override
        public int hashCode()
        {
            int hash = 17;
            hash = hash * 31 + artifact.hashCode();
            hash = hash * 31 + repositories.hashCode();

            return hash;
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
            
            return artifact.equals( other.artifact ) && repositories.equals( other.repositories );
        }
    }

    public class CacheRecord
    {
        Artifact pomArtifact;
        List<Artifact> artifacts;
        List<ArtifactRepository> remoteRepositories;

        long length;
        long timestamp;

        CacheRecord(Artifact pomArtifact, Set<Artifact> artifacts, List<ArtifactRepository> remoteRepositories)
        {
            this.pomArtifact = ArtifactUtils.copyArtifact( pomArtifact );
            this.artifacts = copyArtifacts( artifacts );
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
    
    private Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();

    public ResolutionGroup get( Artifact artifact, ArtifactRepository localRepository,
                                List<ArtifactRepository> remoteRepositories )
    {
        CacheKey cacheKey = new CacheKey( artifact, localRepository, remoteRepositories );

        CacheRecord cacheRecord = cache.get( cacheKey );

        if ( cacheRecord != null && !cacheRecord.isStale() )
        {
            Artifact pomArtifact = ArtifactUtils.copyArtifact( cacheRecord.pomArtifact );
            Set<Artifact> artifacts = new LinkedHashSet<Artifact>( copyArtifacts( cacheRecord.artifacts ) );
            return new ResolutionGroup( pomArtifact, artifacts , cacheRecord.remoteRepositories );
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

    public static List<Artifact> copyArtifacts( Collection<Artifact> artifacts )
    {
        ArrayList<Artifact> result = new ArrayList<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            result.add( ArtifactUtils.copyArtifact( artifact ) );
        }
        return result;
    }

}
