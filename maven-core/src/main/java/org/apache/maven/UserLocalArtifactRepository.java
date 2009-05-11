package org.apache.maven;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;

public class UserLocalArtifactRepository
    extends LocalArtifactRepository
{
    private ArtifactRepository localRepository;
    
    public UserLocalArtifactRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        setLayout( localRepository.getLayout() );
    }
    
    @Override
    public Artifact find( Artifact artifact )
    {
        File artifactFile = new File( localRepository.getBasedir(), pathOf( artifact ) );
        
        // We need to set the file here or the resolver will fail with an NPE, not fully equipped to deal
        // with multiple local repository implementations yet.
        artifact.setFile( artifactFile );
        
        if( artifactFile.exists() )
        {            
            artifact.setResolved( true );            
        }
                
        return artifact;
    }

    @Override
    public String getId()
    {
        return localRepository.getId();
    }
    
    @Override
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return localRepository.pathOfLocalRepositoryMetadata( metadata, repository );
    }
    
    @Override
    public String pathOf( Artifact artifact )
    {
        return localRepository.pathOf( artifact );
    }
    
    @Override
    public boolean isAuthoritative()
    {
        return false;
    }

    @Override
    public boolean hasLocalMetadata()
    {
        return true;
    }
}
