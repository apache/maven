/* Created on Sep 20, 2004 */
package org.apache.maven.tools.repoclean.digest;

import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author jdcasey
 */
public class ArtifactDigestor
{

    public static final String ROLE = ArtifactDigestor.class.getName();

    public static final String MD5 = "MD5";

    public static final String SHA = "SHA";

    public void createArtifactDigest( File artifactFile, File digestFile, String algorithm )
        throws ArtifactDigestException
    {
        byte[] data = null;
        try
        {
            data = readArtifactFile( artifactFile );
        }
        catch ( IOException e )
        {
            throw new ArtifactDigestException( "Error reading artifact data from: \'" + artifactFile + "\'", e );
        }

        MessageDigest digest = null;
        try
        {
            digest = MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new ArtifactDigestException( "Cannot load digest algoritm provider.", e );
        }

        digest.update( data );
        byte[] digestData = digest.digest();

        try
        {
            writeDigestFile( digestFile, digestData );
        }
        catch ( IOException e )
        {
            throw new ArtifactDigestException( "Cannot write digest to file: \'" + digestFile + "\'", e );
        }
    }

    private void writeDigestFile( File digestFile, byte[] digestData ) throws IOException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream( digestFile );
            out.write( digestData );
            out.flush();
        }
        finally
        {
            IOUtil.close( out );
        }
    }

    private byte[] readArtifactFile( File artifactFile ) throws IOException
    {
        BufferedInputStream in = null;
        try
        {
            in = new BufferedInputStream( new FileInputStream( artifactFile ) );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[16];
            int read = -1;
            while ( ( read = in.read( buffer ) ) > -1 )
            {
                baos.write( buffer, 0, read );
            }

            return baos.toByteArray();
        }
        finally
        {
            IOUtil.close( in );
        }
    }
}