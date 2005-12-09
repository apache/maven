package org.apache.maven.artifact.repository.metadata;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Shared methods of the repository metadata handling.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractRepositoryMetadata
    implements RepositoryMetadata
{
    private Metadata metadata;

    protected AbstractRepositoryMetadata( Metadata metadata )
    {
        this.metadata = metadata;
    }

    public String getRemoteFilename()
    {
        return "maven-metadata.xml";
    }

    public String getLocalFilename( ArtifactRepository repository )
    {
        return "maven-metadata-" + repository.getKey() + ".xml";
    }

    public void storeInLocalRepository( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws RepositoryMetadataStoreException
    {
        try
        {
            updateRepositoryMetadata( localRepository, remoteRepository );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataStoreException( "Error updating group repository metadata", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataStoreException( "Error updating group repository metadata", e );
        }
    }

    protected void updateRepositoryMetadata( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws IOException, XmlPullParserException
    {
        MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

        Metadata metadata = null;

        File metadataFile = new File( localRepository.getBasedir(),
                                      localRepository.pathOfLocalRepositoryMetadata( this, remoteRepository ) );

        if ( metadataFile.exists() )
        {
            Reader reader = null;

            try
            {
                reader = new FileReader( metadataFile );

                metadata = mappingReader.read( reader, false );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        boolean changed;

        // If file could not be found or was not valid, start from scratch
        if ( metadata == null )
        {
            metadata = this.metadata;

            changed = true;
        }
        else
        {
            changed = metadata.merge( this.metadata );
        }

        if ( changed )
        {
            Writer writer = null;
            try
            {
                metadataFile.getParentFile().mkdirs();
                writer = new FileWriter( metadataFile );

                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

                mappingWriter.write( writer, metadata );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        else
        {
            metadataFile.setLastModified( System.currentTimeMillis() );
        }
    }

    public String toString()
    {
        return "repository metadata for: \'" + getKey() + "\'";
    }

    protected static Metadata createMetadata( Artifact artifact, Versioning versioning )
    {
        Metadata metadata = new Metadata();
        metadata.setGroupId( artifact.getGroupId() );
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setVersion( artifact.getVersion() );
        metadata.setVersioning( versioning );
        return metadata;
    }

    protected static Versioning createVersioning( Snapshot snapshot )
    {
        Versioning versioning = new Versioning();
        versioning.setSnapshot( snapshot );
        versioning.updateTimestamp();
        return versioning;
    }

    public void setMetadata( Metadata metadata )
    {
        this.metadata = metadata;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    public void merge( ArtifactMetadata metadata )
    {
        // TODO: not sure that it should assume this, maybe the calls to addMetadata should pre-merge, then artifact replaces?
        AbstractRepositoryMetadata repoMetadata = (AbstractRepositoryMetadata) metadata;
        this.metadata.merge( repoMetadata.getMetadata() );
    }

    public String extendedToString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append( "\nRepository Metadata\n--------------------------" );
        buffer.append( "\nGroupId: " ).append( getGroupId() );
        buffer.append( "\nArtifactId: " ).append( getArtifactId() );
        buffer.append( "\nMetadata Type: " ).append( getClass().getName() );

        return buffer.toString();
    }
}
