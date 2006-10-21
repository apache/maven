package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0002Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Builds upon it0001: we add the download of a dependency. We delete
     * the JAR from the local repository and make sure it is there post build.
     */
    public void testit0002()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0002 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0002", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.0", "jar" );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0002/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0002/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-core-it0002-1.0.jar" );
        verifier.assertFilePresent( "target/maven-core-it0002-1.0.jar!/it0002.properties" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.0", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

