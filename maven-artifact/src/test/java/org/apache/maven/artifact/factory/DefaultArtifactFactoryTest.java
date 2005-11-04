package org.apache.maven.artifact.factory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultArtifactFactoryTest
    extends PlexusTestCase
{
    
    public void testPropagationOfSystemScopeRegardlessOfInheritedScope() throws Exception
    {
        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        
        Artifact artifact = factory.createDependencyArtifact( "test-grp", "test-artifact", VersionRange.createFromVersion("1.0"), "type", null, "system", "provided" );
        Artifact artifact2 = factory.createDependencyArtifact( "test-grp", "test-artifact-2", VersionRange.createFromVersion("1.0"), "type", null, "system", "test" );
        Artifact artifact3 = factory.createDependencyArtifact( "test-grp", "test-artifact-3", VersionRange.createFromVersion("1.0"), "type", null, "system", "runtime" );
        Artifact artifact4 = factory.createDependencyArtifact( "test-grp", "test-artifact-4", VersionRange.createFromVersion("1.0"), "type", null, "system", "compile" );
        
        // this one should never happen in practice...
        Artifact artifact5 = factory.createDependencyArtifact( "test-grp", "test-artifact-5", VersionRange.createFromVersion("1.0"), "type", null, "system", "system" );
        
        assertEquals( "system", artifact.getScope() );
        assertEquals( "system", artifact2.getScope() );
        assertEquals( "system", artifact3.getScope() );
        assertEquals( "system", artifact4.getScope() );
        assertEquals( "system", artifact5.getScope() );
    }

}
