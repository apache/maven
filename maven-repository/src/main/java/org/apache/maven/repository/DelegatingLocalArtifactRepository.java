package org.apache.maven.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * Delegating local artifact repository chains the reactor, IDE workspace
 * and user local repository.
 */
public class DelegatingLocalArtifactRepository
    extends DefaultArtifactRepository
{
    private LocalArtifactRepository buildReactor;

    private LocalArtifactRepository ideWorkspace;

    private ArtifactRepository userLocalArtifactRepository;

    public DelegatingLocalArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.userLocalArtifactRepository = artifactRepository;
    }

    public void setBuildReactor( LocalArtifactRepository localRepository )
    {
        this.buildReactor = localRepository;
    }

    public void setIdeWorkspace( LocalArtifactRepository localRepository )
    {
        this.ideWorkspace = localRepository;
    }

    public LocalArtifactRepository getIdeWorspace()
    {
        return ideWorkspace;
    }

    @Override
    public Artifact find( Artifact artifact )
    {
        if ( !artifact.isRelease() && buildReactor != null )
        {
            artifact = buildReactor.find( artifact );
        }

        if ( !artifact.isResolved() && ideWorkspace != null )
        {
            artifact = ideWorkspace.find( artifact );
        }

        if ( !artifact.isResolved() )
        {
            artifact = userLocalArtifactRepository.find( artifact );
        }

        return artifact;
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return userLocalArtifactRepository.pathOfLocalRepositoryMetadata( metadata, userLocalArtifactRepository );
    }

    public String getId()
    {
        return userLocalArtifactRepository.getId();
    }

    @Override
    public String pathOf( Artifact artifact )
    {
        return userLocalArtifactRepository.pathOf( artifact );
    }

    @Override
    public String getBasedir()
    {
        return userLocalArtifactRepository.getBasedir();
    }

    @Override
    public ArtifactRepositoryLayout getLayout()
    {
        return userLocalArtifactRepository.getLayout();
    }

    @Override
    public ArtifactRepositoryPolicy getReleases()
    {
        return userLocalArtifactRepository.getReleases();
    }

    @Override
    public ArtifactRepositoryPolicy getSnapshots()
    {
        return userLocalArtifactRepository.getSnapshots();
    }

    @Override
    public String getKey()
    {
        return userLocalArtifactRepository.getKey();
    }

    @Override
    public String getUrl()
    {
        return userLocalArtifactRepository.getUrl();
    }

}
