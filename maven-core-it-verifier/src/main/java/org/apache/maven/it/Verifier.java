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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.utils.DOMBuilder;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class Verifier
{
    private String basedir;

    private File homeDir;

    private String localRepo;

    public Verifier( String basedir, String homeDir )
    {
        this.basedir = basedir;
        this.homeDir = new File( homeDir );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void verify() throws VerificationException
    {
        try
        {
            retrieveLocalRepo();

            BufferedReader reader = new BufferedReader( new FileReader( new File( basedir, "expected-results.txt" ) ) );

            String line = "";

            while ( (line = reader.readLine()) != null )
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

    private void retrieveLocalRepo() throws Exception
    {
        localRepo = System.getProperty( "maven.repo.local" );
        if ( localRepo == null )
        {
            // parse ~/.m2/pom.xml for it...
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File pom = new File( homeDir, ".m2/pom.xml" );
            Document dom = builder.parse( pom );

            localRepo = XPathAPI.selectSingleNode( dom, "/project/local/repository/text()" ).getNodeValue();
        }
    }

    private void verifyExpectedResult( String line ) throws VerificationException
    {
        line = replace( line, "${localRepository}", localRepo );

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
        while ( (end = text.indexOf( repl, start )) != -1 )
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
    {
        Verifier verifier = new Verifier( args[0], args[1] );

        try
        {
            verifier.verify();
        }
        catch ( VerificationException e )
        {
            System.out.println( e.getMessage() );

            System.exit( 1 );
        }

        System.exit( 0 );
    }
}