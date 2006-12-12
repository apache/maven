package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0007Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * We specify a parent in the POM and make sure that it is downloaded as
     * part of the process.
     */
    public void testit0007()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0007" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-plugin-parent", "2.0", "pom" );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0007/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0007/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0007-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0007-1.0.jar!/it0007.properties" );
        verifier.assertArtifactPresent( "org.apache.maven.plugins", "maven-plugin-parent", "2.0", "pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

