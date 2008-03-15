package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources folder along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources folder. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng3372DirectInvocationOfPlugins
    extends TestCase
{

    public void testitMNG3372()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testBaseDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3372-directInvocationOfPlugins/direct-using-prefix" );
        File plugin = new File( testBaseDir, "plugin" );
        File project = new File( testBaseDir, "project" );
        File settingsFile = new File( testBaseDir, "settings.xml" );

        Verifier verifier = new Verifier( plugin.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3372", "mng3372-maven-plugin", "1", "jar" );

        verifier.executeGoals( Arrays.asList( new String[]{ "clean", "install" } ) );

        verifier = new Verifier( project.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-s" );
        cliOptions.add( settingsFile.getAbsolutePath() );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "mng3372:test" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }

    public void testDependencyTreeInvocation()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testBaseDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3372-directInvocationOfPlugins/dependency-tree" );
        File settingsFile = new File( testBaseDir, "settings.xml" );

        Verifier verifier = new Verifier( testBaseDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-s" );
        cliOptions.add( settingsFile.getAbsolutePath() );
        cliOptions.add( "-U" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "dependency:tree" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }
}
