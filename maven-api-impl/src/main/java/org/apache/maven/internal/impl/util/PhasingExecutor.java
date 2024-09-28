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
package org.apache.maven.internal.impl.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

/**
 * The phasing executor is a simple executor that allows to execute tasks in parallel
 * and wait for all tasks to be executed before closing the executor. The tasks that are
 * currently being executed are allowed to submit new tasks while the executor is closed.
 * The executor implements {@link AutoCloseable} to allow using the executor with
 * a try-with-resources statement.
 *
 * The {@link #phase()} method can be used to submit tasks and wait for them to be executed
 * without closing the executor.
 */
public class PhasingExecutor implements Executor, AutoCloseable {
    private final ExecutorService executor;
    private final Phaser phaser = new Phaser();

    public PhasingExecutor(ExecutorService executor) {
        this.executor = executor;
        this.phaser.register();
    }

    @Override
    public void execute(Runnable command) {
        phaser.register();
        executor.submit(() -> {
            try {
                command.run();
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    public AutoCloseable phase() {
        phaser.register();
        return () -> phaser.awaitAdvance(phaser.arriveAndDeregister());
    }

    @Override
    public void close() {
        phaser.arriveAndAwaitAdvance();
        executor.shutdownNow();
    }
}
