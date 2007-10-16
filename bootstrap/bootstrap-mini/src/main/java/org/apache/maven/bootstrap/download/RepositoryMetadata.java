package org.apache.maven.bootstrap.download;

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

import org.apache.maven.bootstrap.util.AbstractReader;
import org.apache.maven.bootstrap.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/**
 * I/O for repository metadata.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class RepositoryMetadata
{
    private String snapshotTimestamp;

    private int snapshotBuildNumber;

    private String releaseVersion;

    private String groupId;

    private String artifactId;

    private String version;

    private List versions = new ArrayList();

    private String latestVersion;

    private boolean localCopy;

    private String lastUpdated;

    public String getSnapshotTimestamp()
    {
        return snapshotTimestamp;
    }

    public void setSnapshotTimestamp( String snapshotTimestamp )
    {
        this.snapshotTimestamp = snapshotTimestamp;
    }

    public int getSnapshotBuildNumber()
    {
        return snapshotBuildNumber;
    }

    public void setSnapshotBuildNumber( int snapshotBuildNumber )
    {
        this.snapshotBuildNumber = snapshotBuildNumber;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public List getVersions()
    {
        return versions;
    }

    public void setVersions( List versions )
    {
        this.versions = versions;
    }

    public String getReleaseVersion()
    {
        return releaseVersion;
    }

    public void setReleaseVersion( String releaseVersion )
    {
        this.releaseVersion = releaseVersion;
    }

    public String getLatestVersion()
    {
        return latestVersion;
    }

    public void setLatestVersion( String latestVersion )
    {
        this.latestVersion = latestVersion;
    }

    public void addVersion( String version )
    {
        versions.add( version );
    }

    public boolean isLocalCopy()
    {
        return localCopy;
    }

    public void setLocalCopy( boolean localCopy )
    {
        this.localCopy = localCopy;
    }

    public static RepositoryMetadata read( File file )
        throws IOException, ParserConfigurationException, SAXException
    {
        return new Reader().parseMetadata( file );
    }

    public void write( File file )
        throws IOException
    {
        new Writer( this ).write( file );
    }

    public String constructVersion( String baseVersion )
    {
        if ( snapshotTimestamp != null )
        {
            baseVersion = StringUtils.replace( baseVersion, "SNAPSHOT", snapshotTimestamp + "-" + snapshotBuildNumber );
        }
        return baseVersion;
    }

    public long getLastUpdatedUtc()
    {
        TimeZone timezone = TimeZone.getTimeZone( "UTC" );
        DateFormat fmt = new SimpleDateFormat( "yyyyMMddHHmmss" );
        fmt.setTimeZone( timezone );

        try
        {
            return fmt.parse( lastUpdated ).getTime();
        }
        catch ( ParseException e )
        {
            return -1;
        }
    }

    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    public String getLastUpdated()
    {
        return lastUpdated;
    }

    static class Reader
        extends AbstractReader
    {
        private boolean insideVersioning;

        private StringBuffer bodyText = new StringBuffer();

        private boolean insideSnapshot;

        private final RepositoryMetadata metadata = new RepositoryMetadata();

        private boolean insideVersions;

        public RepositoryMetadata parseMetadata( File metadataFile )
            throws IOException, ParserConfigurationException, SAXException
        {
            parse( metadataFile );
            return metadata;
        }

        public void startElement( String uri, String localName, String rawName, Attributes attributes )
        {
            if ( insideVersioning )
            {
                if ( "snapshot".equals( rawName ) )
                {
                    insideSnapshot = true;
                }
                else if ( "versions".equals( rawName ) )
                {
                    insideVersions = true;
                }
            }
            else
            {
                // root element
                if ( "versioning".equals( rawName ) )
                {
                    insideVersioning = true;
                }
            }
        }

        public void characters( char buffer[], int start, int length )
        {
            bodyText.append( buffer, start, length );
        }

        private String getBodyText()
        {
            return bodyText.toString().trim();
        }

        public void endElement( String uri, String localName, String rawName )
            throws SAXException
        {
            if ( insideVersioning )
            {
                if ( "versioning".equals( rawName ) )
                {
                    insideVersioning = false;
                }
                else if ( insideSnapshot )
                {
                    if ( "snapshot".equals( rawName ) )
                    {
                        insideSnapshot = false;
                    }
                    else if ( "buildNumber".equals( rawName ) )
                    {
                        try
                        {
                            metadata.setSnapshotBuildNumber( Integer.valueOf( getBodyText() ).intValue() );
                        }
                        catch ( NumberFormatException e )
                        {
                            // Ignore
                        }
                    }
                    else if ( "timestamp".equals( rawName ) )
                    {
                        metadata.setSnapshotTimestamp( getBodyText() );
                    }
                    else if ( "localCopy".equals( rawName ) )
                    {
                        metadata.setLocalCopy( Boolean.valueOf( getBodyText() ).booleanValue() );
                    }
                }
                else if ( insideVersions )
                {
                    if ( "versions".equals( rawName ) )
                    {
                        insideVersions = false;
                    }
                    else if ( "version".equals( rawName ) )
                    {
                        metadata.addVersion( getBodyText() );
                    }
                }
                else if ( "latest".equals( rawName ) )
                {
                    metadata.setLatestVersion( getBodyText() );
                }
                else if ( "release".equals( rawName ) )
                {
                    metadata.setReleaseVersion( getBodyText() );
                }
                else if ( "lastUpdated".equals( rawName ) )
                {
                    metadata.setLastUpdated( getBodyText() );
                }
            }
            else if ( "groupId".equals( rawName ) )
            {
                metadata.setGroupId( getBodyText() );
            }
            else if ( "artifactId".equals( rawName ) )
            {
                metadata.setArtifactId( getBodyText() );
            }
            else if ( "version".equals( rawName ) )
            {
                metadata.setVersion( getBodyText() );
            }
            bodyText = new StringBuffer();
        }

    }

    static class Writer
    {
        private final RepositoryMetadata metadata;

        public Writer( RepositoryMetadata metadata )
        {
            this.metadata = metadata;
        }

        public void write( File file )
            throws IOException
        {
            PrintWriter w = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" ) );

            try
            {
                w.println( "<metadata>" );
                writeLine( w, "  ", "groupId", metadata.getGroupId() );
                writeLine( w, "  ", "artifactId", metadata.getArtifactId() );
                writeLine( w, "  ", "version", metadata.getVersion() );
                w.println( "  <versioning>" );
                writeLine( w, "    ", "latest", metadata.getLatestVersion() );
                writeLine( w, "    ", "release", metadata.getReleaseVersion() );
                writeLine( w, "    ", "lastUpdated", metadata.getLastUpdated() );
                w.println( "    <snapshot>" );
                if ( metadata.isLocalCopy() )
                {
                    writeLine( w, "      ", "localCopy", "true" );
                }
                if ( metadata.getSnapshotBuildNumber() > 0 )
                {
                    writeLine( w, "      ", "buildNumber", String.valueOf( metadata.getSnapshotBuildNumber() ) );
                }
                writeLine( w, "      ", "timestamp", metadata.getSnapshotTimestamp() );
                w.println( "    </snapshot>" );
                w.println( "    <versions>" );
                for ( Iterator i = metadata.getVersions().iterator(); i.hasNext(); )
                {
                    writeLine( w, "      ", "version", (String) i.next() );
                }
                w.println( "    </versions>" );
                w.println( "  </versioning>" );
                w.println( "</metadata>" );
            }
            finally
            {
                w.close();
            }
        }

        private void writeLine( PrintWriter w, String indent, String tag, String content )
        {
            if ( content != null )
            {
                w.println( indent + ( "<" + tag + ">" + content + "</" + tag + ">" ) );
            }
        }

    }
}
