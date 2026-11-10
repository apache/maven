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
package org.apache.maven.api.logging;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link LoggerManager.Holder} bootstrap behaviour:
 * the shared {@code BOOTSTRAP} instance must be used both for re-entrant
 * calls during ServiceLoader discovery and as the no-provider fallback,
 * so that no buffered events are ever silently discarded.
 */
class LoggerManagerHolderTest {

    @BeforeEach
    @AfterEach
    void resetHolder() {
        // Drain BOOTSTRAP so its queue doesn't bleed between tests
        LoggerManager.Holder.BOOTSTRAP.drainTo(e -> {});
        LoggerManager.Holder.BOOTSTRAP.setLogSink(null);
        // Clear the global singleton so discover() runs fresh in each test
        LoggerManager.Holder.instance = null;
    }

    @Test
    void noProviderFallbackInstallsSharedBootstrap() {
        // No ServiceLoader provider on the test classpath — discover() must fall through
        // to the no-provider path and install the shared BOOTSTRAP constant, not a new
        // BufferingLoggerManager, so events buffered before this point are not lost.
        LoggerManager mgr = LoggerManager.get();

        assertSame(
                LoggerManager.Holder.BOOTSTRAP, mgr, "discover() must return the shared BOOTSTRAP, not a new instance");
        assertSame(
                LoggerManager.Holder.BOOTSTRAP,
                LoggerManager.Holder.instance,
                "Holder.instance must be set to the shared BOOTSTRAP");
    }

    @Test
    void bootstrapBuffersEventsAndDrainsOnSet() {
        // Enqueue an event into BOOTSTRAP before any real manager is installed.
        // Use warn() because the bootstrap manager defaults to WARN level.
        LoggerManager.Holder.instance = LoggerManager.Holder.BOOTSTRAP;
        LoggerManager.Holder.BOOTSTRAP.getLogger("test").warn("pre-install event");

        // Install a real manager with a sink — set() must drain the buffer
        List<LogEvent> received = new ArrayList<>();
        BufferingLoggerManager real = new BufferingLoggerManager();
        real.setLogSink(received::add);
        LoggerManager.set(real);

        assertEquals(1, received.size(), "buffered event must be drained on set()");
        assertEquals("pre-install event", received.get(0).message());
    }

    @Test
    void noProviderFallbackCapturesSubsequentEvents() {
        // discover() falls through to BOOTSTRAP; events logged after are captured
        List<LogEvent> captured = new ArrayList<>();
        LoggerManager.Holder.BOOTSTRAP.setLogSink(captured::add);

        LoggerManager.get().getLogger("test").warn("after discovery");

        assertEquals(1, captured.size());
        assertEquals("after discovery", captured.get(0).message());
    }

    @Test
    void bootstrapDefaultLevelIsWarn() {
        // BOOTSTRAP should only enable WARN and ERROR, not TRACE/DEBUG/INFO
        LoggerManager.Holder.instance = LoggerManager.Holder.BOOTSTRAP;
        BufferingLoggerManager bootstrap = LoggerManager.Holder.BOOTSTRAP;

        assertFalse(bootstrap.isLevelEnabled("test", LogLevel.TRACE));
        assertFalse(bootstrap.isLevelEnabled("test", LogLevel.DEBUG));
        assertFalse(bootstrap.isLevelEnabled("test", LogLevel.INFO));
        assertTrue(bootstrap.isLevelEnabled("test", LogLevel.WARN));
        assertTrue(bootstrap.isLevelEnabled("test", LogLevel.ERROR));
    }

    @Test
    void bootstrapInfoEventsAreFilteredOut() {
        // Events below WARN should not be buffered
        LoggerManager.Holder.instance = LoggerManager.Holder.BOOTSTRAP;

        LoggerManager.Holder.BOOTSTRAP.getLogger("test").info("should be filtered");
        LoggerManager.Holder.BOOTSTRAP.getLogger("test").debug("also filtered");
        LoggerManager.Holder.BOOTSTRAP.getLogger("test").warn("should pass");

        List<LogEvent> received = new ArrayList<>();
        BufferingLoggerManager real = new BufferingLoggerManager();
        real.setLogSink(received::add);
        LoggerManager.set(real);

        assertEquals(1, received.size(), "only WARN+ events should be buffered");
        assertEquals("should pass", received.get(0).message());
    }

    @Test
    void bootstrapBufferCapacityIsLimited() {
        // Use a fresh instance to avoid droppedCount bleeding from other tests
        BufferingLoggerManager mgr = new BufferingLoggerManager();
        LoggerManager.Holder.instance = mgr;

        // Fill buffer beyond capacity
        for (int i = 0; i < BufferingLoggerManager.MAX_BUFFER_SIZE + 100; i++) {
            mgr.getLogger("test").warn("event " + i);
        }

        // Drain and verify we got at most MAX_BUFFER_SIZE events
        List<LogEvent> received = new ArrayList<>();
        mgr.drainTo(received::add);
        assertTrue(
                received.size() <= BufferingLoggerManager.MAX_BUFFER_SIZE,
                "buffer should not exceed max size, got " + received.size());
        assertTrue(mgr.droppedCount() > 0, "some events should have been dropped");
        assertEquals(100, mgr.droppedCount(), "expected 100 dropped events");
    }
}
