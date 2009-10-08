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

import org.apache.maven.repository.ArtifactTransferEvent;
import org.apache.maven.repository.ArtifactTransferListener;

public abstract class AbstractMavenTransferListener
    implements ArtifactTransferListener
{
    private boolean showChecksumEvents = false;
    
    protected boolean showEvent( ArtifactTransferEvent event )
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

    public void transferInitiated( ArtifactTransferEvent transferEvent )
    {
        if ( !showEvent( transferEvent ) )
        {
            return;
        }        
    }

    public void transferCompleted( ArtifactTransferEvent transferEvent )
    {
        long contentLength = transferEvent.getResource().getContentLength();
        if ( contentLength != -1 )
        {
            String type = ( transferEvent.getRequestType() == ArtifactTransferEvent.REQUEST_PUT ? "uploaded" : "downloaded" );
            String l = contentLength >= 1024 ? ( contentLength / 1024 ) + "K" : contentLength + "b";
            System.out.println( l + " " + type );
        }
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
