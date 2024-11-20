/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.cli.transfer;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Console download progress meter.
 * <p>
 * This listener is not thread-safe and should be wrapped in the {@link SimplexTransferListener} in a multi-threaded scenario.
 */
@Deprecated
public class ConsoleMavenTransferListener extends AbstractMavenTransferListener {

    private final Map<TransferResourceIdentifier, TransferResourceAndSize> transfers = new LinkedHashMap<>();
    private final FileSizeFormat format = new FileSizeFormat(); // use in a synchronized fashion
    private final StringBuilder buffer = new StringBuilder(128); // use in a synchronized fashion

    private final boolean printResourceNames;
    private int lastLength;

    public ConsoleMavenTransferListener(
            MessageBuilderFactory messageBuilderFactory, PrintStream out, boolean printResourceNames) {
        this(messageBuilderFactory, new PrintWriter(out), printResourceNames);
    }

    public ConsoleMavenTransferListener(
            MessageBuilderFactory messageBuilderFactory, PrintWriter out, boolean printResourceNames) {
        super(messageBuilderFactory, out);
        this.printResourceNames = printResourceNames;
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        overridePreviousTransfer(event);

        super.transferInitiated(event);
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        overridePreviousTransfer(event);

        super.transferCorrupted(event);
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        transfers.put(
                new TransferResourceIdentifier(resource),
                new TransferResourceAndSize(resource, event.getTransferredBytes()));

        buffer.append("Progress (").append(transfers.size()).append("): ");

        Iterator<TransferResourceAndSize> entries = transfers.values().iterator();
        while (entries.hasNext()) {
            TransferResourceAndSize entry = entries.next();
            // just in case, make sure 0 <= complete <= total
            long complete = Math.max(0, entry.transferredBytes);
            long total = Math.max(complete, entry.resource.getContentLength());

            String resourceName = entry.resource.getResourceName();

            if (printResourceNames) {
                int idx = resourceName.lastIndexOf('/');

                if (idx < 0) {
                    buffer.append(resourceName);
                } else {
                    buffer.append(resourceName, idx + 1, resourceName.length());
                }
                buffer.append(" (");
            }

            format.formatProgress(buffer, complete, total);

            if (printResourceNames) {
                buffer.append(")");
            }

            if (entries.hasNext()) {
                buffer.append(" | ");
            }
        }

        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad(buffer, pad);
        buffer.append('\r');
        out.print(buffer);
        out.flush();
        buffer.setLength(0);
    }

    private void pad(StringBuilder buffer, int spaces) {
        String block = "                                        ";
        while (spaces > 0) {
            int n = Math.min(spaces, block.length());
            buffer.append(block, 0, n);
            spaces -= n;
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        transfers.remove(new TransferResourceIdentifier(event.getResource()));
        overridePreviousTransfer(event);

        super.transferSucceeded(event);
    }

    @Override
    public void transferFailed(TransferEvent event) {
        transfers.remove(new TransferResourceIdentifier(event.getResource()));
        overridePreviousTransfer(event);

        super.transferFailed(event);
    }

    private void overridePreviousTransfer(TransferEvent event) {
        if (lastLength > 0) {
            pad(buffer, lastLength);
            buffer.append('\r');
            out.print(buffer);
            out.flush();
            lastLength = 0;
            buffer.setLength(0);
        }
    }

    private record TransferResourceAndSize(TransferResource resource, long transferredBytes) {}
}
