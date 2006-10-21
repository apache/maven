package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0029Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test for pluginManagement injection of plugin configuration.
     */
    public void testit0029()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0029 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0029", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-core-it0029", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-core-it0029-child", "1.0-SNAPSHOT", "jar" );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "child-project/target/classes/org/apache/maven/it0001/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

