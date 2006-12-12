package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0060Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test aggregation of list configuration items when using
     * 'combine.children=append' attribute. Specifically, merge the list of
     * excludes for the testCompile mojo.
     */
    public void testit0060()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0060" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.assertFilePresent( "subproject/target/classes/org/apache/maven/it0060/Person.class" );
        verifier.assertFilePresent( "subproject/target/test-classes/org/apache/maven/it0060/PersonTest.class" );
        verifier.assertFileNotPresent( "subproject/target/test-classes/org/apache/maven/it0060/PersonTwoTest.class" );
        verifier.assertFileNotPresent( "subproject/target/test-classes/org/apache/maven/it0060/PersonThreeTest.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

