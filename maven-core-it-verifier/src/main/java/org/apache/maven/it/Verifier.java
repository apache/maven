package org.apache.maven.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Iterator;
import java.util.Map;

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

    public static String interpolate( String text, Map namespace )
    {
        Iterator keys = namespace.keySet().iterator();

        while ( keys.hasNext() )
        {
            String key = keys.next().toString();

            Object obj = namespace.get( key );

            String value = obj.toString();

            text = replace( text, "${" + key + "}", value );

            if ( key.indexOf( " " ) == -1 )
            {
                text = replace( text, "$" + key, value );
            }
        }
        return text;
    }

    public void verify()
        throws VerificationException
    {
        Properties properties = new Properties();

        try
        {
            properties.load( new FileInputStream( new File( System.getProperty( "user.home" ), "maven.properties" ) ) );

            for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();

                properties.setProperty( key, interpolate( properties.getProperty( key ), System.getProperties() ) );
            }

        }
        catch ( IOException e )
        {
            throw new VerificationException( "Can't find the maven.properties file! Verification can't proceed!" );
        }

        mavenRepoLocal = properties.getProperty( "maven.repo.local" );

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
                if ( line.indexOf( ":" ) > 0 ) //windows
                {
                    expectedFile = new File( line );
                }
                else
                {
                    expectedFile = new File( basedir, line );
                }
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
