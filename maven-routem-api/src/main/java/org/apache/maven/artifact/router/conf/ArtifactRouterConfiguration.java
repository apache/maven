package org.apache.maven.artifact.router.conf;

import java.io.File;

public interface ArtifactRouterConfiguration
{

    public static final RouterSource CANONICAL_GROUP_SOURCE =
                    new RouterSource( "default", "http://repository.apache.org/router/groups.json" );

    public static final RouterSource CANONICAL_MIRROR_SOURCE =
                    new RouterSource( "default", "http://repository.apache.org/router/mirrors.json" );

    public static final String NO_DISCOVERY_STRATEGIES = "none";

    public static final String WEIGHTED_RANDOM_STRATEGY = "weighted-random";

    RouterSource getGroupSource();
    
    RouterSource getMirrorSource();

    boolean isDisabled();

    String getDiscoveryStrategy();
    
    String getSelectionStrategy();

    File getRoutesFile();

    boolean isOffline();

    boolean isClear();

    boolean isUpdate();

    RouterSource getDefaultGroupSource();

    RouterSource getDefaultMirrorSource();

}