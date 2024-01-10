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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    public static final long MAX_DELAY_BETWEEN_UPDATES_NANOS =
            TimeUnit.MILLISECONDS.toNanos(MAX_DELAY_BETWEEN_UPDATES_MILLIS);
    public static final long MIN_DELAY_BETWEEN_UPDATES_MILLIS = 10;
    public static final long MIN_DELAY_BETWEEN_UPDATES_NANOS =
            TimeUnit.MILLISECONDS.toNanos(MIN_DELAY_BETWEEN_UPDATES_MILLIS);
    public static final long MAX_ALIVE_THREAD_MILLIS = 5000;
    public static final long MAX_ALIVE_THREAD_NANOS = TimeUnit.MILLISECONDS.toNanos(MAX_ALIVE_THREAD_MILLIS);

    private final Map<TransferResource, TransferEvent> ongoing =
            new ConcurrentSkipListMap<>(Comparator.comparing(TransferResource::getResourceName));
    private final Map<TransferResource, TransferEvent> done =
            new ConcurrentSkipListMap<>(Comparator.comparing(TransferResource::getResourceName));

    private final BlockingQueue<TransferEvent> queue = new LinkedBlockingQueue<>();

    private final FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);
    private final ThreadLocal<MessageBuilder> buffers;

    private final boolean printResourceNames;
    private int lastLength;
    private final AtomicReference<Thread> updater = new AtomicReference<>();

    public ConsoleMavenTransferListener(
            MessageBuilderFactory messageBuilderFactory, PrintStream out, boolean printResourceNames) {
        super(messageBuilderFactory, out);
        this.printResourceNames = printResourceNames;
        buffers = ThreadLocal.withInitial(() -> messageBuilderFactory.builder(128));
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        ongoing.put(event.getResource(), event);
        queue.add(event);
        startThread();
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        ongoing.put(event.getResource(), event);
        queue.add(event);
        startThread();
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        ongoing.put(event.getResource(), event);
        queue.add(event);
        startThread();
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        done.put(event.getResource(), event);
        ongoing.remove(event.getResource(), event);
        queue.add(event);
        if (ongoing.isEmpty()) {
            synchronized (queue) {
                doUpdate(queue);
            }
        } else {
            startThread();
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        done.put(event.getResource(), event);
        ongoing.remove(event.getResource(), event);
        queue.add(event);
        if (ongoing.isEmpty()) {
            synchronized (queue) {
                doUpdate(queue);
            }
        } else {
            startThread();
        }
    }

    void startThread() {
        Thread thread = updater.get();
        if (thread == null) {
            synchronized (this) {
                thread = updater.get();
                if (thread == null) {
                    thread = new Thread(this::update);
                    thread.setDaemon(true);
                    thread.start();
                    updater.set(thread);
                }
            }
        }
    }

    void update() {
        try {
            long t0 = System.nanoTime();
            long mt = t0 + MAX_DELAY_BETWEEN_UPDATES_NANOS;
            long le = t0;
            long t1 = t0;
            Map<TransferResource, TransferEvent> transfers = new LinkedHashMap<>();
            while (t1 < le + MAX_ALIVE_THREAD_NANOS) {
                TransferEvent event = queue.poll(mt - t0, TimeUnit.NANOSECONDS);
                t1 = System.nanoTime();
                while (event != null) {
                    le = t1;
                    transfers.put(event.getResource(), event);
                    event = queue.poll();
                }
                if (t1 >= t0 + MIN_DELAY_BETWEEN_UPDATES_NANOS) {
                    doUpdate(transfers);
                    t0 = t1;
                    mt = t0 + MAX_DELAY_BETWEEN_UPDATES_NANOS;
                }
            }
        } catch (InterruptedException e) {
            // ignore
        } finally {
            updater.set(null);
        }
    }

    void doUpdate(BlockingQueue<TransferEvent> queue) {
        Map<TransferResource, TransferEvent> transfers = new LinkedHashMap<>();
        TransferEvent event = queue.poll();
        while (event != null) {
            transfers.put(event.getResource(), event);
            event = queue.poll();
        }
        doUpdate(transfers);
    }

    void doUpdate(Map<TransferResource, TransferEvent> transfers) {
        MessageBuilder buffer = buffers.get();

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
