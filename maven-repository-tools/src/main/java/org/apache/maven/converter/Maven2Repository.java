package org.apache.maven.converter;

/*
 * LICENSE
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Iterator;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class Maven2Repository
    extends AbstractMavenRepository
{
    public Iterator getArtifactsByType( String type )
        throws Exception
    {
        throw new Exception( "Not implemented." );
    }

    public void installArtifact( File artifact, Model model )
        throws Exception
    {
        String type = model.getType();

        if ( type.equals( "jar" ) )
        {
            installJar( artifact, model );
        }
        else
        {
            throw new Exception( "This installer can only handle jars." );
        }
    }

    public String getPomForArtifact( String artifactPath )
        throws Exception
    {
        throw new Exception( "Not implemented" );
    }

    private void installJar( File artifact, Model model )
        throws Exception
    {
        String groupId = model.getGroupId();

        String dest = groupId.replace( '.', File.separatorChar );

        dest += File.separator + model.getArtifactId();

        dest += File.separator + model.getVersion();

        dest += File.separator + model.getArtifactId() + "-" + model.getVersion() + ".jar";

        File destFile = new File( getRepository(), dest );

        if ( !destFile.getParentFile().mkdirs() )
        {
            throw new Exception( "Could not make directories " + dest );
        }

        System.out.println( "Installing " + destFile.getAbsolutePath() );

        FileUtils.copyFile( artifact, destFile );

        installMD5( destFile );

        installPom( destFile, model );
    }

    private void installPom( File artifact, Model model )
        throws Exception
    {
        File pom = new File( artifact.getAbsolutePath() + ".pom" );

        System.out.println( "Installing " + pom );

        MavenXpp3Writer v4Writer = new MavenXpp3Writer();

        Writer output = new FileWriter( pom );

        v4Writer.write( output, model );

        output.close();

        installMD5( pom );
    }

    private void installMD5( File artifact )
        throws Exception
    {
        MessageDigest digester = MessageDigest.getInstance( "MD5" );

        if ( digester == null )
        {
            throw new Exception( "Could not get MD5 message digester." );
        }

        byte[] data = IOUtil.toByteArray( new FileInputStream( artifact ) );

        digester.digest( data );

        String sum = encode( digester.digest() );

        File md5 = new File( artifact.getAbsolutePath() + ".md5" );

        OutputStream output = new FileOutputStream( md5 );

        PrintWriter writer = new PrintWriter( output );

        writer.println( sum.getBytes() );

        writer.close();

        output.close();

        System.out.println( "Installing " + md5 );
    }

    protected String encode( byte[] binaryData )
        throws Exception
    {
        if ( binaryData.length != 16 )
        {
            throw new Exception( "Error in input data." );
        }

        String retValue = "";

        for ( int i = 0; i < 16; i++ )
        {
            String t = Integer.toHexString( binaryData[i] & 0xff );

            if ( t.length() == 1 )
            {
                retValue += ( "0" + t );
            }
            else
            {
                retValue += t;
            }
        }

        return retValue.trim();
    }
}
