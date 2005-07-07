package org.apache.maven.plugin.mapping;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.mapping.io.xpp3.PluginMappingXpp3Reader;
import org.apache.maven.plugin.mapping.metadata.PluginMappingMetadata;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

public class DefaultPluginMappingBuilder
    implements MavenPluginMappingBuilder
{

    // component requirement
    private RepositoryMetadataManager repositoryMetadataManager;

    public PluginMappingManager loadPluginMappings( List pluginGroupIds, List pluginRepositories,
                                                   ArtifactRepository localRepository )
        throws RepositoryMetadataManagementException, PluginMappingManagementException
    {
        PluginMappingManager mappingManager = new PluginMappingManager();

        if ( pluginGroupIds != null )
        {
            for ( Iterator it = pluginGroupIds.iterator(); it.hasNext(); )
            {
                String groupId = (String) it.next();

                File mappingFile = resolveMappingMetadata( groupId, pluginRepositories, localRepository );

                PluginMap pluginMap = readPluginMap( mappingFile );
                
                if ( pluginMap != null )
                {
                    mappingManager.addPluginMap( pluginMap );
                }
            }
        }

        return mappingManager;
    }

    private PluginMap readPluginMap( File mappingFile ) throws PluginMappingManagementException
    {
        if( mappingFile.exists() )
        {
            Reader fileReader = null;
            try
            {
                fileReader = new FileReader( mappingFile );

                PluginMappingXpp3Reader mappingReader = new PluginMappingXpp3Reader();
                
                return mappingReader.read(fileReader);
            }
            catch ( IOException e )
            {
                throw new PluginMappingManagementException( "Cannot read plugin mappings from: " + mappingFile, e );
            }
            catch ( XmlPullParserException e )
            {
                throw new PluginMappingManagementException( "Cannot parse plugin mappings from: " + mappingFile, e );
            }
            finally
            {
                IOUtil.close( fileReader );
            }
        }
        else
        {
            return null;
        }
    }

    private File resolveMappingMetadata( String groupId, List pluginRepositories, ArtifactRepository localRepository )
        throws RepositoryMetadataManagementException
    {
        PluginMappingMetadata metadata = new PluginMappingMetadata( groupId );

        RepositoryMetadataManagementException repositoryException = null;

        for ( Iterator repoIterator = pluginRepositories.iterator(); repoIterator.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) repoIterator.next();

            try
            {
                repositoryMetadataManager.get( metadata, repository, localRepository );

                break;
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

        return metadata.getFile();
    }

}
