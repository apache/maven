package org.apache.maven.artifact.metadata;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.transform.LatestArtifactTransformation;

import java.io.File;

public class LatestArtifactMetadata
    extends AbstractVersionArtifactMetadata
{
    
    private String version;

    public LatestArtifactMetadata( Artifact artifact )
    {
        super( artifact, artifact.getArtifactId() + "-" + LatestArtifactTransformation.LATEST_VERSION + "." + SNAPSHOT_VERSION_FILE );
    }

    public String constructVersion()
    {
        return version;
    }

    public int compareTo( Object o )
    {
        LatestArtifactMetadata metadata = (LatestArtifactMetadata) o;

        // TODO: we need some more complicated version comparison
        if ( version == null )
        {
            if ( metadata.version == null )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            if ( metadata.version == null )
            {
                return 1;
            }
            else
            {
                return version.compareTo( metadata.version );
            }
        }
    }

    public boolean newerThanFile( File file )
    {
        long fileTime = file.lastModified();

        return ( lastModified > fileTime );
    }

    public String toString()
    {
        return "latest-version information for " + artifact.getArtifactId();
    }

    protected void setContent( String content )
    {
        this.version = content.trim();
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getBaseVersion()
    {
        return LatestArtifactTransformation.LATEST_VERSION;
    }
    
    public boolean storedInArtifactDirectory()
    {
        return false;
    }

}
