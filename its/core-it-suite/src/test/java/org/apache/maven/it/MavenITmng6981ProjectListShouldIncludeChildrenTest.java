package org.apache.maven.it;

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.List;

public class MavenITmng6981ProjectListShouldIncludeChildrenTest
        extends AbstractMavenIntegrationTestCase
{

    private static final String RESOURCE_PATH = "/mng-6981-pl-should-include-children";

    public MavenITmng6981ProjectListShouldIncludeChildrenTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    public void testProjectListShouldIncludeChildrenByDefault()
            throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_PATH );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.addCliOption( "-pl" );
        verifier.addCliOption( ":module-a" );
        verifier.executeGoal( "compile" );
        verifier.verifyTextInLog( "Building module-a-1 1.0" );
    }

    /**
     * Since --pl's behavior is changed, make sure the alternative for building a pom without its children still works.
     */
    public void testFileSwitchAllowsExcludeOfChildren()
            throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_PATH );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.addCliOption( "-f" );
        verifier.addCliOption( "module-a" );
        verifier.addCliOption( "--non-recursive" );
        verifier.setLogFileName( "log-non-recursive.txt" );
        verifier.executeGoal( "compile" );
        verifyTextNotInLog( verifier, "Building module-a-1 1.0" );
    }

    /**
     * Throws an exception if the text <strong>is</strong> present in the log.
     *
     * @param verifier the verifier to use
     * @param text the text to assert present
     * @throws VerificationException if text is not found in log
     */
    private void verifyTextNotInLog( Verifier verifier, String text )
            throws VerificationException
    {
        List<String> lines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );

        for ( String line : lines )
        {
            if ( Verifier.stripAnsi( line ).contains( text ) )
            {
                throw new VerificationException( "Text found in log: " + text );
            }
        }
    }
}
