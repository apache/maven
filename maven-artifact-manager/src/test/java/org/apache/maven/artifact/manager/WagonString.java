package org.apache.maven.artifact.manager;

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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;

/**
 * A wagon stub that can be configured to output strings into some artifacts.
 */
public class WagonString
    extends AbstractWagon
{

    /**
     * A mapping from resource names to file contents. Retrievals will throw a {@link ResourceDoesNotExistException} if
     * a resource is requested which has no entry in this map.
     * 
     * @component.configuration default="resourceStrings"
     */
    private Map resourceStrings;

    public void closeConnection()
    {
        // NO-OP
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getIfNewer( resourceName, destination, 0 );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );
        fireGetInitiated( resource, destination );

        String data = (String) this.resourceStrings.get( resourceName );
        if ( data == null )
        {
            throw new ResourceDoesNotExistException( "Unexistent resource: " + resourceName );
        }

        fireGetStarted( resource, destination );
        try
        {
            byte[] bytes = data.getBytes( "UTF-8" );
            FileUtils.fileWrite( destination.getPath(), "UTF-8", data );
            fireTransferProgress( new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS,
                                                     TransferEvent.REQUEST_GET ), bytes, bytes.length );
        }
        catch ( IOException e )
        {
            return false;
        }
        fireGetCompleted( resource, destination );

        return true;
    }

    public void openConnectionInternal()
    {
        // NO-OP
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destination );
        firePutInitiated( resource, source );
        firePutStarted( resource, source );
        firePutCompleted( resource, source );
    }

    public String[] getSupportedProtocols()
    {
        return new String[] { "string" };
    }
}
