package org.apache.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.InvalidRepositoryMetadataException;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.PluginMappingMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manage plugin prefix to artifact ID mapping associations.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultPluginMappingManager
    extends AbstractLogEnabled
    implements PluginMappingManager
{
    protected RepositoryMetadataManager repositoryMetadataManager;

    private List mappings = new ArrayList();

    private boolean refreshed;

    private Map pluginDefinitionsByPrefix;

    public void clear()
    {
        this.mappings = null;
        clearCache();
    }

    private void clearCache()
    {
        this.pluginDefinitionsByPrefix = null;
    }

    public org.apache.maven.model.Plugin getByPrefix( String pluginPrefix, List groupIds, List pluginRepositories,
                                                      ArtifactRepository localRepository )
        throws RepositoryMetadataManagementException
    {
        if ( pluginDefinitionsByPrefix == null )
        {
            // firstly, search the local repository
            loadPluginMappings( groupIds, pluginRepositories, localRepository );

            calculatePluginDefinitionsByPrefix();

            // if not found, try from the remote repository
            if ( !pluginDefinitionsByPrefix.containsKey( pluginPrefix ) && !refreshed )
            {
                getLogger().info(
                    "Refreshing plugin mapping metadata; looking for plugin with prefix: \'" + pluginPrefix + "\'." );

                refreshPluginMappingManager( pluginRepositories, localRepository );

                refreshed = true;
            }

            calculatePluginDefinitionsByPrefix();
        }
        return (org.apache.maven.model.Plugin) pluginDefinitionsByPrefix.get( pluginPrefix );
    }

    private void calculatePluginDefinitionsByPrefix()
    {
        pluginDefinitionsByPrefix = new HashMap();

        for ( Iterator it = mappings.iterator(); it.hasNext(); )
        {
            Metadata pluginMap = (Metadata) it.next();

            String groupId = pluginMap.getGroupId();

            for ( Iterator pluginIterator = pluginMap.getPlugins().iterator(); pluginIterator.hasNext(); )
            {
                Plugin mapping = (Plugin) pluginIterator.next();

                String prefix = mapping.getPrefix();

                String artifactId = mapping.getArtifactId();

                org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

                plugin.setGroupId( groupId );

                plugin.setArtifactId( artifactId );

                pluginDefinitionsByPrefix.put( prefix, plugin );
            }
        }
    }

    private void refreshPluginMappingManager( List pluginRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataManagementException
    {
        List groupIds = new ArrayList();

        for ( Iterator it = mappings.iterator(); it.hasNext(); )
        {
            Metadata map = (Metadata) it.next();

            String groupId = map.getGroupId();

            groupIds.add( groupId );

            repositoryMetadataManager.purgeLocalCopy( new PluginMappingMetadata( groupId ), localRepository );
        }

        loadPluginMappings( groupIds, pluginRepositories, localRepository );
    }

    private void loadPluginMappings( List groupIds, List pluginRepositories, ArtifactRepository localRepository )
    {
        List pluginGroupIds = new ArrayList( groupIds );

        // TODO: use constant
        if ( !pluginGroupIds.contains( "org.apache.maven.plugins" ) )
        {
            pluginGroupIds.add( "org.apache.maven.plugins" );
        }

        for ( Iterator it = pluginGroupIds.iterator(); it.hasNext(); )
        {
            String groupId = (String) it.next();

            try
            {
                File mappingFile = resolveMappingMetadata( repositoryMetadataManager, groupId, pluginRepositories,
                                                           localRepository );

                Metadata pluginMap = readPluginMap( mappingFile );

                if ( pluginMap != null )
                {
                    mappings.add( pluginMap );
                }
            }
            catch ( RepositoryMetadataManagementException e )
            {
                getLogger().warn( "Cannot resolve plugin-mapping metadata for groupId: " + groupId + " - IGNORING." );

                getLogger().debug( "Error resolving plugin-mapping metadata for groupId: " + groupId + ".", e );
            }

            clearCache();
        }
    }

    private static Metadata readPluginMap( File mappingFile )
        throws RepositoryMetadataManagementException
    {
        Metadata result = null;

        if ( mappingFile.exists() )
        {
            Reader fileReader = null;
            try
            {
                fileReader = new FileReader( mappingFile );

                MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

                result = mappingReader.read( fileReader );
            }
            catch ( FileNotFoundException e )
            {
                throw new RepositoryMetadataManagementException( "Cannot read plugin mappings from: " + mappingFile,
                                                                 e );
            }
            catch ( IOException e )
            {
                throw new RepositoryMetadataManagementException( "Cannot read plugin mappings from: " + mappingFile,
                                                                 e );
            }
            catch ( XmlPullParserException e )
            {
                throw new RepositoryMetadataManagementException( "Cannot parse plugin mappings from: " + mappingFile,
                                                                 e );
            }
            finally
            {
                IOUtil.close( fileReader );
            }
        }

        return result;
    }

    private static File resolveMappingMetadata( RepositoryMetadataManager repositoryMetadataManager, String groupId,
                                                List pluginRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataManagementException
    {
        PluginMappingMetadata metadata = new PluginMappingMetadata( groupId );

        RepositoryMetadataManagementException repositoryException = null;

        for ( Iterator repoIterator = pluginRepositories.iterator(); repoIterator.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) repoIterator.next();

            try
            {
                repositoryMetadataManager.resolve( metadata, repository, localRepository );

                // reset this to keep it from getting in the way when we succeed but not on first repo...
                repositoryException = null;

                File metadataFile = new File( localRepository.getBasedir(),
                                              localRepository.pathOfRepositoryMetadata( metadata ) );

                if ( metadataFile.exists() )
                {
                    return metadataFile;
                }
            }
            catch ( InvalidRepositoryMetadataException e )
            {
                repositoryMetadataManager.purgeLocalCopy( metadata, localRepository );
            }
            catch ( RepositoryMetadataManagementException e )
            {
                repositoryException = e;
            }
        }

        if ( repositoryException != null )
        {
            throw repositoryException;
        }

        throw new RepositoryMetadataManagementException( "No repository metadata found" );
    }
}
