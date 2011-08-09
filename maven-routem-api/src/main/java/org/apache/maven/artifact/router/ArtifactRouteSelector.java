package org.apache.maven.artifact.router;

import java.util.Collection;

public interface ArtifactRouteSelector
{
    
    MirrorRoute select( Collection<MirrorRoute> routes );

}
