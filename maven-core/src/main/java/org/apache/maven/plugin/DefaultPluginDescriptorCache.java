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
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
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

    private static <T> List<T> clone( List<T> original )
    {
        return ( original != null ) ? new ArrayList<T>( original ) : null;
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
            hash = hash * 31 + repositories.hashCode();
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
                && this.version.equals( that.version ) && this.repositories.equals( that.repositories );
        }

        @Override
        public String toString()
        {
            return groupId + ':' + artifactId + ':' + version;
        }

    }

}
