/* Created on Sep 20, 2004 */
package org.apache.maven.tools.repoclean.digest;

import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author jdcasey
 */
public class Digestor
{

    public static final String ROLE = Digestor.class.getName();

    public static final String MD5 = "MD5";

    public static final String SHA = "SHA";

    public void createArtifactDigest( File artifactFile, File digestFile, String algorithm )
        throws DigestException
    {
        if ( artifactFile == null || !artifactFile.exists() )
        {
            throw new DigestException( "Cannot generate digest for missing file: " + artifactFile );
        }

        byte[] digestData = generateArtifactDigest( artifactFile, algorithm );

        try
        {
            writeDigestFile( digestFile, digestData );
        }
        catch ( IOException e )
        {
            throw new DigestException( "Cannot write digest to file: \'" + digestFile + "\'", e );
        }
    }

    public boolean verifyArtifactDigest( File artifactFile, File digestFile, String algorithm )
        throws DigestException
    {
        if ( digestFile == null || !digestFile.exists() || artifactFile == null || !artifactFile.exists() )
        {
            return false;
        }

        byte[] generatedDigest = generateArtifactDigest( artifactFile, algorithm );

        InputStream in = null;
        try
        {
            in = new FileInputStream( artifactFile );

            int digestLen = generatedDigest.length;
            int currentIdx = 0;

            boolean matched = true;

            int read = -1;
            while ( ( read = in.read() ) > -1 )
            {
                if ( currentIdx >= digestLen || read != generatedDigest[currentIdx] )
                {
                    return false;
                }
            }
        }
        catch ( IOException e )
        {
            throw new DigestException( "Cannot verify digest for artifact file: \'" + artifactFile
                + "\' against digest file: \'" + digestFile + "\' using algorithm: \'" + algorithm + "\'", e );
        }
        finally
        {
            IOUtil.close( in );
        }

        return true;
    }

    public byte[] generateArtifactDigest( File artifactFile, String algorithm )
        throws DigestException
    {
        if ( artifactFile == null || !artifactFile.exists() )
        {
            throw new DigestException( "Cannot generate digest for missing file: " + artifactFile );
        }

        MessageDigest digest = null;
        try
        {
            digest = MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new DigestException( "Cannot load digest algoritm provider.", e );
        }

        InputStream in = null;
        try
        {
            in = new BufferedInputStream( new FileInputStream( artifactFile ) );

            byte[] buffer = new byte[16];
            int read = -1;
            while ( ( read = in.read( buffer ) ) > -1 )
            {
                digest.update( buffer, 0, read );
            }
        }
        catch ( IOException e )
        {
            throw new DigestException( "Error reading artifact data from: \'" + artifactFile + "\'", e );
        }
        finally
        {
            IOUtil.close( in );
        }

        return digest.digest();
    }

    private void writeDigestFile( File digestFile, byte[] digestData )
        throws IOException
    {
        Writer out = null;
        try
        {
            out = new FileWriter( digestFile );
            for ( int i = 0; i < digestData.length; i++ )
            {
                String t = Integer.toHexString( digestData[i] & 0xff );

                if ( t.length() == 1 )
                {
                    t = "0" + t;
                }

                out.write( t );
            }
        }
        finally
        {
            IOUtil.close( out );
        }
    }

}