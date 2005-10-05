package org.apache.maven.project.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AttachedArtifact
    extends DefaultArtifact
{

    private final Artifact parent;

    public AttachedArtifact ( Artifact parent, String type, String classifier )
    {
        super( parent.getGroupId(), parent.getArtifactId(), parent.getVersionRange(), parent.getScope(), type, classifier, parent.getArtifactHandler(), parent.isOptional() );
        this.parent = parent;
        
        if ( type == null || type.trim().length() < 1 )
        {
            throw new InvalidArtifactRTException( getGroupId(), getArtifactId(), getVersion(), type, "Attached artifacts must specify a type." );
        }
        
        if ( classifier == null || classifier.trim().length() < 1 )
        {
            throw new InvalidArtifactRTException( getGroupId(), getArtifactId(), getVersion(), type, "Attached artifacts must specify a classifier." );
        }
    }

    public ArtifactHandler getArtifactHandler()
    {
        return parent.getArtifactHandler();
    }

    public String getArtifactId()
    {
        return parent.getArtifactId();
    }

    public List getAvailableVersions()
    {
        return parent.getAvailableVersions();
    }

    public String getBaseVersion()
    {
        return parent.getBaseVersion();
    }

    public ArtifactFilter getDependencyFilter()
    {
        return parent.getDependencyFilter();
    }

    public List getDependencyTrail()
    {
        return parent.getDependencyTrail();
    }

    public String getDownloadUrl()
    {
        return parent.getDownloadUrl();
    }

    public String getGroupId()
    {
        return parent.getGroupId();
    }

    public ArtifactRepository getRepository()
    {
        return parent.getRepository();
    }

    public String getScope()
    {
        return parent.getScope();
    }

    public String getType()
    {
        return parent.getType();
    }

    public String getVersion()
    {
        return parent.getVersion();
    }

    public VersionRange getVersionRange()
    {
        return parent.getVersionRange();
    }

    public boolean isOptional()
    {
        return parent.isOptional();
    }

    public boolean isRelease()
    {
        return parent.isRelease();
    }

    public boolean isSnapshot()
    {
        return parent.isSnapshot();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        // ignore. The parent artifact will handle metadata.
    }

    public Collection getMetadataList()
    {
        return Collections.EMPTY_LIST;
    }

}
