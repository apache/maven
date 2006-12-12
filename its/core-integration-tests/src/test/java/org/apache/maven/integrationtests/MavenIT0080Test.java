package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0080Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that depending on a WAR doesn't also get its dependencies
     * transitively.
     */
    public void testit0080()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0080" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "test-component-a/target/test-component-a-0.1.jar" );
        verifier.assertFilePresent( "test-component-b/target/test-component-b-0.1.war" );
        verifier.assertFilePresent(
            "test-component-b/target/test-component-b-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar" );
        verifier.assertFilePresent( "test-component-c/target/test-component-c-0.1.ear" );
        verifier.assertFilePresent( "test-component-c/target/test-component-c-0.1.ear!/test-component-b-0.1.war" );
        verifier.assertFilePresent( "test-component-c/target/test-component-c-0.1/test-component-b-0.1.war" );
        verifier.assertFileNotPresent( "test-component-c/target/test-component-c-0.1/test-component-a-0.1.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

