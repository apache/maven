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
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Contains the information stored for a snapshot.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class SnapshotArtifactMetadata
    extends AbstractArtifactMetadata
{
    private String timestamp = null;

    private int buildNumber = 0;

    private static final String SNAPSHOT_VERSION_LOCAL_FILE = "version-local.txt";

    private static final String SNAPSHOT_VERSION_FILE = "version.txt";

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private static final String UTC_TIMESTAMP_PATTERN = "yyyyMMdd.HHmmss";

    private SnapshotArtifactMetadata( Artifact artifact, String filename )
    {
        super( artifact, filename );
    }

    public static SnapshotArtifactMetadata createLocalSnapshotMetadata( Artifact artifact )
    {
        return new SnapshotArtifactMetadata( artifact, SNAPSHOT_VERSION_LOCAL_FILE );
    }

    public static SnapshotArtifactMetadata createRemoteSnapshotMetadata( Artifact artifact )
    {
        return new SnapshotArtifactMetadata( artifact, SNAPSHOT_VERSION_FILE );
    }

    public void storeInLocalRepository( ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        try
        {
            if ( timestamp == null )
            {
                timestamp = getUtcDateFormatter().format( new Date() );
            }
            String path = new File( localRepository.getBasedir(), localRepository.pathOfMetadata( this ) ).getPath();
            FileUtils.fileWrite( path, getVersion() );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to retrieve metadata", e );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to retrieve metadata", e );
        }
    }

    public String getVersion()
    {
        String version = artifact.getVersion();
        if ( version != null )
        {
            version = StringUtils.replace( version, "SNAPSHOT", timestamp );
        }
        else
        {
            version = timestamp;
        }
        return version + "-" + buildNumber;
    }

    public void retrieveFromRemoteRepository( ArtifactRepository remoteRepository, WagonManager wagonManager )
        throws ArtifactMetadataRetrievalException
    {
        try
        {
            File destination = File.createTempFile( "maven-artifact", null );
            destination.deleteOnExit();

            try
            {
                wagonManager.getArtifactMetadata( this, remoteRepository, destination );
            }
            catch ( ResourceDoesNotExistException e )
            {
                // this just means that there is no snapshot version file, so we keep timestamp = null, build = 0
                return;
            }

            String version = FileUtils.fileRead( destination );

            int index = version.lastIndexOf( "-" );
            timestamp = version.substring( 0, index );
            buildNumber = Integer.valueOf( version.substring( index + 1 ) ).intValue();
            index = version.indexOf( "-" );
            if ( index >= 0 )
            {
                // ignore starting version part, will be prepended later
                timestamp = timestamp.substring( index + 1 );
            }
        }
        catch ( TransferFailedException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to retrieve metadata", e );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to retrieve metadata", e );
        }
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public static DateFormat getUtcDateFormatter()
    {
        DateFormat utcDateFormatter = new SimpleDateFormat( UTC_TIMESTAMP_PATTERN );
        utcDateFormatter.setTimeZone( UTC_TIME_ZONE );
        return utcDateFormatter;
    }

    public void update()
    {
        this.buildNumber++;
        timestamp = getUtcDateFormatter().format( new Date() );
    }
}
