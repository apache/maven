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
package org.apache.maven.di.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.di.impl.InjectorDiagnosticsFixture.RecordingInjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@Timeout(30)
class InjectorDiagnosticOutputTest {
    private InjectorDiagnosticsFixture environment;

    @BeforeEach
    void enableDiagnostics() {
        environment = new InjectorDiagnosticsFixture();
    }

    @AfterEach
    void restoreEnvironment() {
        environment.close();
    }

    @Test
    void failedDeltasRestartWithFullSnapshots() {
        AtomicInteger attempts = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            protected void writeDump(String dump) {
                if (attempts.incrementAndGet() == 2) {
                    throw new IllegalStateException("delta output failed");
                }
                super.writeDump(dump);
            }
        };
        injector.bindInstance(String.class, "value");
        injector.checkpoint();
        injector.bindInstance(Integer.class, 1);
        injector.checkpoint();
        assertEquals(1, injector.dumps.size());
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        assertFalse(injector.dumps.get(1).contains(", delta"));
        assertTrue(injector.dumps.get(1).contains("candidate: java.lang.String"));
        // A further checkpoint with no changes does not produce another dump.
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        // The restarted full snapshot contains both bindings.
        assertTrue(injector.dumps.get(1).contains("candidate: java.lang.Integer"));
    }

    @Test
    void sinkExceptionKeepsRetryingFullSnapshotsOnNextCheckpoint() {
        AtomicInteger writes = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            protected void writeDump(String dump) {
                if (writes.getAndIncrement() == 0) {
                    throw new IllegalStateException("sink unavailable");
                }
                super.writeDump(dump);
            }
        };
        injector.bindInstance(String.class, "value");
        injector.checkpoint(); // fails: nothing recorded
        assertEquals(0, injector.dumps.size());
        injector.checkpoint(); // succeeds: full snapshot (not delta)
        assertEquals(1, injector.dumps.size());
        assertTrue(injector.dumps.get(0).contains("candidate: java.lang.String"), injector.dumps.get(0));
        assertFalse(injector.dumps.get(0).contains(", delta"), injector.dumps.get(0));
    }
}
