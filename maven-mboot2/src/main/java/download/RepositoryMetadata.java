package download;

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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import util.AbstractReader;
import util.StringUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        RepositoryMetadata metadata = new RepositoryMetadata();
        new Reader( metadata ).parse( file );
        return metadata;
    }

    public void write( File file )
        throws IOException
    {
        new Writer( this ).write( file );
    }

    public String constructVersion( String baseVersion )
    {
        if ( snapshotTimestamp != null && !localCopy )
        {
            baseVersion = StringUtils.replace( baseVersion, "SNAPSHOT", snapshotTimestamp + "-" + snapshotBuildNumber );
        }
        return baseVersion;
    }

    static class Reader
        extends AbstractReader
    {
        private boolean insideVersioning;

        private StringBuffer bodyText = new StringBuffer();

        private boolean insideSnapshot;

        private final RepositoryMetadata metadata;

        private boolean insideVersions;

        public Reader( RepositoryMetadata metadata )
        {
            this.metadata = metadata;
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
                else if ( insideSnapshot && "snapshot".equals( rawName ) )
                {
                    if ( "buildNumber".equals( rawName ) )
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
                    insideSnapshot = false;
                }
                else if ( insideVersions && "versions".equals( rawName ) )
                {
                    if ( "version".equals( rawName ) )
                    {
                        metadata.addVersion( getBodyText() );
                    }
                    insideVersions = false;
                }
                else if ( "latest".equals( rawName ) )
                {
                    metadata.setLatestVersion( getBodyText() );
                }
                else if ( "release".equals( rawName ) )
                {
                    metadata.setReleaseVersion( getBodyText() );
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
            PrintWriter w = new PrintWriter( new FileWriter( file ) );

            try
            {
                w.println( "<metadata>" );
                writeLine( w, "  ", "groupId", metadata.getGroupId() );
                writeLine( w, "  ", "artifactId", metadata.getArtifactId() );
                writeLine( w, "  ", "version", metadata.getVersion() );
                w.println( "  <versioning>" );
                writeLine( w, "    ", "latest", metadata.getLatestVersion() );
                writeLine( w, "    ", "release", metadata.getReleaseVersion() );
                w.println( "    <snapshot>" );
                writeLine( w, "      ", "localCopy", String.valueOf( metadata.isLocalCopy() ) );
                writeLine( w, "      ", "buildNumber", String.valueOf( metadata.getSnapshotBuildNumber() ) );
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
