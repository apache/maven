package org.apache.maven.repository.internal;

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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.repository.internal.PluginsMetadataInfoProvider.PluginInfo;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.util.ConfigUtils;

import static java.util.Objects.requireNonNull;

/**
 * Plugin G level metadata.
 */
class PluginsMetadataGenerator
    implements MetadataGenerator
{
    private final PluginsMetadataInfoProvider pluginsMetadataInfoProvider;

    private final Map<Object, PluginsMetadata> plugins;

    private final Map<Object, PluginsMetadata> processedPlugins;

    private final Date timestamp;

    PluginsMetadataGenerator( PluginsMetadataInfoProvider pluginsMetadataInfoProvider,
                              RepositorySystemSession session,
                              InstallRequest request )
    {
        this( pluginsMetadataInfoProvider, session, request.getMetadata() );
    }

    PluginsMetadataGenerator( PluginsMetadataInfoProvider pluginsMetadataInfoProvider,
                              RepositorySystemSession session,
                              DeployRequest request )
    {
        this( pluginsMetadataInfoProvider, session, request.getMetadata() );
    }

    private PluginsMetadataGenerator( PluginsMetadataInfoProvider pluginsMetadataInfoProvider,
                                      RepositorySystemSession session,
                                      Collection<? extends Metadata> metadatas )
    {
        this.pluginsMetadataInfoProvider = requireNonNull( pluginsMetadataInfoProvider );
        this.plugins = new LinkedHashMap<>();
        this.processedPlugins = new LinkedHashMap<>();
        this.timestamp = (Date) ConfigUtils.getObject( session, new Date(), "maven.startTime" );

        /*
         * NOTE: This should be considered a quirk to support interop with Maven's legacy ArtifactDeployer which
         * processes one artifact at a time and hence cannot associate the artifacts from the same project to use the
         * same version index. Allowing the caller to pass in metadata from a previous deployment allows to re-establish
         * the association between the artifacts of the same project.
         */
        for ( Iterator<? extends Metadata> it = metadatas.iterator(); it.hasNext(); )
        {
            Metadata metadata = it.next();
            if ( metadata instanceof PluginsMetadata )
            {
                it.remove();
                PluginsMetadata pluginMetadata = ( PluginsMetadata ) metadata;
                processedPlugins.put( pluginMetadata.getKey(), pluginMetadata );
            }
        }
    }

    @Override
    public Collection<? extends Metadata> prepare( Collection<? extends Artifact> artifacts )
    {
        return Collections.emptyList();
    }

    @Override
    public Artifact transformArtifact( Artifact artifact )
    {
        return artifact;
    }

    @Override
    public Collection<? extends Metadata> finish( Collection<? extends Artifact> artifacts )
    {
        for ( Artifact artifact : artifacts )
        {
            PluginInfo pluginInfo = pluginsMetadataInfoProvider.getPluginInfo( artifact );
            if ( pluginInfo != null )
            {
                Object key = PluginsMetadata.getKey( artifact );
                if ( processedPlugins.get( key ) == null )
                {
                    PluginsMetadata pluginMetadata = plugins.get( key );
                    if ( pluginMetadata == null )
                    {
                        pluginMetadata = new PluginsMetadata( pluginInfo, timestamp );
                        plugins.put( key, pluginMetadata );
                    }
                }
            }
        }

        return plugins.values();
    }
}
