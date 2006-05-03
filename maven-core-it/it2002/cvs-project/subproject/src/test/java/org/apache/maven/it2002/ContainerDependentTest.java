package org.apache.maven.it2002;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.codehaus.plexus.PlexusTestCase;

public class ContainerDependentTest extends PlexusTestCase
{
    
    public void testOne() throws Exception
    {
        ArtifactResolver resolver = (ArtifactResolver) lookup( ArtifactResolver.ROLE );
        
        assertNotNull( resolver );
        
        Thing thing = (Thing) lookup( Thing.ROLE );
        
        assertNotNull( thing );
    }

}
