package org.apache.maven.wagon.providers.ssh.external;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringInputStream;

/**
 * NOTE: Plexus will only pick this correctly if the Class package and name are the same as that in core. This is
 * because the core component descriptor is read, but the class is read from the latter JAR.
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="scpexe"
 */
public class ScpExternalWagon
    extends AbstractWagon
{
    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        InputData inputData = new InputData();

        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        inputData.setResource( resource );

        fillInputData( inputData );

        InputStream is = inputData.getInputStream();

        if ( is == null )
        {
            throw new TransferFailedException( getRepository().getUrl()
                + " - Could not open input stream for resource: '" + resource + "'" );
        }

        createParentDirectories( destination );

        getTransfer( inputData.getResource(), destination, is );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return false;
    }

    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        OutputData outputData = new OutputData();

        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        outputData.setResource( resource );

        fillOutputData( outputData );

        OutputStream os = outputData.getOutputStream();

        if ( os == null )
        {
            throw new TransferFailedException( getRepository().getUrl()
                + " - Could not open output stream for resource: '" + resource + "'" );
        }

        putTransfer( outputData.getResource(), source, os, true );
    }

    public void closeConnection()
        throws ConnectionException
    {
        File f = new File( "target/wagon-data" );
        try
        {
            f.createNewFile();
        }
        catch ( IOException e )
        {
            throw new ConnectionException( e.getMessage(), e );
        }
    }

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        inputData.setInputStream( new StringInputStream( "<metadata />" ) );
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        outputData.setOutputStream( new ByteArrayOutputStream() );
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        // TODO Auto-generated method stub

    }
}
