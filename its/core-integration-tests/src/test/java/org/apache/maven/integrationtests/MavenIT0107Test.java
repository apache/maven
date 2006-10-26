package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * #it0107 requires a snapshot version of maven-plugin-plugin, which indicates it doesn't belong here                                  
 */
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
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0107" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "core-it:param-implementation" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0107 PASS" );
    }
}

