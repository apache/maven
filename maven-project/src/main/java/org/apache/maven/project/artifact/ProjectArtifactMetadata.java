package org.apache.maven.project.artifact;

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
import org.apache.maven.artifact.ArtifactStatus;
import org.apache.maven.artifact.metadata.AbstractArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Attach a POM to an artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ProjectArtifactMetadata
    extends AbstractArtifactMetadata
{
    private final File file;

    public ProjectArtifactMetadata( Artifact artifact )
    {
        this( artifact, null );
    }

    public ProjectArtifactMetadata( Artifact artifact, File file )
    {
        super( artifact );
        this.file = file;
    }

    public String getRemoteFilename()
    {
        return getFilename();
    }

    public String getLocalFilename( ArtifactRepository repository )
    {
        return getFilename();
    }

    private String getFilename()
    {
        return getArtifactId() + "-" + artifact.getVersion() + ".pom";
    }

    public void storeInLocalRepository( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws RepositoryMetadataStoreException
    {
        File destination = new File( localRepository.getBasedir(),
                                     localRepository.pathOfLocalRepositoryMetadata( this, remoteRepository ) );

        destination.getParentFile().mkdirs();

        FileReader reader = null;
        FileWriter writer = null;
        try
        {
            reader = new FileReader( file );
            StringWriter sWriter = new StringWriter();
            IOUtil.copy( reader, sWriter );
            
            String modelSrc = sWriter.toString().replaceAll( "\\$\\{(pom\\.|project\\.)?version\\}", artifact.getBaseVersion() );
            
            StringReader sReader = new StringReader( modelSrc );
            
            writer = new FileWriter( destination );

            MavenXpp3Reader modelReader = new MavenXpp3Reader();
            Model model = modelReader.read( sReader, false );
            model.setVersion( artifact.getVersion() );

            DistributionManagement distributionManagement = model.getDistributionManagement();
            if ( distributionManagement == null )
            {
                distributionManagement = new DistributionManagement();
                model.setDistributionManagement( distributionManagement );
            }
            distributionManagement.setStatus( ArtifactStatus.DEPLOYED.toString() );

            MavenXpp3Writer modelWriter = new MavenXpp3Writer();
            modelWriter.write( writer, model );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryMetadataStoreException( "Error rewriting POM", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataStoreException( "Error rewriting POM", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataStoreException( "Error rewriting POM", e );
        }
        finally
        {
            IOUtil.close( reader );
            IOUtil.close( writer );
        }
    }

    public String toString()
    {
        return "project information for " + artifact.getArtifactId() + " " + artifact.getVersion();
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return true;
    }

    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }

    public Object getKey()
    {
        return "project " + artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    public void merge( ArtifactMetadata metadata )
    {
        ProjectArtifactMetadata m = (ProjectArtifactMetadata) metadata;
        if ( !m.file.equals( file ) )
        {
            throw new IllegalStateException( "Cannot add two different pieces of metadata for: " + getKey() );
        }
    }
}
