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
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.codehaus.plexus.util.FileUtils;

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

    private int buildNumber = 1;

    private static final String SNAPSHOT_VERSION_LOCAL_FILE = "version-local.txt";

    private static final String SNAPSHOT_VERSION_FILE = "version.txt";

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private SnapshotArtifactMetadata( Artifact artifact, String filename )
    {
        super( artifact, filename );
    }

    public static SnapshotArtifactMetadata createLocalSnapshotMetadata( Artifact artifact )
    {
        return new SnapshotArtifactMetadata( artifact, SNAPSHOT_VERSION_LOCAL_FILE );
    }

    public void storeInLocalRepository( ArtifactRepository localRepository )
        throws IOException, ArtifactPathFormatException
    {
        FileUtils.fileWrite( localRepository.getBasedir() + "/" + localRepository.pathOfMetadata( this ),
                             getTimestamp() + "-" + buildNumber );
    }

    public String getTimestamp()
    {
        if ( timestamp == null )
        {
            timestamp = getUtcDateFormatter().format( new Date() );
        }
        return timestamp;
    }

    public DateFormat getUtcDateFormatter()
    {
        DateFormat utcDateFormatter = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        utcDateFormatter.setTimeZone( UTC_TIME_ZONE );
        return utcDateFormatter;
    }
}
