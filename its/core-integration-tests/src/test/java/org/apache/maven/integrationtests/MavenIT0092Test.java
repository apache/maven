package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;                                                                            

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class MavenIT0092Test
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0092Test()                                                                                                                                 
        throws InvalidVersionSpecificationException                                                                                                          
    {                                                                                                                                                        
        super( "[,2.1-SNAPSHOT)" );                                                                                                                          
    }    
    
    /**
     * Test that legacy repositories with legacy snapshots download correctly.
     */
    public void testit0092()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0092" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--settings settings.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar" );
        verifier.executeGoal( "compile" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

