package org.apache.maven.repository;

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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.ArtifactDownload;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.transfer.ArtifactNotFoundException;
import org.sonatype.aether.transfer.ArtifactTransferException;

/**
 * @author Benjamin Bentmann
 */
public class TestRepositoryConnector
    implements RepositoryConnector
{

    private RemoteRepository repository;

    private File basedir;

    public TestRepositoryConnector( RemoteRepository repository )
    {
        this.repository = repository;
        try
        {
            basedir = FileUtils.toFile( new URL( repository.getUrl() ) );
        }
        catch ( MalformedURLException e )
        {
            throw new IllegalStateException( e );
        }
    }

    public void close()
    {
    }

    public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                     Collection<? extends MetadataDownload> metadataDownloads )
    {
        if ( artifactDownloads != null )
        {
            for ( ArtifactDownload download : artifactDownloads )
            {
                File remoteFile = new File( basedir, path( download.getArtifact() ) );
                try
                {
                    FileUtils.copyFile( remoteFile, download.getFile() );
                }
                catch ( IOException e )
                {
                    if ( !remoteFile.exists() )
                    {
                        download.setException( new ArtifactNotFoundException( download.getArtifact(), repository ) );
                    }
                    else
                    {
                        download.setException( new ArtifactTransferException( download.getArtifact(), repository, e ) );
                    }
                }
            }
        }
    }

    private String path( Artifact artifact )
    {
        StringBuilder path = new StringBuilder( 128 );

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '/' );

        path.append( artifact.getBaseVersion() ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );

        if ( artifact.getClassifier().length() > 0 )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        path.append( '.' ).append( artifact.getExtension() );

        return path.toString();
    }

    public void put( Collection<? extends ArtifactUpload> artifactUploads,
                     Collection<? extends MetadataUpload> metadataUploads )
    {
        // TODO Auto-generated method stub

    }

}
