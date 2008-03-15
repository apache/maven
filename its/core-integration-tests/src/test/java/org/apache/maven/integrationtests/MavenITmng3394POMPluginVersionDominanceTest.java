package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Check that plugin versions in the POM obey the correct order
 * of precedence. Specifically, that mojos in the default lifecycle
 * bindings can find plugin versions in the pluginManagement section
 * when the build/plugins section is missing that plugin, and that
 * plugin versions in build/plugins override those in build/pluginManagement.
 */
public class MavenITmng3394POMPluginVersionDominanceTest
    extends AbstractMavenIntegrationTestCase
{

	private static final String BASEDIR_PREFIX = "/mng-3394-pomPluginVersionDominance/";

    public MavenITmng3394POMPluginVersionDominanceTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3394a ()
        throws Exception
    {
        //testShouldUsePluginVersionFromPluginMgmtForLifecycleMojoWhenNotInBuildPlugins 
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR_PREFIX + "lifecycleMojoVersionInPluginMgmt" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.setCliOptions( Collections.singletonList( "-X" ) );

        verifier.executeGoal( "install" );

        /*
         * This is the simplest way to check a build
         * succeeded. It is also the simplest way to create
         * an IT test: make the build pass when the test
         * should pass, and make the build fail when the
         * test should fail. There are other methods
         * supported by the verifier. They can be seen here:
         * http://maven.apache.org/shared/maven-verifier/apidocs/index.html
         */
        verifier.verifyErrorFreeLog();

        List logFile = verifier.loadFile( new File( testDir, "log.txt" ), false );

        boolean foundSiteBeta5 = false;
        for ( Iterator it = logFile.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( "maven-site-plugin:2.0-beta-5" ) > -1 )
            {
                foundSiteBeta5 = true;
                break;
            }
        }

        /*
         * Reset the streams before executing the verifier
         * again.
         */
        verifier.resetStreams();

        assertTrue( "No reference to maven-site-plugin, version 2.0-beta-5 found in build log.", foundSiteBeta5 );
    }

    public void testitMNG3394b ()
        throws Exception
    {
        //testShouldPreferPluginVersionFromBuildPluginsOverThatInPluginMgmt
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR_PREFIX + "preferBuildPluginOverPluginMgmt" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.setAutoclean( false );
        verifier.executeGoal( "clean" );

        /*
         * This is the simplest way to check a build
         * succeeded. It is also the simplest way to create
         * an IT test: make the build pass when the test
         * should pass, and make the build fail when the
         * test should fail. There are other methods
         * supported by the verifier. They can be seen here:
         * http://maven.apache.org/shared/maven-verifier/apidocs/index.html
         */
        verifier.verifyErrorFreeLog();

        /*
         * Reset the streams before executing the verifier
         * again.
         */
        verifier.resetStreams();
    }
}
