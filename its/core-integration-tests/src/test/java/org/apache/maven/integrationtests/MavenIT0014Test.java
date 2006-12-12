package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0014Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test POM configuration by settings the -source and -target for the
     * compiler to 1.4
     */
    public void testit0014()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0014" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0014/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

