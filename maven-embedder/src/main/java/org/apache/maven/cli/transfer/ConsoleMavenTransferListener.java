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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Console download progress meter.
 *
 */
public class ConsoleMavenTransferListener extends AbstractMavenTransferListener {

    private Map<TransferResource, Long> transfers = new LinkedHashMap<>();
    private FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH); // use in a synchronized fashion
    private StringBuilder buffer = new StringBuilder(128); // use in a synchronized fashion

    private boolean printResourceNames;
    private int lastLength;

    public ConsoleMavenTransferListener(
            MessageBuilderFactory messageBuilderFactory, PrintStream out, boolean printResourceNames) {
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
        transfers.put(resource, event.getTransferredBytes());

        buffer.append("Progress (").append(transfers.size()).append("): ");

        Iterator<Map.Entry<TransferResource, Long>> entries =
                transfers.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<TransferResource, Long> entry = entries.next();
            long total = entry.getKey().getContentLength();
            Long complete = entry.getValue();

            String resourceName = entry.getKey().getResourceName();

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
        transfers.remove(event.getResource());
        overridePreviousTransfer(event);

        super.transferSucceeded(event);
    }

    @Override
    public void transferFailed(TransferEvent event) {
        transfers.remove(event.getResource());
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
}
