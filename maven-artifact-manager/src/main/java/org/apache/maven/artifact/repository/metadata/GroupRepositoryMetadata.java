package org.apache.maven.artifact.repository.metadata;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Metadata for the group directory of the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class GroupRepositoryMetadata
    implements ArtifactMetadata
{
    /**
     * TODO: reuse.
     */
    protected static final String METADATA_FILE = "maven-metadata.xml";

    private final String groupId;

    private Map pluginMappings = new HashMap();

    public GroupRepositoryMetadata( String groupId )
    {
        this.groupId = groupId;
    }

    public String toString()
    {
        return "repository metadata for group: \'" + groupId + "\'";
    }

    public void storeInLocalRepository( ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( !pluginMappings.isEmpty() )
        {
            try
            {
                updateRepositoryMetadata( localRepository );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( "Error updating group repository metadata", e );
            }
        }
    }

    public String getFilename()
    {
        return METADATA_FILE;
    }

    public boolean storedInGroupDirectory()
    {
        return true;
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return false;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return null;
    }

    public String getBaseVersion()
    {
        return null;
    }

    public void addPluginMapping( String goalPrefix, String artifactId )
    {
        pluginMappings.put( goalPrefix, artifactId );
    }

    private void updateRepositoryMetadata( ArtifactRepository localRepository )
        throws IOException
    {
        MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

        Metadata pluginMap = null;

        File metadataFile = new File( localRepository.getBasedir(), localRepository.pathOfArtifactMetadata( this ) );

        if ( metadataFile.exists() )
        {
            Reader reader = null;

            try
            {
                reader = new FileReader( metadataFile );

                pluginMap = mappingReader.read( reader );
            }
            catch ( FileNotFoundException e )
            {
                // TODO: Log a warning
            }
            catch ( IOException e )
            {
                // TODO: Log a warning
            }
            catch ( XmlPullParserException e )
            {
                // TODO: Log a warning
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        // If file could not be found or was not valid, start from scratch
        if ( pluginMap == null )
        {
            pluginMap = new Metadata();

            pluginMap.setGroupId( groupId );
        }

        for ( Iterator i = pluginMappings.keySet().iterator(); i.hasNext(); )
        {
            String prefix = (String) i.next();
            boolean found = false;

            for ( Iterator it = pluginMap.getPlugins().iterator(); it.hasNext() && !found; )
            {
                Plugin preExisting = (Plugin) it.next();

                if ( preExisting.getPrefix().equals( prefix ) )
                {
                    // TODO: log
//                    getLog().info( "Plugin-mapping metadata for prefix: " + prefix + " already exists. Skipping." );

                    found = true;
                }
            }

            if ( !found )
            {
                Plugin mappedPlugin = new Plugin();

                mappedPlugin.setArtifactId( (String) pluginMappings.get( prefix ) );

                mappedPlugin.setPrefix( prefix );

                pluginMap.addPlugin( mappedPlugin );
            }
        }

        Writer writer = null;
        try
        {
            writer = new FileWriter( metadataFile );

            MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

            mappingWriter.write( writer, pluginMap );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public Object getKey()
    {
        return groupId;
    }

    public boolean isSnapshot()
    {
        return false;
    }
}
