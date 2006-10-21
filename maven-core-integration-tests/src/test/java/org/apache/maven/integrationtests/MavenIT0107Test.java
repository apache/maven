package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0107Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Verify that default implementation of an implementation for a complex object works as
     * expected [MNG-2293]
     */
    public void testit0107()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0107 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0107", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "core-it:param-implementation" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

