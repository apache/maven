package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0010Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Since the artifact resolution does not use the project builder, we must
     * ensure that the full hierarchy of all dependencies is resolved. This
     * includes the dependencies of the parent-pom's of dependencies. This test
     * will check this, by depending on classworlds, which is a dependency of
     * maven-component, which is the parent of maven-plugin, which is an
     * explicit dependency of this test.
     * # TODO: must correct the assumptions of this test
     */
    public void testit0010()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0010 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0010", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0010/PersonFinder.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

