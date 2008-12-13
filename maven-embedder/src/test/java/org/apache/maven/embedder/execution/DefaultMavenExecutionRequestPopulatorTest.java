package org.apache.maven.embedder.execution;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultMavenExecutionRequestPopulatorTest
    extends PlexusTestCase
{
    private MavenExecutionRequestPopulator populator;

    private WagonManager wagonManager;

    public void setUp()
        throws Exception
    {
        super.setUp();

        populator = (MavenExecutionRequestPopulator) lookup( MavenExecutionRequestPopulator.class );
        wagonManager = (WagonManager) lookup( WagonManager.class );
    }

    public void testWagonManagerOfflineFlagIsPopulatedFromSettings()
        throws MavenEmbedderException
    {
        MavenExecutionRequest req = new DefaultMavenExecutionRequest().setOffline( true );

        assertTrue( wagonManager.isOnline() );

        populator.populateDefaults( req, new DefaultConfiguration() );

        assertFalse( wagonManager.isOnline() );
    }

}
