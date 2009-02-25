package org.apache.maven.cli;

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

import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.PrintStream;

/**
 * Abstract console download progress meter.
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @since 2.0.5
 */
public abstract class AbstractConsoleDownloadMonitor
    extends AbstractLogEnabled
    implements TransferListener
{
    private Logger logger;

    PrintStream out = System.out;

    public AbstractConsoleDownloadMonitor()
    {
    }

    public AbstractConsoleDownloadMonitor( Logger logger )
    {
        this.logger = logger;
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        String message = transferEvent.getRequestType() == TransferEvent.REQUEST_PUT ? "Uploading" : "Downloading";

        String url = transferEvent.getWagon().getRepository().getUrl();

        // TODO: can't use getLogger() because this isn't currently instantiated as a component
        out.println( message + ": " + url + "/" + transferEvent.getResource().getName() );
    }

    /**
     * Do nothing
     */
    public void transferStarted( TransferEvent transferEvent )
    {
        // This space left intentionally blank
    }

    /**
     * Do nothing
     */
    public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
    {
        // This space left intentionally blank
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        String line = createCompletionLine( transferEvent );
        out.println( line );
    }

    protected String createCompletionLine( TransferEvent transferEvent )
    {
        String line;
        long contentLength = transferEvent.getResource().getContentLength();
        if ( contentLength != WagonConstants.UNKNOWN_LENGTH )
        {
            StringBuffer buf = new StringBuffer();
            String type = ( transferEvent.getRequestType() == TransferEvent.REQUEST_PUT ? "uploaded" : "downloaded" );
            buf.append( contentLength >= 1024 ? ( contentLength / 1024 ) + "K" : contentLength + "b" );
            String name = transferEvent.getResource().getName();
            name = name.substring( name.lastIndexOf( '/' ) + 1, name.length() );
            buf.append( " " );
            buf.append( type );
            buf.append( "  (" );
            buf.append( name );
            buf.append( ")" );
            line = buf.toString();
        }
        else
        {
            line = "";
        }
        return line;
    }

    public void transferError( TransferEvent transferEvent )
    {
        // these errors should already be handled elsewhere by Maven since they all result in an exception from Wagon
        if ( logger != null )
        {
            Exception exception = transferEvent.getException();
            logger.debug( exception.getMessage(), exception );
        }
    }

    /**
     * Do nothing
     */
    public void debug( String message )
    {
        if ( logger != null )
        {
            logger.debug( message );
        }
    }

}
