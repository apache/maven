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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

/**
 * Caches raw plugin descriptors. A raw plugin descriptor is a descriptor that has just been extracted from the plugin
 * artifact and does not contain any runtime specific data. The cache must not be used for descriptors that hold runtime
 * data like the plugin realm. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 * 
 * @since 3.0-alpha-3
 * @author Benjamin Bentmann
 */
@Component( role = PluginDescriptorCache.class )
public class DefaultPluginDescriptorCache
    implements PluginDescriptorCache
{

    private Map<Key, PluginDescriptor> descriptors = new HashMap<Key, PluginDescriptor>( 128 );

    public void flush()
    {
        descriptors.clear();
    }

    public Key createKey( Plugin plugin, RepositoryRequest repositoryRequest )
    {
        return new CacheKey( plugin, repositoryRequest );
    }

    public void put( Key cacheKey, PluginDescriptor pluginDescriptor )
    {
        descriptors.put( cacheKey, clone( pluginDescriptor ) );
    }

    public PluginDescriptor get( Key cacheKey )
    {
        return clone( descriptors.get( cacheKey ) );
    }

    private static PluginDescriptor clone( PluginDescriptor original )
    {
        PluginDescriptor clone = null;

        if ( original != null )
        {
            clone = new PluginDescriptor();

            clone.setGroupId( original.getGroupId() );
            clone.setArtifactId( original.getArtifactId() );
            clone.setVersion( original.getVersion() );
            clone.setGoalPrefix( original.getGoalPrefix() );
            clone.setInheritedByDefault( original.isInheritedByDefault() );

            clone.setName( original.getName() );
            clone.setDescription( original.getDescription() );

            clone.setPluginArtifact( ArtifactUtils.copyArtifactSafe( original.getPluginArtifact() ) );

            clone.setComponents( clone( original.getMojos(), clone ) );
            clone.setId( original.getId() );
            clone.setIsolatedRealm( original.isIsolatedRealm() );
            clone.setSource( original.getSource() );
        }

        return clone;
    }

    private static List<ComponentDescriptor<?>> clone( List<MojoDescriptor> mojos, PluginDescriptor pluginDescriptor )
    {
        List<ComponentDescriptor<?>> clones = null;

        if ( mojos != null )
        {
            clones = new ArrayList<ComponentDescriptor<?>>( mojos.size() );

            for ( MojoDescriptor mojo : mojos )
            {
                MojoDescriptor clone = mojo.clone();
                clone.setPluginDescriptor( pluginDescriptor );
                clones.add( clone );
            }
        }

        return clones;
    }

    private static final class CacheKey
        implements Key
    {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final List<ArtifactRepository> repositories;

        private final int hashCode;

        public CacheKey( Plugin plugin, RepositoryRequest repositoryRequest )
        {
            groupId = plugin.getGroupId();
            artifactId = plugin.getArtifactId();
            version = plugin.getVersion();

            repositories = new ArrayList<ArtifactRepository>( repositoryRequest.getRemoteRepositories().size() + 1 );
            repositories.add( repositoryRequest.getLocalRepository() );
            repositories.addAll( repositoryRequest.getRemoteRepositories() );

            int hash = 17;
            hash = hash * 31 + groupId.hashCode();
            hash = hash * 31 + artifactId.hashCode();
            hash = hash * 31 + version.hashCode();
            hash = hash * 31 + repositoriesHashCode( repositories );
            this.hashCode = hash;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }

            if ( !( obj instanceof CacheKey ) )
            {
                return false;
            }

            CacheKey that = (CacheKey) obj;

            return this.artifactId.equals( that.artifactId ) && this.groupId.equals( that.groupId )
                && this.version.equals( that.version ) && repositoriesEquals( this.repositories, that.repositories );
        }

        @Override
        public String toString()
        {
            return groupId + ':' + artifactId + ':' + version;
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

            return eq( r1.getId(), r2.getId() ) && eq( r1.getUrl(), r2.getUrl() )
                && repositoryPolicyEquals( r1.getReleases(), r2.getReleases() )
                && repositoryPolicyEquals( r1.getSnapshots(), r2.getSnapshots() );
        }

        private static boolean repositoryPolicyEquals( ArtifactRepositoryPolicy p1, ArtifactRepositoryPolicy p2 )
        {
            if ( p1 == p2 )
            {
                return true;
            }

            return p1.isEnabled() == p2.isEnabled() && eq( p1.getUpdatePolicy(), p2.getUpdatePolicy() );
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

        private static <T> boolean eq( T s1, T s2 )
        {
            return s1 != null ? s1.equals( s2 ) : s2 == null;
        }

    }

}
