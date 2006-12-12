package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0024Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test usage of &lt;executions/&gt; inside a plugin rather than &lt;goals/&gt;
     * that are directly inside th plugin.
     */
    public void testit0024()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0024" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "generate-sources" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0024/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

