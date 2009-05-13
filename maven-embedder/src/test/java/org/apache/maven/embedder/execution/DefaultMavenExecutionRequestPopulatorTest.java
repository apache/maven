package org.apache.maven.embedder.execution;

import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultMavenExecutionRequestPopulatorTest
    extends PlexusTestCase
{
    private MavenExecutionRequestPopulator populator;
    
    public void setUp()
        throws Exception
    {
        super.setUp();

        populator = (MavenExecutionRequestPopulator) lookup( MavenExecutionRequestPopulator.class );
    }
    
    @Override
    protected void tearDown() throws Exception {
            populator = null;
            super.tearDown();
    }

    public void testWagonManagerOfflineFlagIsPopulatedFromSettings()
        throws MavenEmbedderException
    {
        MavenExecutionRequest req = new DefaultMavenExecutionRequest().setOffline( true );

        populator.populateDefaults( req, new DefaultConfiguration() );
    }
}
