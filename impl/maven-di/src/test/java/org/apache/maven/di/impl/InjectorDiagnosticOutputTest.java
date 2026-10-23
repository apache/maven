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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.di.impl.InjectorDiagnosticsFixture.RecordingInjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_OUT)
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
    void outputChangesAndFailedDeltasRestartWithFullSnapshots() {
        AtomicInteger attempts = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (attempts.incrementAndGet() == 2) {
                    throw new IllegalStateException("delta output failed");
                }
                super.writeDump(output, dump);
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
        try (PrintStream other = new PrintStream(new ByteArrayOutputStream())) {
            System.setOut(other);
            injector.checkpoint();
            assertEquals(3, injector.dumps.size());
            assertFalse(injector.dumps.get(2).contains(", delta"));
            assertEquals(body(injector.dumps.get(1)), body(injector.dumps.get(2)));
        }
    }

    @Test
    void printStreamErrorsDoNotCommitAnUnwrittenSnapshot() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        AtomicInteger writes = new AtomicInteger();
        try (PrintStream sink =
                new PrintStream(
                        new OutputStream() {
                            @Override
                            public void write(int value) throws IOException {
                                if (writes.getAndIncrement() == 0) {
                                    throw new IOException("output unavailable");
                                }
                                captured.write(value);
                            }
                        },
                        true,
                        StandardCharsets.UTF_8) {
                    @Override
                    public boolean checkError() {
                        boolean failed = super.checkError();
                        clearError();
                        return failed;
                    }
                }) {
            System.setOut(sink);
            InjectorImpl injector = new InjectorImpl();
            injector.bindInstance(String.class, "value");
            assertEquals("value", injector.getInstance(String.class));
            captured.reset();
            assertEquals("value", injector.getInstance(String.class));
            String retry = captured.toString(StandardCharsets.UTF_8);
            assertTrue(retry.contains("candidate: java.lang.String"), retry);
            assertFalse(retry.contains(", delta"), retry);
        }
    }

    @Test
    void stickyPrintStreamErrorKeepsRetryingFullSnapshotsUntilOutputChanges() {
        ByteArrayOutputStream failed = new ByteArrayOutputStream();
        ByteArrayOutputStream recovered = new ByteArrayOutputStream();
        try (PrintStream sink = new PrintStream(failed, true, StandardCharsets.UTF_8) {
                    {
                        setError();
                    }
                };
                PrintStream recovery = new PrintStream(recovered, true, StandardCharsets.UTF_8)) {
            InjectorImpl injector = new InjectorImpl();
            injector.bindInstance(String.class, "fixture");
            System.setOut(sink);
            injector.debugBindings();
            injector.debugBindings();
            String attempts = failed.toString(StandardCharsets.UTF_8);
            assertEquals(
                    2,
                    attempts.lines()
                            .filter(line -> line.startsWith("Maven DI native registry "))
                            .count());
            assertFalse(attempts.contains(", delta"), attempts);
            System.setOut(recovery);
            injector.debugBindings();
            String success = recovered.toString(StandardCharsets.UTF_8);
            assertTrue(success.contains("candidate: java.lang.String"), success);
            assertFalse(success.contains(", delta"), success);
        }
    }
}
