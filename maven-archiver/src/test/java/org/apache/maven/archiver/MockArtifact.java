package org.apache.maven.archiver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @todo move to maven-artifact-test
 */
class MockArtifact
    implements Artifact
{
    private String groupId;

    private String artifactId;

    private String version;

    private File file;

    private String scope;

    private String type;

    private String classifier;

    private String baseVersion;

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String string )
    {
        this.version = string;
    }

    public String getScope()
    {
        return scope;
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean hasClassifier()
    {
        return classifier != null;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public String getBaseVersion()
    {
        return baseVersion;
    }

    public void setBaseVersion( String string )
    {
        this.baseVersion = string;
    }

    public String getId()
    {
        // TODO
        return null;
    }

    public String getDependencyConflictId()
    {
        // TODO
        return null;
    }

    public void addMetadata( ArtifactMetadata artifactMetadata )
    {
        // TODO
    }

    public Collection getMetadataList()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setRepository( ArtifactRepository artifactRepository )
    {
        //To change body of implemented methods use File | Settings | File Templates.
        // TODO
    }

    public ArtifactRepository getRepository()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateVersion( String string, ArtifactRepository artifactRepository )
    {
        //To change body of implemented methods use File | Settings | File Templates.
        // TODO
    }

    public String getDownloadUrl()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDownloadUrl( String string )
    {
        //To change body of implemented methods use File | Settings | File Templates.
        // TODO
    }

    public ArtifactFilter getDependencyFilter()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArtifactHandler getArtifactHandler()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getDependencyTrail()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDependencyTrail( List list )
    {
        //To change body of implemented methods use File | Settings | File Templates.
        // TODO
    }

    public VersionRange getVersionRange()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setVersionRange( VersionRange versionRange )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void selectVersion( String string )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSnapshot()
    {
        // TODO
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setResolved( boolean b )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isResolved()
    {
        // TODO
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setResolvedVersion( String string )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRelease()
    {
        // TODO
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setRelease( boolean b )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getAvailableVersions()
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAvailableVersions( List list )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isOptional()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
        // TODO
    }

    public void setOptional( boolean b )
    {
        // TODO
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
        // TODO
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public void setScope( String string )
    {
         this.scope = string;
    }

    public int compareTo( Object o )
    {
        // TODO
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
