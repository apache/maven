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
package org.apache.maven.cling.event;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.internal.build.DefaultLogEvent;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MachineBuildEventListener}.
 */
class MachineBuildEventListenerTest {

    private MockitoSession mockitoSession;
    private List<String> capturedOutput;
    private MachineBuildEventListener listener;

    @BeforeEach
    void beforeEach() {
        mockitoSession = Mockito.mockitoSession().startMocking();
        capturedOutput = new ArrayList<>();
        listener = new MachineBuildEventListener(capturedOutput::add);
    }

    @org.junit.jupiter.api.AfterEach
    void afterEach() {
        mockitoSession.finishMocking();
    }

    @Test
    void testLogEmitsJsonLine() {
        listener.log("Hello world");

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.startsWith("{"), "Should be JSON object");
        assertTrue(json.endsWith("}"), "Should be JSON object");
        assertTrue(json.contains("\"event\":\"log\""), "Should have event type");
        assertTrue(json.contains("\"message\":\"Hello world\""), "Should have message");
        assertTrue(json.contains("\"timestamp\":\""), "Should have timestamp");
    }

    @Test
    void testProjectLogMessageIncludesModule() {
        listener.projectLogMessage(
                "my-core",
                new DefaultLogEvent(
                        MonotonicClock.now(),
                        LogLevel.INFO,
                        "Compiling 42 source files",
                        "compiler",
                        null,
                        "[INFO] Compiling 42 source files"));

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"log\""));
        assertTrue(json.contains("\"level\":\"INFO\""));
        assertTrue(json.contains("\"module\":\"my-core\""));
        assertTrue(json.contains("\"message\":\"Compiling 42 source files\""));
    }

    @Test
    void testExecutionFailureEmitsJsonLine() {
        listener.executionFailure("my-core", true, "Compilation failed");

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"execution.failure\""));
        assertTrue(json.contains("\"module\":\"my-core\""));
        assertTrue(json.contains("\"halted\":true"));
        assertTrue(json.contains("\"error\":\"Compilation failed\""));
    }

    @Test
    void testTransferStartedEmitsJsonLine() {
        TransferResource resource = mock(TransferResource.class);
        when(resource.getResourceName()).thenReturn("org/apache/maven/core/4.1.0/core-4.1.0.jar");
        when(resource.getContentLength()).thenReturn(524288L);

        TransferEvent event = mock(TransferEvent.class);
        when(event.getType()).thenReturn(TransferEvent.EventType.STARTED);
        when(event.getResource()).thenReturn(resource);

        listener.transfer("my-core", event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"transfer.started\""));
        assertTrue(json.contains("\"artifact\":\"core-4.1.0.jar\""));
        assertTrue(json.contains("\"size\":524288"));
        assertTrue(json.contains("\"module\":\"my-core\""));
    }

    @Test
    void testTransferProgressedEmitsJsonLine() {
        TransferResource resource = mock(TransferResource.class);
        when(resource.getResourceName()).thenReturn("org/apache/maven/core/4.1.0/core-4.1.0.jar");
        when(resource.getContentLength()).thenReturn(524288L);

        TransferEvent event = mock(TransferEvent.class);
        when(event.getType()).thenReturn(TransferEvent.EventType.PROGRESSED);
        when(event.getResource()).thenReturn(resource);
        when(event.getTransferredBytes()).thenReturn(262144L);

        listener.transfer("my-core", event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"transfer.progressed\""));
        assertTrue(json.contains("\"transferred\":262144"));
        assertTrue(json.contains("\"total\":524288"));
    }

    @Test
    void testTransferCompletedEmitsJsonLine() {
        TransferResource resource = mock(TransferResource.class);
        when(resource.getResourceName()).thenReturn("org/apache/maven/core/4.1.0/core-4.1.0.jar");
        when(resource.getContentLength()).thenReturn(524288L);

        TransferEvent event = mock(TransferEvent.class);
        when(event.getType()).thenReturn(TransferEvent.EventType.SUCCEEDED);
        when(event.getResource()).thenReturn(resource);
        when(event.getTransferredBytes()).thenReturn(524288L);

        listener.transfer("my-core", event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"transfer.completed\""));
        assertTrue(json.contains("\"transferred\":524288"));
    }

    @Test
    void testTransferFailedEmitsJsonLine() {
        TransferResource resource = mock(TransferResource.class);
        when(resource.getResourceName()).thenReturn("org/apache/maven/core/4.1.0/core-4.1.0.jar");
        when(resource.getContentLength()).thenReturn(524288L);

        TransferEvent event = mock(TransferEvent.class);
        when(event.getType()).thenReturn(TransferEvent.EventType.FAILED);
        when(event.getResource()).thenReturn(resource);
        when(event.getException()).thenReturn(new RuntimeException("Connection timed out"));

        listener.transfer("my-core", event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"transfer.failed\""));
        assertTrue(json.contains("\"error\":\"Connection timed out\""));
    }

    @Test
    void testJsonEscapingInMessages() {
        listener.log("Message with \"quotes\" and \\backslash and\nnewline");

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        // Verify the JSON is properly escaped
        assertTrue(json.contains("\\\"quotes\\\""), "Quotes should be escaped");
        assertTrue(json.contains("\\\\backslash"), "Backslash should be escaped");
        assertTrue(json.contains("\\n"), "Newline should be escaped");
        // Verify it's parseable as a single line (no raw newlines)
        assertFalse(json.contains("\n"), "JSON line should not contain raw newlines");
    }

    @Test
    void testEmitEventIsSynchronized() throws Exception {
        // Verify that concurrent calls to emitEvent don't interleave
        List<String> output = new ArrayList<>();
        MachineBuildEventListener concurrentListener = new MachineBuildEventListener(output::add);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                concurrentListener.log("Thread1-" + i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                concurrentListener.log("Thread2-" + i);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(200, output.size(), "All events should be emitted");
        // Each line should be a complete JSON object
        for (String line : output) {
            assertTrue(line.startsWith("{") && line.endsWith("}"), "Each line should be a complete JSON object");
        }
    }

    @Test
    void testNoOpMethods() throws Exception {
        // These should not produce any output (handled by MachineExecutionEventLogger)
        listener.sessionStarted(null);
        listener.projectStarted("my-core");
        listener.projectFinished("my-core");
        listener.mojoStarted(null);
        listener.finish(0);
        listener.fail(new RuntimeException("error"));

        assertEquals(0, capturedOutput.size(), "No-op methods should not produce output");
    }
}
