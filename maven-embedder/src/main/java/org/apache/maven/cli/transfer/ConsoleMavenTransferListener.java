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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Console download progress meter.
 *
 */
public class ConsoleMavenTransferListener extends AbstractMavenTransferListener {

    public static final long MAX_DELAY_BETWEEN_UPDATES_MILLIS = 100;
    public static final long MIN_DELAY_BETWEEN_UPDATES_MILLIS = 50;
    public static final long MIN_DELAY_BETWEEN_UPDATES_NANOS =
            TimeUnit.MILLISECONDS.toNanos(MIN_DELAY_BETWEEN_UPDATES_MILLIS);

    private final Map<TransferResource, TransferEvent> transfers =
            new ConcurrentSkipListMap<>(Comparator.comparing(TransferResource::getResourceName));
    private final FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);
    private final ThreadLocal<MessageBuilder> buffers;

    private final boolean printResourceNames;
    private int lastLength;
    private final Object lock = new Object();
    private final Thread updater;

    public ConsoleMavenTransferListener(
            MessageBuilderFactory messageBuilderFactory, PrintStream out, boolean printResourceNames) {
        super(messageBuilderFactory, out);
        this.printResourceNames = printResourceNames;
        buffers = ThreadLocal.withInitial(() -> messageBuilderFactory.builder(128));
        updater = new Thread(this::update);
        updater.setDaemon(true);
        updater.start();
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        transfers.put(event.getResource(), event);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        transfers.put(event.getResource(), event);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        transfers.put(event.getResource(), event);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        transfers.put(event.getResource(), event);
        synchronized (lock) {
            if (transfers.size() == 1) {
                doUpdate();
            } else {
                lock.notifyAll();
            }
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        transfers.put(event.getResource(), event);
        synchronized (lock) {
            if (transfers.size() == 1) {
                doUpdate();
            } else {
                lock.notifyAll();
            }
        }
    }

    void update() {
        synchronized (lock) {
            try {
                long t0 = System.nanoTime();
                while (true) {
                    lock.wait(MAX_DELAY_BETWEEN_UPDATES_MILLIS);
                    long t1 = System.nanoTime();
                    if (t1 >= t0 + MIN_DELAY_BETWEEN_UPDATES_NANOS) {
                        doUpdate();
                        t0 = t1;
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    void doUpdate() {
        MessageBuilder buffer = buffers.get();

        Map<TransferResource, TransferEvent> transfers = new LinkedHashMap<>(this.transfers);
        for (Iterator<Map.Entry<TransferResource, TransferEvent>> it =
                        transfers.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<TransferResource, TransferEvent> entry = it.next();
            TransferEvent event = entry.getValue();
            TransferEvent.EventType type = event.getType();
            if (type == TransferEvent.EventType.INITIATED) {
                transferInitiated(buffer, event);
            } else if (type == TransferEvent.EventType.SUCCEEDED) {
                transferSucceeded(buffer, event);
            } else if (type == TransferEvent.EventType.PROGRESSED) {
                continue;
            }
            it.remove();
            this.transfers.compute(entry.getKey(), (r, e) -> (e == event ? null : e));
            if (buffer.length() > 0) {
                if (lastLength > 0) {
                    pad(buffer, lastLength - buffer.length());
                    lastLength = 0;
                }
                out.println(buffer);
                buffer.setLength(0);
            }
        }
        if (!transfers.isEmpty()) {
            for (Map.Entry<TransferResource, TransferEvent> entry : transfers.entrySet()) {
                TransferEvent event = entry.getValue();
                if (buffer.length() == 0) {
                    buffer.append("Progress (")
                            .append(Integer.toString(transfers.size()))
                            .append("): ");
                } else {
                    buffer.append(" | ");
                }
                long total = entry.getKey().getContentLength();
                long complete = event.getTransferredBytes();
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
            }
            if (lastLength > 0) {
                int l = buffer.length();
                pad(buffer, lastLength - l);
                lastLength = l;
            }
            buffer.append('\r');
            out.print(buffer);
            out.flush();
            buffer.setLength(0);
        }
    }

    @Override
    protected void println(Object message) {}

    private void pad(MessageBuilder buffer, int spaces) {
        String block = "                                        ";
        while (spaces > 0) {
            int n = Math.min(spaces, block.length());
            buffer.append(block, 0, n);
            spaces -= n;
        }
    }
}
