package org.apache.maven.cli;

/* ====================================================================
 *   Copyright 2001-2005 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Console download progress meter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ConsoleDownloadMonitor
    extends AbstractLogEnabled
    implements TransferListener
{
    private long complete;

    public void transferInitiated( TransferEvent transferEvent )
    {
        System.out.println( "Downloading: " + transferEvent.getResource().getName() );
        complete = 0;
    }

    public void transferStarted( TransferEvent transferEvent )
    {
        // This space left intentionally blank
    }

    public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
    {
        long total = transferEvent.getResource().getContentLength();
        complete += length;
        // TODO [BP]: Sys.out may no longer be appropriate, but will \r work with getLogger()?
        System.out.print( ( complete / 1024 ) + "/" + ( total == 0 ? "?" : ( total / 1024 ) + "K" ) + "\r" );
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        long total = transferEvent.getResource().getContentLength();
        System.out.println( ( total / 1024 ) + "K downloaded" );
    }

    public void transferError( TransferEvent transferEvent )
    {
        getLogger().error( transferEvent.getException().getMessage() );
    }

    public void debug( String message )
    {
        getLogger().debug( message );
    }
}



