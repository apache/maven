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

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.maven.repository.ArtifactTransferEvent;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.ArtifactTransferResource;

public abstract class AbstractMavenTransferListener
    implements ArtifactTransferListener
{

    protected PrintStream out;

    private boolean showChecksumEvents;

    protected AbstractMavenTransferListener( PrintStream out )
    {
        this.out = ( out != null ) ? out : System.out;
    }

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

        doInitiated( transferEvent );
    }

    protected void doInitiated( ArtifactTransferEvent transferEvent )
    {
        String message =
            transferEvent.getRequestType() == ArtifactTransferEvent.REQUEST_PUT ? "Uploading" : "Downloading";

        out.println( message + ": " + transferEvent.getResource().getUrl() );
    }

    public void transferStarted( ArtifactTransferEvent transferEvent )
    {
        if ( !showEvent( transferEvent ) )
        {
            return;
        }

        doStarted( transferEvent );
    }

    protected void doStarted( ArtifactTransferEvent transferEvent )
    {
        // to be overriden by sub classes
    }

    public void transferProgress( ArtifactTransferEvent transferEvent )
    {
        if ( !showEvent( transferEvent ) )
        {
            return;
        }

        doProgress( transferEvent );
    }

    protected void doProgress( ArtifactTransferEvent transferEvent )
    {
        // to be overriden by sub classes
    }

    public void transferCompleted( ArtifactTransferEvent transferEvent )
    {
        if ( !showEvent( transferEvent ) )
        {
            return;
        }

        doCompleted( transferEvent );
    }

    protected void doCompleted( ArtifactTransferEvent transferEvent )
    {
        ArtifactTransferResource artifact = transferEvent.getResource();
        long contentLength = transferEvent.getTransferredBytes();
        if ( contentLength >= 0 )
        {
            String type =
                ( transferEvent.getRequestType() == ArtifactTransferEvent.REQUEST_PUT ? "uploaded" : "downloaded" );
            String l = contentLength >= 1024 ? toKB( contentLength ) + " KB" : contentLength + " B";

            String throughput = "";
            long duration = System.currentTimeMillis() - artifact.getTransferStartTime();
            if ( duration > 0 )
            {
                DecimalFormat format = new DecimalFormat( "0.0", new DecimalFormatSymbols( Locale.ENGLISH ) );
                double kbPerSec = ( contentLength / 1024.0 ) / ( duration / 1000.0 );
                throughput = " at " + format.format( kbPerSec ) + " KB/sec";
            }

            out.println( l + " " + type + throughput );
        }
    }

    protected long toKB( long bytes )
    {
        return ( bytes + 1023 ) / 1024;
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
