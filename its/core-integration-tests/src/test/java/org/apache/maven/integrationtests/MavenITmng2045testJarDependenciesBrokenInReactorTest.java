package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Simple IT test invoking maven in a reactor with 2 projects.
 * First project produced a test-jar, which is required to 
 * compile second project. 
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author mikko.koponen@ri.fi
 */
public class MavenITmng2045testJarDependenciesBrokenInReactorTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2045testJarDependenciesBrokenInReactorTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.7,)" ); // 2.0.8+
    }

    public void testitMNG2045()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2045-testJarDependenciesBrokenInReactor" );
        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "testing", "mng-2045-test", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "testing", "first-project", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "testing", "second-project", "1.0-SNAPSHOT", "jar" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}
