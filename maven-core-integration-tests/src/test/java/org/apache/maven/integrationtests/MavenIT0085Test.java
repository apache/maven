package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0085Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
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
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0085 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0085", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFileNotPresent( "war/target/it0085-war-1.0/WEB-INF/lib/pom.xml" );
        verifier.assertFileNotPresent( "war/target/it0085-war-1.0/WEB-INF/lib/it0085-dep-1.0.jar" );
        verifier.assertFilePresent( "war/target/it0085-war-1.0/WEB-INF/lib/junit-3.8.1.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

