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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Console download progress meter.
 *
 */
public class ConsoleMavenTransferListener extends AbstractMavenTransferListener {

    private final Map<TransferResource, Long> transfers =
            new ConcurrentSkipListMap<>(Comparator.comparing(TransferResource::getResourceName));
    private final FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);
    private final ThreadLocal<StringBuilder> buffers = ThreadLocal.withInitial(() -> new StringBuilder(128));

    private final boolean printResourceNames;
    private int lastLength;

    public ConsoleMavenTransferListener(
            MessageBuilderFactory messageBuilderFactory, PrintStream out, boolean printResourceNames) {
        super(messageBuilderFactory, out);
        this.printResourceNames = printResourceNames;
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        StringBuilder buffer = buffers.get();
        overridePreviousTransfer(buffer);
        transferInitiated(buffer, event);
        buffer.setLength(0);
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        StringBuilder buffer = buffers.get();
        overridePreviousTransfer(buffer);
        transferCorrupted(buffer, event);
        buffer.setLength(0);
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        transfers.put(event.getResource(), event.getTransferredBytes());

        StringBuilder buffer = buffers.get();
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

        overridePreviousTransfer(buffer);
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        transfers.remove(event.getResource());
        StringBuilder buffer = buffers.get();
        overridePreviousTransfer(buffer);
        transferSucceeded(buffer, event);
        buffer.setLength(0);
    }

    @Override
    public void transferFailed(TransferEvent event) {
        transfers.remove(event.getResource());
        StringBuilder buffer = buffers.get();
        overridePreviousTransfer(buffer);
    }

    protected synchronized void overridePreviousTransfer(StringBuilder buffer) {
        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad(buffer, pad);
        buffer.append('\r');
        out.print(buffer);
        out.flush();
        buffer.setLength(0);
    }

    protected synchronized void println(Object message) {
        out.println(message);
    }

    private void pad(StringBuilder buffer, int spaces) {
        String block = "                                        ";
        while (spaces > 0) {
            int n = Math.min(spaces, block.length());
            buffer.append(block, 0, n);
            spaces -= n;
        }
    }
}
