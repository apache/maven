package org.apache.maven.integrationtests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng3652UserAgentHeader
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3652UserAgentHeader()
        throws InvalidVersionSpecificationException
    {
        // TODO: fix support for this in 2.1-SNAPSHOT
        // 2.0.10+
        //super( "(2.0.9,)" );
        super( "(2.0.9,2.1-SNAPSHOT)" );
    }

    /**
     * Test that the user agent header is configured in the wagon manager.
     */
    public void testmng3652()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652-user-agent" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        int port = ( Math.abs( new Random().nextInt() ) % 2048 ) + 1024;

        Server s = new Server( port );
        Thread t = new Thread( s );
        t.setDaemon( true );
        t.start();
        
        verifier = new Verifier( projectDir.getAbsolutePath() );
        
        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        String userAgent = s.userAgent;
        assertNotNull( userAgent );
        
        File touchFile = new File( projectDir, "target/touch.txt" );
        assertTrue( touchFile.exists() );
        
        List lines = verifier.loadFile( touchFile, false );

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = ((String) lines.get( 0 )).substring( 0, 3 );
        String javaVersion = (String) lines.get( 1 );
        String os = (String) lines.get( 2 ) + " " + (String) lines.get( 3 );
        String artifactVersion = (String) lines.get( 4 );

        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );

        // test webdav
        cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "-DtestProtocol=dav:http" );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        userAgent = s.userAgent;
        assertNotNull( userAgent );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );

        // test settings with no config

        cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "--settings" );
        cliOptions.add( "settings-no-config.xml" );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        userAgent = s.userAgent;
        assertNotNull( userAgent );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );

        // test settings with config

        cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "--settings" );
        cliOptions.add( "settings.xml" );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        userAgent = s.userAgent;
        assertNotNull( userAgent );
        assertEquals( "Maven Fu", userAgent );        

        t.interrupt();
    }

    private static final class Server
        implements Runnable
    {
        
        private final int port;
        
        private String userAgent;

        private Server( int port )
        {
            this.port = port;
        }

        public void run()
        {
            ServerSocket ssock = null;
            try
            {
                try
                {
                    ssock = new ServerSocket( port );
                    ssock.setSoTimeout( 2000 );
                }
                catch ( IOException e )
                {
                    Logger.getLogger( Server.class.getName() ).log( Level.SEVERE, "", e );
                    return;
                }

                while ( !Thread.currentThread().isInterrupted() )
                {
                    Socket sock = null;
                    BufferedReader r = null;
                    try
                    {
                        try
                        {
                            sock = ssock.accept();
                        }
                        catch ( SocketTimeoutException e )
                        {
                            continue;
                        }

                        r = new BufferedReader( new InputStreamReader( sock.getInputStream() ) );

                        String line = null;
                        int count = 0;
                        while ( ( line = r.readLine() ) != null )
                        {
                            Logger.getLogger( Server.class.getName() ).fine( line );
                            if ( line.startsWith( "User-Agent:" ) )
                            {
                                userAgent = line.replaceAll( "User-Agent:\\s+", "" );
                                
                                break;
                            }
                            count++;
                        }
                    }
                    catch ( IOException e )
                    {
                        Logger.getLogger( Server.class.getName() ).log( Level.SEVERE, "", e );
                    }
                    finally
                    {
                        if ( r != null )
                        {
                            try
                            {
                                r.close();
                            }
                            catch ( IOException e )
                            {
                            }
                        }
                        if ( sock != null )
                        {
                            try
                            {
                                sock.close();
                            }
                            catch ( IOException e )
                            {
                            }
                        }
                    }
                }
            }
            finally
            {
                if ( ssock != null )
                {
                    try
                    {
                        ssock.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }
    }
}
