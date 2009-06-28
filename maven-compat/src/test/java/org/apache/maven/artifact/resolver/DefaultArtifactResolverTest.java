package org.apache.maven.artifact.resolver;

import org.codehaus.plexus.PlexusTestCase;

public class DefaultArtifactResolverTest
    extends PlexusTestCase
{

    public void testLookup()
        throws Exception
    {
        ArtifactResolver resolver = lookup( ArtifactResolver.class, "default" );
    }
}
