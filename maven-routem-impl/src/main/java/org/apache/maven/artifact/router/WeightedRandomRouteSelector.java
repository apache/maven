package org.apache.maven.artifact.router;

import java.util.Collection;

import org.apache.maven.artifact.router.ArtifactRouteSelector;
import org.apache.maven.artifact.router.MirrorRoute;
import org.codehaus.plexus.component.annotations.Component;

@Component( role=ArtifactRouteSelector.class, hint="weighted-random" )
public class WeightedRandomRouteSelector
    implements ArtifactRouteSelector
{

    public MirrorRoute select( Collection<MirrorRoute> routes )
    {
        // FIXME: Implement this!
        throw new UnsupportedOperationException();
    }

}
