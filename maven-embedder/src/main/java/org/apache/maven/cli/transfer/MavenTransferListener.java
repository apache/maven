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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
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
    private final ArrayBlockingQueue<TransferEvent> eventQueue;
    private final Consumer<List<TransferEvent>> consumer;

    public MavenTransferListener(TransferListener transferListener) {
        this.transferListener = requireNonNull(transferListener);
        this.eventQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        this.consumer = new MavenTransferEventDMX(transferListener);
        Thread updater = new Thread(this::feedConsumer);
        updater.setDaemon(true);
        updater.start();
    }

    public TransferListener getTransferListener() {
        return transferListener;
    }

    private void feedConsumer() {
        final ArrayList<TransferEvent> batch = new ArrayList<>(BATCH_MAX_SIZE);
        try {
            while (true) {
                batch.clear();
                if (eventQueue.drainTo(batch, BATCH_MAX_SIZE) == 0) {
                    batch.add(eventQueue.take());
                }
                consumer.accept(batch);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void put(TransferEvent event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        put(event);
    }

    @Override
    public void transferStarted(TransferEvent event) {
        put(event);
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        put(event);
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        put(event);
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        put(event);
    }

    @Override
    public void transferFailed(TransferEvent event) {
        put(event);
    }

    private static class MavenTransferEventDMX implements Consumer<List<TransferEvent>> {
        private final TransferListener delegate;

        private MavenTransferEventDMX(TransferListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(List<TransferEvent> transferEvents) {
            for (TransferEvent transferEvent : transferEvents) {
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
            }
        }
    }
}
