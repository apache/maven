package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0085Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that system-scoped dependencies get resolved with system scope
     * when they are resolved transitively via another (non-system)
     * dependency. Inherited scope should not apply in the case of
     * system-scoped dependencies, no matter where they are.
     */
    public void testit0085()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0085" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFileNotPresent( "war/target/war-1.0/WEB-INF/lib/pom.xml" );
        verifier.assertFileNotPresent( "war/target/war-1.0/WEB-INF/lib/it0085-dep-1.0.jar" );
        verifier.assertFilePresent( "war/target/war-1.0/WEB-INF/lib/junit-3.8.1.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

