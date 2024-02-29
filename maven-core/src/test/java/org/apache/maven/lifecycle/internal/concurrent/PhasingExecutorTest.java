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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

public class PhasingExecutorTest {

    @Test
    void testPhaser() {
        PhasingExecutor p = new PhasingExecutor(Executors.newFixedThreadPool(4));
        p.execute(() -> waitSomeTime(p, 2));
        p.await();
    }

    private void waitSomeTime(Executor executor, int nb) {
        try {
            Thread.sleep(10);
            if (nb > 0) {
                executor.execute(() -> waitSomeTime(executor, nb - 1));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
