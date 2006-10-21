package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0030Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test for injection of dependencyManagement through parents of
     * dependency poms.
     */
    public void testit0030()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0030 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0030", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-core-it0030", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-core-it0030-child-hierarchy", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-core-it0030-child-project1", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-core-it0030-child-project2", "1.0-SNAPSHOT", "jar" );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "child-hierarchy/project2/target/classes/org/apache/maven/it0001/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

