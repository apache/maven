package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.IOUtil;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public class MavenITmng2883LegacyRepoOfflineTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2883LegacyRepoOfflineTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.4,)" );
    }

    public void testParentUnresolvable()
        throws Exception
    {
        String testName = "parent";
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-2883-legacy-repo-offline/"
                                                                                 + testName );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        File settings = writeSettings( testDir );
        List cliOptions = new ArrayList();

        // used to inject the remote repository
        cliOptions.add( "-s" );
        cliOptions.add( settings.getAbsolutePath() );

        verifier.setCliOptions( cliOptions );

        // execute once just to make sure this test works at all!
        try
        {
            // this will ensure that all relevant plugins are present.
            verifier.executeGoal( "initialize" );
        }
        catch ( VerificationException e )
        {
            throw new VerificationException( "Build should succeed the first time through when NOT in offline mode!", e );
        }

        // the centerpiece of these tests!
        cliOptions.add( "-o" );

        verifier.setCliOptions( cliOptions );
        verifier.setAutoclean( false );

        // clear out the parent POM if it's in the local repository.
        verifier.deleteArtifact( "org.apache.maven.its.mng2883", "parent", "1.0-SNAPSHOT", "pom" );

        try
        {
            verifier.executeGoal( "initialize" );

            fail( "Build should fail with unresolvable parent POM." );
        }
        catch ( VerificationException e )
        {
        }

        List missingMessages = new ArrayList();
        missingMessages.add( "System is offline." );
        missingMessages.add( "org.apache.maven.its.mng2883:parent:pom:1.0-SNAPSHOT" );

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            for ( Iterator messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.indexOf( message ) > -1 )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( Iterator it = missingMessages.iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                if ( buffer.length() < 1 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( '\'' ).append( message ).append( '\'' );
            }

            fail( buffer.toString() );
        }
    }

    public void testDependencyUnresolvable()
        throws Exception
    {
        String testName = "dependency";
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-2883-legacy-repo-offline/"
                                                                                 + testName );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        File settings = writeSettings( testDir );

        // used to inject the remote repository
        cliOptions.add( "-s" );
        cliOptions.add( settings.getAbsolutePath() );

        verifier.setCliOptions( cliOptions );

        // execute once just to make sure this test works at all!
        try
        {
            // this will ensure that all relevant plugins are present.
            verifier.executeGoal( "compile" );
        }
        catch ( VerificationException e )
        {
            throw new VerificationException( "Build should succeed the first time through when NOT in offline mode!", e );
        }

        // the centerpiece of these tests!
        cliOptions.add( "-o" );

        verifier.setCliOptions( cliOptions );

        // clear out the dependency if it's in the local repository.
        verifier.deleteArtifact( "org.apache.maven.its.mng2883", "dep", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2883", "dep", "1.0-SNAPSHOT", "jar" );

        try
        {
            verifier.executeGoal( "compile" );

            fail( "Build should fail with unresolvable dependency artifact." );
        }
        catch ( VerificationException e )
        {
        }

        List missingMessages = new ArrayList();

        // FIXME: We need a more prominent diagnosis including system being in offline mode for 2.0.x.
        missingMessages.add( "offline mode." );
        missingMessages.add( "org.apache.maven.its.mng2883:dep:jar:1.0-SNAPSHOT" );

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            for ( Iterator messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.indexOf( message ) > -1 )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( Iterator it = missingMessages.iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                if ( buffer.length() < 1 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( '\'' ).append( message ).append( '\'' );
            }

            fail( buffer.toString() );
        }
    }

    public void testPluginUnresolvable()
        throws Exception
    {
        String testName = "plugin";
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-2883-legacy-repo-offline/"
                                                                                 + testName );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        // the centerpiece of these tests!
        cliOptions.add( "-o" );

        File settings = writeSettings( testDir );

        // used to inject the remote repository
        cliOptions.add( "-s" );
        cliOptions.add( settings.getAbsolutePath() );

        verifier.setCliOptions( cliOptions );

        // clear out the dependency if it's in the local repository.
        verifier.deleteArtifact( "org.apache.maven.its.mng2883", "plugin", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2883", "plugin", "1.0-SNAPSHOT", "jar" );

        try
        {
            verifier.executeGoal( "org.apache.maven.its.mng2883:plugin:1.0-SNAPSHOT:run" );

            fail( "Build should fail with unresolvable plugin artifact." );
        }
        catch ( VerificationException e )
        {
        }

        List missingMessages = new ArrayList();
        missingMessages.add( "System is offline." );
        missingMessages.add( "org.apache.maven.its.mng2883:plugin:pom:1.0-SNAPSHOT" );

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            for ( Iterator messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.indexOf( message ) > -1 )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( Iterator it = missingMessages.iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                if ( buffer.length() < 1 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( '\'' ).append( message ).append( '\'' );
            }

            fail( buffer.toString() );
        }
    }

    private File writeSettings( File testDir )
        throws IOException
    {
        File settingsIn = new File( testDir.getParentFile(), "settings.xml.in" );

        String settingsContent = null;
        Reader reader = null;
        try
        {
            reader = new FileReader( settingsIn );
            settingsContent = IOUtil.toString( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        settingsContent = StringUtils.replace( settingsContent,
                                               "@TESTDIR@",
                                               testDir.getAbsolutePath() );

        File settingsOut = new File( testDir, "settings.xml" );

        System.out.println( "Writing tets settings to: " + settingsOut );

        if ( settingsOut.exists() )
        {
            settingsOut.delete();
        }

        Writer writer = null;
        try
        {
            writer = new FileWriter( settingsOut );
            IOUtil.copy( settingsContent, writer );
        }
        finally
        {
            IOUtil.close( writer );
        }

        return settingsOut;
    }

}
