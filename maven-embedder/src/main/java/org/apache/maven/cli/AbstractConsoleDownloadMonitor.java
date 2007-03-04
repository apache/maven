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

import org.apache.maven.MavenTransferListener;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract console download progress meter.
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @since 2.0.5
 */
public abstract class AbstractConsoleDownloadMonitor
    extends AbstractLogEnabled
    implements MavenTransferListener
{
    private boolean showChecksumEvents = false;
    
    protected boolean showEvent( TransferEvent event )
    {
        if ( event.getResource() == null )
        {
            return true;
        }

        String resource = event.getResource().getName();

        if ( resource == null || resource.trim().length() == 0 )
        {
            return true;
        }

        if ( resource.endsWith( ".sha1" ) || resource.endsWith( ".md5" ) )
        {
            return showChecksumEvents;
        }

        return true;
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        if ( !showEvent( transferEvent ) )
        {
            return;
        }
        
        String message = transferEvent.getRequestType() == TransferEvent.REQUEST_PUT ? "Uploading" : "Downloading";

        String url = transferEvent.getWagon().getRepository().getUrl();

        // TODO: can't use getLogger() because this isn't currently instantiated as a component
        System.out.println( message + ": " + url + "/" + transferEvent.getResource().getName() );
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
        long contentLength = transferEvent.getResource().getContentLength();
        if ( contentLength != WagonConstants.UNKNOWN_LENGTH )
        {
            String type = ( transferEvent.getRequestType() == TransferEvent.REQUEST_PUT ? "uploaded" : "downloaded" );
            String l = contentLength >= 1024 ? ( contentLength / 1024 ) + "K" : contentLength + "b";
            System.out.println( l + " " + type );
        }
    }

    public void transferError( TransferEvent transferEvent )
    {
        // TODO: can't use getLogger() because this isn't currently instantiated as a component
        transferEvent.getException().printStackTrace();
    }

    /**
     * Do nothing
     */
    public void debug( String message )
    {
        // TODO: can't use getLogger() because this isn't currently instantiated as a component
//        getLogger().debug( message );
    }

    public boolean isShowChecksumEvents()
    {
        return showChecksumEvents;
    }

    public void setShowChecksumEvents( boolean showChecksumEvents )
    {
        this.showChecksumEvents = showChecksumEvents;
    }
}
