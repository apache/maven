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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

/**
 * Console download progress meter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ConsoleMavenTransferListener
    extends AbstractMavenTransferListener
{

    private Map<TransferResource, Long> transfers = Collections.synchronizedMap(
                                                        new LinkedHashMap<TransferResource, Long>() );

    private boolean printResourceNames;
    private int lastLength;

    public ConsoleMavenTransferListener( PrintStream out, boolean printResourceNames )
    {
        super( out );
        this.printResourceNames = printResourceNames;
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
        transfers.put( resource, event.getTransferredBytes() );

        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( "Progress (" ).append(  transfers.size() ).append( "): " );

        synchronized ( transfers )
        {
            Iterator<Map.Entry<TransferResource, Long>> entries = transfers.entrySet().iterator();
            while ( entries.hasNext() )
            {
                Map.Entry<TransferResource, Long> entry = entries.next();
                long total = entry.getKey().getContentLength();
                Long complete = entry.getValue();
                buffer.append( getStatus( entry.getKey().getResourceName(), complete, total ) );
                if ( entries.hasNext() )
                {
                    buffer.append( " | " );
                }
            }
        }

        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad( buffer, pad );
        buffer.append( '\r' );
        out.print( buffer );
        out.flush();
    }

    private String getStatus( String resourceName, long complete, long total )
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );
        StringBuilder status = new StringBuilder();

        if ( printResourceNames )
        {
            status.append( substringAfterLast( resourceName,  "/" ) );
            status.append( " (" );
        }

        status.append( format.formatProgress( complete, total ) );

        if ( printResourceNames )
        {
            status.append( ")" );
        }

        return status.toString();
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
        transfers.remove( event.getResource() );
        overridePreviousTransfer( event );

        super.transferSucceeded( event );
    }

    @Override
    public synchronized void transferFailed( TransferEvent event )
    {
        transfers.remove( event.getResource() );
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
            out.print( buffer );
            out.flush();
            lastLength = 0;
        }
    }

}
