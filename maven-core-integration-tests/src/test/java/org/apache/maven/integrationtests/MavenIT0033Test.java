package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0033Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test an EAR generation
     */
    public void testit0033()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0033 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0033", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-core-it00xx-1.0.ear" );
        verifier.assertFilePresent( "target/maven-core-it00xx-1.0.ear!/META-INF/application.xml" );
        verifier.assertFilePresent( "target/maven-core-it00xx-1.0.ear!/META-INF/appserver-application.xml" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

