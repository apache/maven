/* Created on Sep 20, 2004 */
package org.apache.maven.tools.repoclean.digest;

import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        byte[] digestData = generateArtifactDigest( artifactFile, algorithm );

        try
        {
            writeDigestFile( digestFile, digestData );
        }
        catch ( IOException e )
        {
            throw new ArtifactDigestException( "Cannot write digest to file: \'" + digestFile + "\'", e );
        }
    }

    public boolean verifyArtifactDigest( File artifactFile, File digestFile, String algorithm )
        throws ArtifactDigestException
    {
        if(artifactFile.exists() && digestFile.exists())
        {
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
                    if(currentIdx >= digestLen || read != generatedDigest[currentIdx])
                    {
                        return false;
                    }
                }
            }
            catch ( IOException e )
            {
                throw new ArtifactDigestException("Cannot verify digest for artifact file: \'" + artifactFile + "\' against digest file: \'" + digestFile + "\' using algorithm: \'" + algorithm + "\'", e);
            }
            finally
            {
                IOUtil.close( in );
            }
            
        }
        else
        {
            return false;
        }
        
        return true;
    }
    
    private byte[] generateArtifactDigest( File artifactFile, String algorithm ) throws ArtifactDigestException
    {
        MessageDigest digest = null;
        try
        {
            digest = MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new ArtifactDigestException( "Cannot load digest algoritm provider.", e );
        }

        InputStream in = null;
        try
        {
            in = new BufferedInputStream( new FileInputStream( artifactFile ) );

            byte[] buffer = new byte[16];
            int read = -1;
            while ( ( read = in.read( buffer ) ) > -1 )
            {
                digest.update(buffer, 0, read);
            }
        }
        catch ( IOException e )
        {
            throw new ArtifactDigestException( "Error reading artifact data from: \'" + artifactFile + "\'", e );
        }
        finally
        {
            IOUtil.close( in );
        }

        return digest.digest();
    }

    private void writeDigestFile( File digestFile, byte[] digestData ) throws IOException
    {
        OutputStream out = null;
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

}