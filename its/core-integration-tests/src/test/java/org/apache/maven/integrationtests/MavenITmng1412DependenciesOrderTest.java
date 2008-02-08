package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test that dependencies order in classpath matches pom.xml.
 *
 * @author <a href="mailto:hboutemy@apache.org">Herve Boutemy</a>
 *
 */
public class MavenITmng1412DependenciesOrderTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng1412DependenciesOrderTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    public void testitMNG1412()
        throws Exception
    {
        // The testdir is computed from the location of this file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1412-DependenciesOrder" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );

        verifier.setCliOptions( cliOptions );

        verifier.executeGoal( "test" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }
}
