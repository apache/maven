package org.apache.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;

public class DelegatingLocalArtifactRepository
    extends DefaultArtifactRepository
{
    private List<LocalArtifactRepository> localRepositories;        
    
    public void addLocalArtifactRepository( LocalArtifactRepository localRepository )
    {
        if ( localRepositories == null )
        {
            localRepositories = new ArrayList<LocalArtifactRepository>();
        }
        
        localRepositories.add( localRepository );
    }
    
    @Override
    public Artifact find( Artifact artifact )
    {
        for( LocalArtifactRepository repository : localRepositories )
        {
            artifact = repository.find( artifact );
            
            if ( artifact.isResolved() )
            {
                return artifact;
            }
        }
        
        return artifact;
    }
    
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        for( LocalArtifactRepository localRepository : localRepositories )
        {
            if ( localRepository.hasLocalMetadata() )
            {
                return localRepository.pathOfLocalRepositoryMetadata( metadata, localRepository );
            }
        }
        
        return null;
    }    
    
    // This ID is necessary of the metadata lookup doesn't work correctly.
    public String getId()
    {
        return "local";
    }
    
    @Override
    public String pathOf( Artifact artifact )
    {
        for( LocalArtifactRepository localRepository : localRepositories )
        {
            if( localRepository.hasLocalMetadata() )
            {
                String path = localRepository.pathOf( artifact );
                
                return path;
            }
        }
        
        return null;
    }
}
