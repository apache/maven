package org.apache.maven.artifact.ant;

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

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Log wagon events in the ant tasks
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class AntDownloadMonitor
    extends ProjectComponent
    implements TransferListener
{
    public void debug( String s )
    {
        log( s, Project.MSG_DEBUG );
    }

    public void transferCompleted( TransferEvent event )
    {
    }

    public void transferError( TransferEvent event )
    {
        log( event.getException().getMessage(), Project.MSG_ERR );
    }

    public void transferInitiated( TransferEvent event )
    {
        String message = event.getRequestType() == TransferEvent.REQUEST_PUT ? "Uploading" : "Downloading";

        log( message + ": " + event.getResource().getName() );
    }

    public void transferProgress( TransferEvent event, byte[] bytes, int i )
    {
    }

    public void transferStarted( TransferEvent event )
    {
        long contentLength = event.getResource().getContentLength();
        if ( contentLength > 0 )
        {
            log( "Transferring " + ( contentLength / 1024 ) + "K" );
        }
    }
}
