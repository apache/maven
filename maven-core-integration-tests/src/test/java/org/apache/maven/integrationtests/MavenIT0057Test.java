package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0057Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Verify that scope == 'provided' dependencies are available to tests.
     */
    public void testit0057()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0057 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0057", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0001/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0001/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-core-it0057-1.0.jar" );
        verifier.assertFilePresent( "target/maven-core-it0057-1.0.jar!/it0001.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

