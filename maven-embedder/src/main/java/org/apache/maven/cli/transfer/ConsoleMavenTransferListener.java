package org.apache.maven.cli.transfer;

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
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Console download progress meter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ConsoleMavenTransferListener
    extends AbstractMavenTransferListener
{

    private Map<TransferResource, Long> downloads = new ConcurrentHashMap<>();

    private int lastLength;

    public ConsoleMavenTransferListener( PrintStream out )
    {
        super( out );
    }

    @Override
    public synchronized void transferInitiated( TransferEvent event )
    {
        overridePreviousTransfer( event );

        super.transferInitiated( event );
    }

    @Override
    public synchronized void transferCorrupted( TransferEvent event )
        throws TransferCancelledException
    {
        overridePreviousTransfer( event );

        super.transferCorrupted( event );
    }

    @Override
    public synchronized void transferProgressed( TransferEvent event )
        throws TransferCancelledException
    {
        TransferResource resource = event.getResource();
        downloads.put( resource, event.getTransferredBytes() );

        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( "Progress: " );

        Iterator<Map.Entry<TransferResource, Long>> iter = downloads.entrySet().iterator();
        while ( iter.hasNext() )
        {
            Map.Entry<TransferResource, Long> entry = iter.next();
            long total = entry.getKey().getContentLength();
            Long complete = entry.getValue();
            buffer.append( getStatus( complete, total ) );
            if ( iter.hasNext() )
            {
                buffer.append( " | " );
            }
        }

        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad( buffer, pad );
        buffer.append( '\r' );
        out.print( buffer.toString() );
        out.flush();
    }

    private String getStatus( long complete, long total )
    {
        DecimalFormat format = new FileDecimalFormat( Locale.ENGLISH );
        String status = format.format( complete );
        if ( total > 0 && complete != total )
        {
            status += "/" + format.format( total );
        }

        return status;
    }

    private void pad( StringBuilder buffer, int spaces )
    {
        String block = "                                        ";
        while ( spaces > 0 )
        {
            int n = Math.min( spaces, block.length() );
            buffer.append( block, 0, n );
            spaces -= n;
        }
    }

    @Override
    public synchronized void transferSucceeded( TransferEvent event )
    {
        downloads.remove( event.getResource() );
        overridePreviousTransfer( event );

        super.transferSucceeded( event );
    }

    @Override
    public synchronized void transferFailed( TransferEvent event )
    {
        downloads.remove( event.getResource() );
        overridePreviousTransfer( event );

        super.transferFailed( event );
    }

    private void overridePreviousTransfer( TransferEvent event )
    {
        if ( lastLength > 0 )
        {
            StringBuilder buffer = new StringBuilder( 128 );
            pad( buffer, lastLength );
            buffer.append( '\r' );
            out.print( buffer.toString() );
            out.flush();
            lastLength = 0;
        }
    }

}
