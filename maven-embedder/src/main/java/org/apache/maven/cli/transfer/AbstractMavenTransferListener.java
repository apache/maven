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
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.Locale;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

public abstract class AbstractMavenTransferListener
    extends AbstractTransferListener
{

    // CHECKSTYLE_OFF: LineLength
    /**
     * Formats file length with the associated <a href="https://en.wikipedia.org/wiki/Metric_prefix">SI</a> prefix
     * (GB, MB, kB) and using the pattern <code>###0.#</code> by default.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Metric_prefix">https://en.wikipedia.org/wiki/Metric_prefix</a>
     * @see <a href="https://en.wikipedia.org/wiki/Binary_prefix">https://en.wikipedia.org/wiki/Binary_prefix</a>
     * @see <a
     *      href="https://en.wikipedia.org/wiki/Octet_%28computing%29">https://en.wikipedia.org/wiki/Octet_(computing)</a>
     */
    // CHECKSTYLE_ON: LineLength
    static class FileDecimalFormat
        extends DecimalFormat
    {
        private static final long serialVersionUID = -684999256062614038L;

        /**
         * Default constructor
         *
         * @param locale
         */
        public FileDecimalFormat( Locale locale )
        {
            super( "###0.#", new DecimalFormatSymbols( locale ) );
        }

        /** {@inheritDoc} */
        public StringBuffer format( long fs, StringBuffer result, FieldPosition fieldPosition )
        {
            if ( fs > 1000L * 1000L * 1000L )
            {
                result = super.format( (float) fs / ( 1000L * 1000L * 1000L ), result, fieldPosition );
                result.append( " GB" );
                return result;
            }

            if ( fs > 1000L * 1000L )
            {
                result = super.format( (float) fs / ( 1000L * 1000L ), result, fieldPosition );
                result.append( " MB" );
                return result;
            }

            if ( fs > 1000L )
            {
                result = super.format( (float) fs / ( 1000L ), result, fieldPosition );
                result.append( " kB" );
                return result;
            }

            result = super.format( fs, result, fieldPosition );
            result.append( " B" );
            return result;
        }
    }

    protected PrintStream out;

    protected AbstractMavenTransferListener( PrintStream out )
    {
        this.out = out;
    }

    @Override
    public void transferInitiated( TransferEvent event )
    {
        String type = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

        TransferResource resource = event.getResource();
        out.println( type + ": " + resource.getRepositoryUrl() + resource.getResourceName() );
    }

    @Override
    public void transferCorrupted( TransferEvent event )
        throws TransferCancelledException
    {
        TransferResource resource = event.getResource();
        out.println( "[WARNING] " + event.getException().getMessage() + " for " + resource.getRepositoryUrl()
            + resource.getResourceName() );
    }

    @Override
    public void transferSucceeded( TransferEvent event )
    {
        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();

        DecimalFormat format = new FileDecimalFormat( Locale.ENGLISH );
        String type = ( event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded" );
        String len = format.format( contentLength );

        String throughput = "";
        long duration = System.currentTimeMillis() - resource.getTransferStartTime();
        if ( duration > 0L )
        {
            double bytesPerSecond = contentLength / ( duration / 1000.0 );
            throughput = " at " + format.format( (long) bytesPerSecond ) + "/s";
        }

        out.println( type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len
            + throughput + ")" );
    }

}
