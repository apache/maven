package org.apache.maven.cli;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Console download progress meter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: ConsoleDownloadMonitor.java 169548 2005-05-11 01:04:50Z brett $
 */
public class BatchModeDownloadMonitor
    extends AbstractLogEnabled
    implements TransferListener
{
    public void transferInitiated( TransferEvent transferEvent )
    {
        String message = transferEvent.getRequestType() == TransferEvent.REQUEST_PUT ? "Uploading" : "Downloading";

        String url = transferEvent.getWagon().getRepository().getUrl();

        // TODO: can't use getLogger() because this isn't currently instantiated as a component
        System.out.println( message + ": " + url + "/" + transferEvent.getResource().getName() );
    }

    public void transferStarted( TransferEvent transferEvent )
    {
        // This space left intentionally blank
    }

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

    public void debug( String message )
    {
        // TODO: can't use getLogger() because this isn't currently instantiated as a component
//        getLogger().debug( message );
    }
}



