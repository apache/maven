package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.util.FileUtils;

import java.io.File;

public class MavenITmng3652UserAgentHeader
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3652UserAgentHeader()
        throws InvalidVersionSpecificationException
    {
        // 2.0.10+
        super( "(2.0.9,)" );
    }

    /**
     * Test that the user agent header is configured in the wagon manager.
     */
    public void testmng3652()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652-user-agent" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );
        verifier.executeGoal( "process-sources" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertEquals( "Apache Maven/" + System.getProperty( "maven.version" ), FileUtils.fileRead( new File( projectDir, "target/touch.txt" ) ) );
    }
}

