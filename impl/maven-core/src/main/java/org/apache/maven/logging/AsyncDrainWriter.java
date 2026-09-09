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
package org.apache.maven.logging;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A lock-free buffering wrapper around a {@link Consumer Consumer&lt;String&gt;} that
 * eliminates contention when multiple threads log concurrently.
 * <p>
 * Callers enqueue messages into a {@link ConcurrentLinkedQueue} (lock-free),
 * then attempt a non-blocking drain via {@link ReentrantLock#tryLock()}.
 * If another thread is already draining, the caller returns immediately —
 * its message will be picked up by the ongoing or next drain cycle.
 * This ensures that at most one thread writes to the underlying consumer
 * at any time, without blocking producers.
 * <p>
 * The pattern eliminates the {@code synchronized} contention in
 * {@link java.io.PrintWriter#println(String)} that occurs when multiple
 * {@link org.apache.maven.impl.util.PhasingExecutor} threads log during
 * parallel model building.
 *
 * @since 4.0.0
 */
public class AsyncDrainWriter implements Consumer<String>, AutoCloseable {

    private final Consumer<String> delegate;
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock drainLock = new ReentrantLock();

    public AsyncDrainWriter(Consumer<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void accept(String msg) {
        queue.add(msg);
        tryDrain();
    }

    /**
     * Attempts a non-blocking drain of all queued messages.
     * Only one thread drains at a time; others return immediately.
     * After releasing the lock, a re-check ensures no message is
     * stranded by a race between enqueue and the last poll.
     */
    private void tryDrain() {
        if (drainLock.tryLock()) {
            try {
                drain();
            } finally {
                drainLock.unlock();
            }
            // Re-check: a message may have been enqueued after our last poll()
            // but before unlock(). The enqueuer's tryLock() would have failed,
            // so we need to pick it up here.
            if (!queue.isEmpty() && drainLock.tryLock()) {
                try {
                    drain();
                } finally {
                    drainLock.unlock();
                }
            }
        }
    }

    private void drain() {
        String m;
        while ((m = queue.poll()) != null) {
            delegate.accept(m);
        }
    }

    /**
     * Flushes all remaining buffered messages to the delegate.
     * Blocks until the drain is complete — call this before shutdown
     * to ensure no messages are lost.
     */
    @Override
    public void close() {
        drainLock.lock();
        try {
            drain();
        } finally {
            drainLock.unlock();
        }
    }
}
