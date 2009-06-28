package org.apache.maven.artifact.transform;

import java.util.List;

import org.apache.maven.repository.legacy.resolver.transform.ArtifactTransformationManager;
import org.apache.maven.repository.legacy.resolver.transform.LatestArtifactTransformation;
import org.apache.maven.repository.legacy.resolver.transform.ReleaseArtifactTransformation;
import org.apache.maven.repository.legacy.resolver.transform.SnapshotTransformation;
import org.codehaus.plexus.PlexusTestCase;

/** @author Jason van Zyl */
public class TransformationManagerTest
    extends PlexusTestCase
{
    public void testTransformationManager()
        throws Exception
    {
        ArtifactTransformationManager tm = (ArtifactTransformationManager) lookup( ArtifactTransformationManager.class );

        List tms = tm.getArtifactTransformations();

        assertEquals( 3, tms.size() );

        assertTrue( "We expected the release transformation and got " + tms.get(0), tms.get(0) instanceof ReleaseArtifactTransformation );

        assertTrue( "We expected the latest transformation and got " + tms.get(1), tms.get(1) instanceof LatestArtifactTransformation );

        assertTrue( "We expected the snapshot transformation and got " + tms.get(2), tms.get(2) instanceof SnapshotTransformation );
    }

}
