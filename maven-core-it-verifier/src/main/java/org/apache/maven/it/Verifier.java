package org.apache.maven.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class Verifier
{
    private String basedir;

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
    }

    private void verifyExpectedResult( String line )
        throws VerificationException
    {
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
            File expectedFile = new File( basedir, line );

            if ( !expectedFile.exists() )
            {
                throw new VerificationException( "Expected file was not found: " + line );
            }
        }
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
