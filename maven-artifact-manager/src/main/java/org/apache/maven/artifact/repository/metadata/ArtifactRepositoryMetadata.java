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
import java.util.Iterator;

/**
 * Metadata for the artifact directory of the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo split instantiation (versioning, plugin mappings) from definition
 */
public class ArtifactRepositoryMetadata
    extends AbstractRepositoryMetadata
{
    private Versioning versioning;

    private Artifact artifact;

    public ArtifactRepositoryMetadata( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public ArtifactRepositoryMetadata( Artifact artifact, Versioning versioning )
    {
        this.versioning = versioning;
        this.artifact = artifact;
    }

    public String toString()
    {
        return "repository metadata for: \'" + getKey() + "\'";
    }

    public boolean storedInGroupDirectory()
    {
        return false;
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return false;
    }

    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    public String getBaseVersion()
    {
        return null;
    }

    protected void updateRepositoryMetadata( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws IOException
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

                metadata = mappingReader.read( reader );
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

        boolean changed = false;

        // If file could not be found or was not valid, start from scratch
        if ( metadata == null )
        {
            metadata = new Metadata();

            metadata.setGroupId( artifact.getGroupId() );
            metadata.setArtifactId( artifact.getArtifactId() );
            changed = true;
        }

        if ( versioning != null )
        {
            Versioning v = metadata.getVersioning();
            if ( v != null )
            {
                if ( versioning.getRelease() != null )
                {
                    changed = true;
                    v.setRelease( versioning.getRelease() );
                }
                if ( versioning.getLatest() != null )
                {
                    changed = true;
                    v.setLatest( versioning.getLatest() );
                }
                for ( Iterator i = versioning.getVersions().iterator(); i.hasNext(); )
                {
                    String version = (String) i.next();
                    if ( !v.getVersions().contains( version ) )
                    {
                        changed = true;
                        v.getVersions().add( version );
                    }
                }
            }
            else
            {
                metadata.setVersioning( versioning );
                changed = true;
            }
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

    public Object getKey()
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    public boolean isSnapshot()
    {
        return artifact.isSnapshot();
    }

    public Snapshot getSnapshot()
    {
        return null;
    }

}
