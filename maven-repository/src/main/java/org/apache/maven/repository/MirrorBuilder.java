package org.apache.maven.repository;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;

public interface MirrorBuilder
{
    ArtifactRepository getMirror( ArtifactRepository repository );
    
    List<ArtifactRepository> getMirrors( List<ArtifactRepository> repositories );
    
    void addMirror( String id, String mirrorOf, String url );
    
    void clearMirrors();    
}
