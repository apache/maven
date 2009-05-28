package org.apache.maven.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;

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

    // This ID is necessary of the metadata lookup doesn't work correctly.
    public String getId()
    {
        return "delegating";
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
}
