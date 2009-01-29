package org.apache.maven.wagon.providers.ssh.external;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import org.codehaus.plexus.util.FileUtils;
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
        String content = "";
        if ( getRepository().getPermissions() != null )
        {
            String dirPerms = getRepository().getPermissions().getDirectoryMode();

            if ( dirPerms != null )
            {
                content += "directory.mode = " + dirPerms + "\n";
            }

            String filePerms = getRepository().getPermissions().getFileMode();
            if ( filePerms != null )
            {
                content += "file.mode = " + filePerms + "\n";
            }
        }
        try
        {
            FileUtils.fileWrite( "target/wagon.properties", content );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }

        outputData.setOutputStream( new ByteArrayOutputStream() );
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        // TODO Auto-generated method stub

    }
}
