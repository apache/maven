package org.apache.maven.artifact.metadata;

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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Attach a POM to an artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class MavenMetadata
    extends AbstractArtifactMetadata
{
    private final File file;

    public MavenMetadata( Artifact artifact, File file )
    {
        super( artifact, null );
        this.file = file;
    }

    public String getFilename()
    {
        return getArtifact().getArtifactId() + "-" + getArtifact().getVersion() + ".pom";
    }

    public void storeInLocalRepository( ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        File destination;
        try
        {
            destination = new File( localRepository.getBasedir(), localRepository.pathOfMetadata( this ) );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to install POM", e );
        }

        destination.getParentFile().mkdirs();

        FileReader reader = null;
        FileWriter writer = null;
        try
        {
            reader = new FileReader( file );
            writer = new FileWriter( destination );

            MavenXpp3Reader modelReader = new MavenXpp3Reader();
            Model model = modelReader.read( reader );
            model.setVersion( getArtifact().getVersion() );

            MavenXpp3Writer modelWriter = new MavenXpp3Writer();
            modelWriter.write( writer, model );
        }
        catch ( Exception e )
        {
            throw new ArtifactMetadataRetrievalException( "Error rewriting POM", e );
        }
        finally
        {
            IOUtil.close( reader );
            IOUtil.close( writer );
        }
    }

    public void retrieveFromRemoteRepository( ArtifactRepository remoteRepository, WagonManager wagonManager )
    {
        // not used - TODO: again indicates bad design?
    }
}
