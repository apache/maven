package org.apache.maven.integrationtests;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Integration test to check MNG-3284 - that explicitly defined plugins are used, not the one that is cached.
 */
public class MavenITmng3284UsingCachedPluginsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3284UsingCachedPluginsTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); 
    }
    
    public void testitMNG3284()
        throws Exception
    {

        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng3284-usingCachedPlugins" );

        Verifier verifier;

        /*
         * Build Mojo v1
         */
        verifier = new Verifier( new File( testDir.getAbsolutePath(), "mojo" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        /*
         * Build Mojo v2
         */
        verifier = new Verifier( new File( testDir.getAbsolutePath(), "mojo2" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        /*
         * Run the simple build
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        List lines = verifier.loadFile( testDir.getAbsolutePath(), "log.txt", false );
        int foundVersionOne = 0;
        int foundVersionTwo = 0;
        for ( Iterator i = lines.iterator(); i.hasNext(); )
        {

            String line = (String) i.next();
            if ( line.indexOf( "USING VERSION 1" ) != -1 )
                foundVersionOne++;
            if ( line.indexOf( "USING VERSION 2" ) != -1 )
                foundVersionTwo++;
        }

        verifier.resetStreams();

        Assert.assertEquals( "Should be using plugin version 1 only once.", 1,foundVersionOne );
        Assert.assertEquals( "Should be using plugin version 2 only once.", 1,foundVersionTwo );

    }
}
