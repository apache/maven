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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringOutputStream;

public class StringWagon
    extends StreamWagon
{
    private Map expectedContent = new HashMap();
    
    public void addExpectedContent( String resourceName, String expectedContent )
    {
        this.expectedContent.put( resourceName, expectedContent );
    }

    public String[] getSupportedProtocols()
    {
        return new String[] { "string" };
    }

    public void closeConnection()
        throws ConnectionException
    {
    }

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = inputData.getResource();

        String content = (String) expectedContent.get( resource.getName() );
        
        if ( content != null )
        {
            resource.setContentLength( content.length() );
            resource.setLastModified( System.currentTimeMillis() );

            inputData.setInputStream( new StringInputStream( content ) );
        }
        else
        {
            throw new ResourceDoesNotExistException( "No content provided for " + resource.getName() );
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        outputData.setOutputStream( new StringOutputStream() );
    }

    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException
    {
    }

    public void clearExpectedContent()
    {
        expectedContent.clear();        
    }
}
