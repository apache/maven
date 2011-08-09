package org.apache.maven.artifact.router;

import java.util.List;

public interface ArtifactRouter
{

    public static final String SESSION_KEY = ArtifactRouter.class.getName();

    MirrorRoute selectSingleMirror( final String canonicalUrl );

    List<MirrorRoute> getAllMirrors( final String canonicalUrl );

    GroupRoute getGroup( String groupId );

    boolean contains( final MirrorRoute o );

    List<MirrorRoute> getMirrors();

    boolean containsMirrorOf( final String canonicalUrl );

}