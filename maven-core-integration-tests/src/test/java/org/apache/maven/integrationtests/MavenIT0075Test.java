package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MavenIT0075Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Verify that direct invocation of a mojo from the command line still
     * results in the processing of modules included via profiles.
     */
    public void testit0075()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0075 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0075", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        FileUtils.deleteFile( new File( basedir, "sub1/target/maven-core-it0075-sub1-1.0.jar" ) );
        FileUtils.deleteFile( new File( basedir, "sub2/target/maven-core-it0075-sub2-1.0.jar" ) );
        List cliOptions = new ArrayList();
        cliOptions.add( "-Dactivate=anything" );
        verifier.setCliOptions( cliOptions );
        List goals = Arrays.asList( new String[]{"help:active-profiles", "package", "eclipse:eclipse", "clean:clean"} );
        verifier.executeGoals( goals );
        verifier.assertFileNotPresent( "sub1/target/maven-core-it0075-sub1-1.0.jar" );
        verifier.assertFileNotPresent( "sub2/target/maven-core-it0075-sub2-1.0.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

