package org.apache.maven.wagon.providers.coreit;

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

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.component.annotations.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Shamelessly copied from ScpExternalWagon in this same project...
 */
@Component( role = org.apache.maven.wagon.Wagon.class,  hint = "http-coreit", instantiationStrategy = "per-lookup" )
public class CoreItHttpWagon
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
            throw new TransferFailedException(
                getRepository().getUrl() + " - Could not open input stream for resource: '" + resource + "'" );
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
            throw new TransferFailedException(
                getRepository().getUrl() + " - Could not open output stream for resource: '" + resource + "'" );
        }

        putTransfer( outputData.getResource(), source, os, true );
    }

    public void closeConnection()
        throws ConnectionException
    {
        File f = new File( "target/wagon-data" );
        try
        {
            f.getParentFile().mkdirs();
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
        try
        {
            inputData.setInputStream( new ByteArrayInputStream( "<metadata />".getBytes( "UTF-8" ) ) );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Broken JVM", e );
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        Properties props = new Properties();
        if ( getRepository().getPermissions() != null )
        {
            String dirPerms = getRepository().getPermissions().getDirectoryMode();

            if ( dirPerms != null )
            {
                props.setProperty( "directory.mode", dirPerms );
            }

            String filePerms = getRepository().getPermissions().getFileMode();
            if ( filePerms != null )
            {
                props.setProperty( "file.mode", filePerms );
            }
        }

        try
        {
            new File( "target" ).mkdirs();

            try ( OutputStream os = new FileOutputStream( "target/wagon.properties" ) )
            {
                props.store( os, "MAVEN-CORE-IT-WAGON" );
            }
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
        // ignore
    }
}
