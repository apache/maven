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
        // 2.0.10+
        super( "(2.0.9,)" );
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
        
        System.out.println( "\n\nHTTP Recording server started on port: " + port + "\n\n" );
        
        verifier = new Verifier( projectDir.getAbsolutePath() );
        
        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.resetStreams();
        
        t.interrupt();

        String userAgent = s.userAgent;
        assertNotNull( userAgent );
        
        File touchFile = new File( projectDir, "target/touch.txt" );
        assertTrue( touchFile.exists() );
        
        List lines = verifier.loadFile( touchFile, false );

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = (String) lines.get( 0 );
        String javaVersion = (String) lines.get( 1 );
        String os = (String) lines.get( 2 );

        String minorVersion = mavenVersion.substring( 0, 3 );
        
        System.out.println( "Found User-Agent of: \'" + userAgent + "\'" );
        
        assertTrue( "User-Agent:\n\n\'" + userAgent + "\'\n\nshould have a main value of:\n\n\'ApacheMavenArtifact/" + minorVersion + "\'", userAgent.startsWith( "ApacheMavenArtifact/" + minorVersion ) );
        assertTrue( "User-Agent:\n\n\'" + userAgent + "\'\n\nshould contain:\n\n\'Apache Maven " + mavenVersion + "\'", userAgent.indexOf( "Apache Maven " + mavenVersion ) > -1 );
        assertTrue( "User-Agent:\n\n\'" + userAgent + "\'\n\nshould contain:\n\n\'JDK " + javaVersion + "\'", userAgent.indexOf( "JDK " + javaVersion ) > -1 );
        assertTrue( "User-Agent:\n\n\'" + userAgent + "\'\n\nshould contain:\n\n\'" +os + "\'", userAgent.indexOf( os ) > -1 );
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
                            System.out.println( line );
                            if ( line.startsWith( "User-Agent:" ) )
                            {
                                userAgent = line.substring( "User-Agent: ".length() );
                                while( userAgent.startsWith( " " ) )
                                {
                                    userAgent = userAgent.substring( 1 );
                                }
                                
                                break;
                            }
                            count++;
                        }

                        System.out.println( "Read in " + count + " passes." );
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
