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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * Contains the information stored for a snapshot.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class SnapshotArtifactMetadata
    extends AbstractVersionArtifactMetadata
{
    private String timestamp;

    private int buildNumber;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private static final String UTC_TIMESTAMP_PATTERN = "yyyyMMdd.HHmmss";

    public SnapshotArtifactMetadata( Artifact artifact )
    {
        super( artifact );
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
        return artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." + SNAPSHOT_VERSION_FILE;
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

    protected void setContent( String content )
    {
        Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( content.trim() );
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

    public int getBuildNumber()
    {
        return buildNumber;
    }

    public static DateFormat getUtcDateFormatter()
    {
        DateFormat utcDateFormatter = new SimpleDateFormat( UTC_TIMESTAMP_PATTERN );
        utcDateFormatter.setTimeZone( UTC_TIME_ZONE );
        return utcDateFormatter;
    }

    public int compareTo( Object o )
    {
        SnapshotArtifactMetadata metadata = (SnapshotArtifactMetadata) o;

        if ( buildNumber > metadata.buildNumber )
        {
            return 1;
        }
        else if ( timestamp == null )
        {
            if ( metadata.timestamp == null )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else if ( metadata.timestamp == null )
        {
            return 1;
        }
        else
        {
            return timestamp.compareTo( metadata.timestamp );
        }
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
            return fileTimestamp.compareTo( timestamp ) < 0;
        }
        return false;
    }

    public String toString()
    {
        return "snapshot information for " + artifact.getArtifactId() + " " + artifact.getBaseVersion();
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return true;
    }

    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }
}
