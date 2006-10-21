package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0063Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test the use of a system scoped dependency to tools.jar.
     */
    public void testit0063()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0063 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0063", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0001/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0001/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-core-it0063-1.0.jar" );
        verifier.assertFilePresent( "target/maven-core-it0063-1.0.jar!/it0001.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

