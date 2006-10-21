package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0032Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Tests that a specified Maven version requirement that is lower doesn't cause any problems
     */
    public void testit0032()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0032 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0032", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0032/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0032/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-core-it0032-1.0.jar" );
        verifier.assertFilePresent( "target/maven-core-it0032-1.0.jar!/it0032.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

