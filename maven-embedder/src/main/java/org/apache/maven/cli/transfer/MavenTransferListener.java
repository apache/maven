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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TransferListener} implementation that wraps another delegate {@link TransferListener} but makes it run
 * on single thread, keeping the listener logic simple.
 *
 * @since 4.0.0
 */
public final class MavenTransferListener extends AbstractTransferListener {
    private static final int QUEUE_SIZE = 1024;
    private static final int BATCH_MAX_SIZE = 500;
    private final TransferListener transferListener;
    private final ArrayBlockingQueue<AsyncExchange> eventQueue;
    private final Demuxer demuxer;

    public MavenTransferListener(TransferListener transferListener) {
        this.transferListener = requireNonNull(transferListener);
        this.eventQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        this.demuxer = new Demuxer(transferListener);
        Thread updater = new Thread(this::feedConsumer);
        updater.setDaemon(true);
        updater.start();
    }

    public TransferListener getTransferListener() {
        return transferListener;
    }

    private void feedConsumer() {
        final ArrayList<AsyncExchange> batch = new ArrayList<>(BATCH_MAX_SIZE);
        try {
            while (true) {
                batch.clear();
                if (eventQueue.drainTo(batch, BATCH_MAX_SIZE) == 0) {
                    batch.add(eventQueue.take());
                }
                demuxer.demux(batch);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void put(TransferEvent event, boolean last) {
        try {
            AsyncExchange exchange;
            if (last) {
                exchange = new SyncExchange(event);
            } else {
                exchange = new AsyncExchange(event);
            }
            eventQueue.put(exchange);
            exchange.waitForProcessed();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final ConcurrentHashMap<File, Boolean> ongoing = new ConcurrentHashMap<>();

    @Override
    public void transferInitiated(TransferEvent event) {
        ongoing.putIfAbsent(event.getResource().getFile(), Boolean.TRUE);
        put(event, false);
    }

    @Override
    public void transferStarted(TransferEvent event) {
        put(event, false);
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        put(event, false);
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        put(event, false);
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        ongoing.remove(event.getResource().getFile());
        put(event, ongoing.isEmpty());
    }

    @Override
    public void transferFailed(TransferEvent event) {
        ongoing.remove(event.getResource().getFile());
        put(event, ongoing.isEmpty());
    }

    private static class AsyncExchange {
        private final TransferEvent event;

        private AsyncExchange(TransferEvent event) {
            this.event = event;
        }

        public void process(Consumer<TransferEvent> consumer) {
            consumer.accept(event);
        }

        public void waitForProcessed() throws InterruptedException {
            // nothing, is async
        }
    }

    private static class SyncExchange extends AsyncExchange {
        private final CountDownLatch latch = new CountDownLatch(1);

        private SyncExchange(TransferEvent event) {
            super(event);
        }

        @Override
        public void process(Consumer<TransferEvent> consumer) {
            super.process(consumer);
            latch.countDown();
        }

        @Override
        public void waitForProcessed() throws InterruptedException {
            latch.await();
        }
    }

    private static class Demuxer {
        private final TransferListener delegate;

        private Demuxer(TransferListener delegate) {
            this.delegate = delegate;
        }

        public void demux(List<AsyncExchange> exchanges) {
            for (AsyncExchange exchange : exchanges) {
                exchange.process(transferEvent -> {
                    TransferEvent.EventType type = transferEvent.getType();
                    try {
                        switch (type) {
                            case INITIATED:
                                delegate.transferInitiated(transferEvent);
                                break;
                            case STARTED:
                                delegate.transferStarted(transferEvent);
                                break;
                            case PROGRESSED:
                                delegate.transferProgressed(transferEvent);
                                break;
                            case CORRUPTED:
                                delegate.transferCorrupted(transferEvent);
                                break;
                            case SUCCEEDED:
                                delegate.transferSucceeded(transferEvent);
                                break;
                            case FAILED:
                                delegate.transferFailed(transferEvent);
                                break;
                            default:
                                throw new IllegalArgumentException("Type: " + type);
                        }
                    } catch (TransferCancelledException e) {
                        // we do not cancel, ignore it
                    }
                });
            }
        }
    }
}
