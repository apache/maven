package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

public class MavenIT0031Test
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0031Test()                                                                                                                                    
        throws InvalidVersionSpecificationException                                                                                                             
    {                                                                                                                                                           
        super( "[,2.1-SNAPSHOT)" );                                                                                                                             
    }            

    /**
     * Test usage of plugins.xml mapping file on the repository to resolve plugin artifactId from it's prefix using the
     * pluginGroups in the provided settings.xml.
     */
    public void testit0031()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0031" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--settings settings.xml" );
        verifier.setCliOptions( cliOptions );
        Properties systemProperties = new Properties();
        systemProperties.put( "model", "src/main/mdo/test.mdo" );
        systemProperties.put( "version", "1.0.0" );
        verifier.setSystemProperties( systemProperties );
        Properties verifierProperties = new Properties();
        verifierProperties.put( "failOnErrorOutput", "false" );
        verifier.setVerifierProperties( verifierProperties );
        verifier.executeGoal( "modello:java" );
        verifier.assertFilePresent( "target/generated-sources/modello/org/apache/maven/it/it0031/Root.java" );
        // don't verify error free log
        verifier.resetStreams();

    }
}
