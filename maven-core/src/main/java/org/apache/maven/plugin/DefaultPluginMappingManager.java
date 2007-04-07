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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manage plugin prefix to artifact ID mapping associations.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: DefaultPluginMappingManager.java 495147 2007-01-11 07:47:53Z
 *          jvanzyl $
 */
public class DefaultPluginMappingManager
    extends AbstractLogEnabled
    implements PluginMappingManager
{
    protected RepositoryMetadataManager repositoryMetadataManager;

    private Map pluginDefinitionsByPrefix = new HashMap();

    public org.apache.maven.model.Plugin getByPrefix( String pluginPrefix, List groupIds, List pluginRepositories,
                                                      ArtifactRepository localRepository )
    {
        // if not found, try from the remote repository
        if ( !pluginDefinitionsByPrefix.containsKey( pluginPrefix ) )
        {
            getLogger().info( "Searching repository for plugin with prefix: \'" + pluginPrefix + "\'." );

            loadPluginMappings( groupIds, pluginRepositories, localRepository );
        }

        return (org.apache.maven.model.Plugin) pluginDefinitionsByPrefix.get( pluginPrefix );
    }

    private void loadPluginMappings( List groupIds, List pluginRepositories, ArtifactRepository localRepository )
    {
        List pluginGroupIds = new ArrayList( groupIds );

        // TODO: use constant
        if ( !pluginGroupIds.contains( "org.apache.maven.plugins" ) )
        {
            pluginGroupIds.add( "org.apache.maven.plugins" );
        }
        if ( !pluginGroupIds.contains( "org.codehaus.mojo" ) )
        {
            pluginGroupIds.add( "org.codehaus.mojo" );
        }

        for ( Iterator it = pluginGroupIds.iterator(); it.hasNext(); )
        {
            String groupId = (String) it.next();
            getLogger().debug( "Loading plugin prefixes from group: " + groupId );
            try
            {
                loadPluginMappings( groupId, pluginRepositories, localRepository );
            }
            catch ( RepositoryMetadataResolutionException e )
            {
                getLogger().warn( "Cannot resolve plugin-mapping metadata for groupId: " + groupId + " - IGNORING." );

                getLogger().debug( "Error resolving plugin-mapping metadata for groupId: " + groupId + ".", e );
            }
        }
    }

    private void loadPluginMappings( String groupId, List pluginRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataResolutionException
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( groupId );

        repositoryMetadataManager.resolve( metadata, pluginRepositories, localRepository );

        Metadata repoMetadata = metadata.getMetadata();
        if ( repoMetadata != null )
        {
            for ( Iterator pluginIterator = repoMetadata.getPlugins().iterator(); pluginIterator.hasNext(); )
            {
                Plugin mapping = (Plugin) pluginIterator.next();

                String prefix = mapping.getPrefix();
                
                //if the prefix has already been found, don't add it again.
                //this is to preserve the correct ordering of prefix searching (MNG-2926)
                if ( !pluginDefinitionsByPrefix.containsKey( prefix ) )
                {
                    String artifactId = mapping.getArtifactId();

                    org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

                    plugin.setGroupId( metadata.getGroupId() );

                    plugin.setArtifactId( artifactId );

                    pluginDefinitionsByPrefix.put( prefix, plugin );
                }
            }
        }
    }
}
