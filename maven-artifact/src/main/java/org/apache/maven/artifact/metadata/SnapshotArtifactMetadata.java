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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains the information stored for a snapshot.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class SnapshotArtifactMetadata
    extends AbstractArtifactMetadata
    implements Comparable
{
    private String timestamp = null;

    private int buildNumber = 0;

    private static final String SNAPSHOT_VERSION_FILE = "version.txt";

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private static final String UTC_TIMESTAMP_PATTERN = "yyyyMMdd.HHmmss";

    private long lastModified = 0;

    private static final Pattern VERSION_FILE_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$" );

    public SnapshotArtifactMetadata( Artifact artifact )
    {
        super( artifact, artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." + SNAPSHOT_VERSION_FILE );
    }

    public static SnapshotArtifactMetadata readFromLocalRepository( Artifact artifact,
                                                                    ArtifactRepository localRepository )
        throws ArtifactPathFormatException, IOException
    {
        SnapshotArtifactMetadata metadata = new SnapshotArtifactMetadata( artifact );
        File f = metadata.getLocalRepositoryLocation( localRepository );
        if ( f.exists() )
        {
            metadata.readFromFile( f );
        }
        return metadata;
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
            String path = getLocalRepositoryLocation( localRepository ).getPath();
            File file = new File( path );
            // TODO: this should be centralised before the resolution of the artifact
            file.getParentFile().mkdirs();
            FileUtils.fileWrite( path, constructVersion() );
            lastModified = file.lastModified();
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

    private File getLocalRepositoryLocation( ArtifactRepository localRepository )
        throws ArtifactPathFormatException
    {
        return new File( localRepository.getBasedir(), localRepository.pathOfMetadata( this ) );
    }

    public String constructVersion()
    {
        String version = artifact.getBaseVersion();
        if ( timestamp != null && buildNumber > 0 )
        {
            String newVersion = timestamp + "-" + buildNumber;
            if ( version != null )
            {
                version = StringUtils.replace( version, "SNAPSHOT", newVersion );
            }
            else
            {
                version = newVersion;
            }
        }
        return version;
    }

    /**
     * Retrieve the metadata from the remote repository into the local repository.
     *
     * @param remoteRepository the remote repository
     * @param wagonManager     the wagon manager to use to retrieve the metadata
     */
    public static SnapshotArtifactMetadata retrieveFromRemoteRepository( Artifact artifact,
                                                                         ArtifactRepository remoteRepository,
                                                                         WagonManager wagonManager )
        throws ArtifactMetadataRetrievalException
    {
        SnapshotArtifactMetadata snapshotMetadata = new SnapshotArtifactMetadata( artifact );

        try
        {
            File destination = File.createTempFile( "maven-artifact", null );
            destination.deleteOnExit();

            try
            {
                wagonManager.getArtifactMetadata( snapshotMetadata, remoteRepository, destination );

                snapshotMetadata.readFromFile( destination );
            }
            catch ( ResourceDoesNotExistException e )
            {
                // this just means that there is no snapshot version file, so we keep timestamp = null, build = 0
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

        return snapshotMetadata;
    }

    private void readFromFile( File file )
        throws IOException
    {
        String version = FileUtils.fileRead( file );
        lastModified = file.lastModified();

        Matcher matcher = VERSION_FILE_PATTERN.matcher( version );
        if ( matcher.matches() )
        {
            timestamp = matcher.group( 2 );
            buildNumber = Integer.valueOf( matcher.group( 3 ) ).intValue();
        }
        else
        {
            timestamp = null;
            buildNumber = 0;
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


    public int compareTo( Object o )
    {
        SnapshotArtifactMetadata metadata = (SnapshotArtifactMetadata) o;

        // TODO: probably shouldn't test timestamp - except that it may be used do differentiate for a build number of 0
        //  in the local repository. check, then remove from here and just compare the build numbers

        if ( buildNumber > metadata.buildNumber )
        {
            return 1;
        }
        else if ( timestamp == null )
        {
            return -1;
        }
        else
        {
            return timestamp.compareTo( metadata.timestamp );
        }
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public boolean newerThanFile( File file )
    {
        long fileTime = file.lastModified();

        // previous behaviour - compare based on timestamp of file
        //  problem was that version.txt is often updated even if the remote snapshot was not
        // return ( lastModified > fileTime );

        // Compare to timestamp
        if ( timestamp != null )
        {
            String fileTimestamp = getUtcDateFormatter().format( new Date( fileTime ) );
            return ( fileTimestamp.compareTo( timestamp ) < 0 );
        }
        return false;
    }
}
