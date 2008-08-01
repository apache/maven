package org.apache.maven.integrationtests;

import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test that changes to a project's POM file reference (MavenProject.setFile(..))
 * doesn't affect the basedir of the project instance for using that project's classes directory
 * in the classpath of another project's build...this happens when both projects are
 * built in the same reactor, and one project depends on the other.
 * 
 * @author jdcasey
 */
public class MavenITmng3693PomFileBasedirChangeTest
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG3693 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3693-pomFileBasedirChange" );
        
        File pluginDir = new File( testDir, "maven-mng3693-plugin" );
        File projectsDir = new File( testDir, "projects" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        String depPath = verifier.getArtifactPath( "org.apache.maven.its.mng3693", "dep", "1", "pom" );

        File dep = new File( depPath );
        dep = dep.getParentFile().getParentFile();

        // remove the dependency from the local repository.
        FileUtils.deleteDirectory( dep );

        verifier = new Verifier( projectsDir.getAbsolutePath() );
        
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}
