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

import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
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

        for ( Iterator it = pluginGroupIds.iterator(); it.hasNext(); )
        {
            String groupId = (String) it.next();

            try
            {
                loadPluginMappings( groupId, pluginRepositories, localRepository );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                getLogger().warn( "Cannot resolve plugin-mapping metadata for groupId: " + groupId + " - IGNORING." );

                getLogger().debug( "Error resolving plugin-mapping metadata for groupId: " + groupId + ".", e );
            }
        }
    }

    private void loadPluginMappings( String groupId, List pluginRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        GroupRepositoryMetadata metadata = new GroupRepositoryMetadata( groupId );

        repositoryMetadataManager.resolve( metadata, pluginRepositories, localRepository );

        // TODO: can this go directly into the manager?
        for ( Iterator i = pluginRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            loadRepositoryPluginMappings( metadata, repository, localRepository );
        }
        loadRepositoryPluginMappings( metadata, localRepository, localRepository );
    }

    private void loadRepositoryPluginMappings( GroupRepositoryMetadata metadata, ArtifactRepository remoteRepository,
                                               ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        File metadataFile = new File( localRepository.getBasedir(),
                                      localRepository.pathOfLocalRepositoryMetadata( metadata, remoteRepository ) );

        if ( metadataFile.exists() )
        {
            Metadata pluginMap = readMetadata( metadataFile );

            if ( pluginMap != null )
            {
                for ( Iterator pluginIterator = pluginMap.getPlugins().iterator(); pluginIterator.hasNext(); )
                {
                    Plugin mapping = (Plugin) pluginIterator.next();

                    String prefix = mapping.getPrefix();

                    String artifactId = mapping.getArtifactId();

                    org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

                    plugin.setGroupId( metadata.getGroupId() );

                    plugin.setArtifactId( artifactId );

                    pluginDefinitionsByPrefix.put( prefix, plugin );
                }
            }
        }
    }

    private static Metadata readMetadata( File mappingFile )
        throws ArtifactMetadataRetrievalException
    {
        Metadata result;

        Reader fileReader = null;
        try
        {
            fileReader = new FileReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot read plugin mappings from: " + mappingFile, e );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot read plugin mappings from: " + mappingFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot parse plugin mappings from: " + mappingFile, e );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
        return result;
    }
}
