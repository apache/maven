package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class MavenITmng2277AggregatorAndResolutionPluginsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2277AggregatorAndResolutionPluginsTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.7,)" ); // 2.0.8+
    }

    public void testitMNG2277 ()
        throws Exception
    {
   
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng2277aggregatorPlugins" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng2277", "parent", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2277", "test", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2277", "assembly", "1.0", "jar" );

        /*
         * The Command Line Options (CLI) are passed to the
         * verifier as a list. This is handy for things like
         * redefining the local repository if needed. In
         * this case, we use the -N flag so that Maven won't
         * recurse. We are only installing the parent pom to
         * the local repo here.
         */
        List cliOptions = new ArrayList();
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-all:aggregator-dependencies" );

        verifier.verifyErrorFreeLog();
    }
}
