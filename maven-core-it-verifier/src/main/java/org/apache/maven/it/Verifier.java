package org.apache.maven.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class Verifier
{
    private String basedir;

    private String mavenRepoLocal;

    public Verifier( String basedir )
    {
        this.basedir = basedir;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void verify()
        throws VerificationException
    {
        Properties mavenProperties = new Properties();

        try
        {
            mavenProperties.load( new FileInputStream( new File( System.getProperty( "user.home" ), "build.properties" ) ) );
        }
        catch ( IOException e )
        {
            throw new VerificationException( "Can't find the build.properties file! Verification can't proceed!" );
        }

        mavenRepoLocal = mavenProperties.getProperty( "maven.repo.local" );

        try
        {
            BufferedReader reader = new BufferedReader( new FileReader( new File( basedir, "expected-results.txt" ) ) );

            String line = "";

            while ( ( line = reader.readLine() ) != null )
            {
                verifyExpectedResult( line );
            }
        }
        catch ( Exception e )
        {
            throw new VerificationException( e );
        }

        System.out.println( "-----------------------------------------------------------------------------------> OK" );        
    }

    private void verifyExpectedResult( String line )
        throws VerificationException
    {
        line = replace( line, "${maven.repo.local}", mavenRepoLocal );

        if ( line.indexOf( "!/" ) > 0 )
        {
            String urlString = "jar:file:" + line;

            try
            {
                URL url = new URL( urlString );

                InputStream is = url.openStream();

                if ( is == null )
                {
                    throw new VerificationException( "Expected JAR resource was not found: " + line );
                }
            }
            catch ( Exception e )
            {
                throw new VerificationException( "Expected JAR resource was not found: " + line );
            }
        }
        else
        {
            File expectedFile;

            if ( line.startsWith( "/" ) )
            {
                expectedFile = new File( line );
            }
            else
            {
                expectedFile = new File( basedir, line );
            }

            if ( !expectedFile.exists() )
            {
                throw new VerificationException( "Expected file was not found: " + expectedFile.getPath() );
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static String replaceOnce( String text, String repl, String with )
    {
        return replace( text, repl, with, 1 );
    }

    public static String replace( String text, String repl, String with )
    {
        return replace( text, repl, with, -1 );
    }

    public static String replace( String text, String repl, String with, int max )
    {
        if ( text == null || repl == null || with == null || repl.length() == 0 )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }


    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static void main( String args[] )
        throws VerificationException
    {
        Verifier verifier = new Verifier( args[0] );

        verifier.verify();
    }
}
