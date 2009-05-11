package org.apache.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;

public abstract class LocalArtifactRepository
    extends DefaultArtifactRepository
{   
    public abstract Artifact find( Artifact artifact );
    
    /** 
     * If an artifact is found in this repository and this method returns true the search is over. This would
     * be the case if we look for artifacts in the reactor or a IDE workspace. We don't want to search any
     * further.
     * 
     * @return
     */
    // workspace or reactor
    public abstract boolean isAuthoritative();
    
    public abstract boolean hasLocalMetadata();    
}
