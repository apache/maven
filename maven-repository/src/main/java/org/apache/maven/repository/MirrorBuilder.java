package org.apache.maven.repository;

import org.apache.maven.artifact.repository.ArtifactRepository;

public interface MirrorBuilder
{
    ArtifactRepository getMirror( ArtifactRepository repository );

    void addMirror( String id, String mirrorOf, String url );
    
    void clearMirrors();    
}
