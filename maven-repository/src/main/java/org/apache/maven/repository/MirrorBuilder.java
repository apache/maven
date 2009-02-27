package org.apache.maven.repository;

import org.apache.maven.artifact.repository.ArtifactRepository;

public interface MirrorBuilder
{
    ArtifactRepository getMirror( ArtifactRepository originalRepository );

    void addMirror( String id, String mirrorOf, String url );
    
    void clearMirrors();
    
    // These need to go
    
    boolean isExternalRepo( ArtifactRepository originalRepository );
    
    boolean matchPattern( ArtifactRepository originalRepository, String pattern );
    
    ArtifactRepository getMirrorRepository( ArtifactRepository repository );    
}
