package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0003Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Builds upon it0001: we add a jar installation step. We delete the JAR
     * from the local repository to make sure it is there post build.
     */
    public void testit0003()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0003 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0003", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it0003", "1.0", "jar" );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0003/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0003/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-core-it0003-1.0.jar" );
        verifier.assertFilePresent( "target/maven-core-it0003-1.0.jar!/it0003.properties" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it0003", "1.0", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

