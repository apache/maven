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
package org.apache.maven.impl.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The phasing executor allows executing tasks in parallel and waiting for all tasks
 * to be executed before fully closing the executor. Tasks can be submitted even after
 * the close method has been called, allowing for use with try-with-resources.
 * The {@link #phase()} method can be used to submit tasks and wait for them to be
 * executed without closing the executor.
 *
 * <p>Example usage:
 * <pre>
 * try (PhasingExecutor executor = createExecutor()) {
 *     try (var phase = executor.phase()) {
 *         executor.execute(() -> { /* task 1 *&#47; });
 *         executor.execute(() -> { /* task 2 *&#47; });
 *         More tasks...
 *     } This will wait for all tasks in this phase to complete
 *
 *     You can have multiple phases
 *     try (var anotherPhase = executor.phase()) {
 *         executor.execute(() -> { /* another task *&#47; });
 *     }
 * } The executor will wait for all tasks to complete before shutting down
 * </pre>
 */
public class PhasingExecutor implements Executor, AutoCloseable {
    private static final AtomicInteger ID = new AtomicInteger(0);
    private static final Logger LOGGER = LoggerFactory.getLogger(PhasingExecutor.class);

    private final ExecutorService executor;
    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);
    private final AtomicBoolean inPhase = new AtomicBoolean(false);
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private final AtomicInteger completedTaskCount = new AtomicInteger(0);
    private final int id = ID.incrementAndGet();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition taskCompletionCondition = lock.newCondition();

    public PhasingExecutor(ExecutorService executor) {
        this.executor = executor;
        log("[{}][general] PhasingExecutor created.");
    }

    @Override
    public void execute(Runnable command) {
        activeTaskCount.incrementAndGet();
        log("[{}][task] Task submitted. Active tasks: {}", activeTaskCount.get());
        executor.execute(() -> {
            try {
                log("[{}][task] Task executing. Active tasks: {}", activeTaskCount.get());
                command.run();
            } finally {
                lock.lock();
                try {
                    completedTaskCount.incrementAndGet();
                    activeTaskCount.decrementAndGet();
                    log("[{}][task] Task completed. Active tasks: {}", activeTaskCount.get());
                    taskCompletionCondition.signalAll();
                    if (activeTaskCount.get() == 0 && shutdownInitiated.get()) {
                        log("[{}][task] Last task completed. Initiating executor shutdown.");
                        executor.shutdown();
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public AutoCloseable phase() {
        if (inPhase.getAndSet(true)) {
            throw new IllegalStateException("Already in a phase");
        }
        int phaseNumber = completedTaskCount.get();
        log("[{}][phase] Entering phase {}. Active tasks: {}", phaseNumber, activeTaskCount.get());
        return () -> {
            try {
                int tasksAtPhaseStart = completedTaskCount.get();
                log("[{}][phase] Closing phase {}. Waiting for all tasks to complete.", phaseNumber);
                lock.lock();
                try {
                    while (activeTaskCount.get() > 0
                            && completedTaskCount.get() - tasksAtPhaseStart < activeTaskCount.get()) {
                        taskCompletionCondition.await(100, TimeUnit.MILLISECONDS);
                    }
                } finally {
                    lock.unlock();
                }
                log("[{}][phase] Phase {} completed. Total completed tasks: {}", phaseNumber, completedTaskCount.get());
            } catch (InterruptedException e) {
                log("[{}][phase] Phase {} was interrupted.", phaseNumber);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Phase interrupted", e);
            } finally {
                inPhase.set(false);
            }
        };
    }

    @Override
    public void close() {
        log("[{}][close] Closing PhasingExecutor. Active tasks: {}", activeTaskCount.get());
        if (shutdownInitiated.getAndSet(true)) {
            log("[{}][close] Shutdown already initiated. Returning.");
            return;
        }

        lock.lock();
        try {
            while (activeTaskCount.get() > 0) {
                log("[{}][close] Waiting for {} active tasks to complete.", activeTaskCount.get());
                taskCompletionCondition.await(100, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            log("[{}][close] Interrupted while waiting for tasks to complete.");
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
            log("[{}][close] All tasks completed. Shutting down executor.");
            executor.shutdown();
        }
        log("[{}][close] PhasingExecutor closed. Total completed tasks: {}", completedTaskCount.get());
    }

    private void log(String message) {
        LOGGER.debug(message, id);
    }

    private void log(String message, Object o1) {
        LOGGER.debug(message, id, o1);
    }

    private void log(String message, Object o1, Object o2) {
        LOGGER.debug(message, id, o1, o2);
    }
}
