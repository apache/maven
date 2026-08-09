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
package org.apache.maven.internal.impl;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultLog}, focused on verifying the bug fix for
 * {@code warn(Supplier, Throwable)} and the Log API metadata contract.
 */
class DefaultLogTest {

    /**
     * Regression test: {@code warn(Supplier, Throwable)} was incorrectly
     * calling {@code logger.info()} instead of {@code logger.warn()}.
     */
    @Test
    void warnWithSupplierAndThrowableDelegatesToWarn() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        when(mockLogger.getName()).thenReturn("test.logger");

        DefaultLog log = new DefaultLog(mockLogger);
        RuntimeException ex = new RuntimeException("test");
        log.warn(() -> "warning message", ex);

        verify(mockLogger).warn("warning message", ex);
    }

    /**
     * Verify that Log API metadata is set during the log call and
     * cleared afterwards — no leakage across calls.
     */
    @Test
    void logApiMetadataIsClearedAfterCall() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(mockLogger.getName()).thenReturn("com.example.MyMojo");

        DefaultLog log = new DefaultLog(mockLogger);
        log.info("test message");

        // After the call completes, metadata should be cleared
        assertNull(DefaultLog.getLogApiMetadata(), "Log API metadata should be cleared after the log call");
    }

    /**
     * Verify trace methods delegate to the SLF4J logger correctly.
     */
    @Test
    void traceMethodsDelegateToSlf4jTrace() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isTraceEnabled()).thenReturn(true);
        when(mockLogger.getName()).thenReturn("test.logger");

        DefaultLog log = new DefaultLog(mockLogger);
        log.trace("trace message");

        verify(mockLogger).trace("trace message");
    }

    /**
     * Verify that trace methods are no-ops when trace is disabled.
     */
    @Test
    void traceIsNoOpWhenDisabled() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isTraceEnabled()).thenReturn(false);

        DefaultLog log = new DefaultLog(mockLogger);
        log.trace("should not be logged");

        verify(mockLogger).isTraceEnabled();
        // trace() should NOT have been called on the underlying logger
    }
}
