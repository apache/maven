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

import org.apache.maven.api.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultLogger}.
 */
class DefaultLoggerTest {

    @Test
    void warnWithSupplierAndThrowableDelegatesToWarn() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        when(mockLogger.isWarnEnabled()).thenReturn(true);

        Logger log = new DefaultLogger(mockLogger);
        RuntimeException ex = new RuntimeException("test");
        log.warn(() -> "warning message", ex);

        verify(mockLogger).warn("warning message", ex);
    }

    @Test
    void traceMethodsDelegateToSlf4jTrace() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        when(mockLogger.isTraceEnabled()).thenReturn(true);

        Logger log = new DefaultLogger(mockLogger);
        log.trace("trace message");

        verify(mockLogger).trace("trace message");
    }

    @Test
    void traceIsNoOpWhenDisabled() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        when(mockLogger.isTraceEnabled()).thenReturn(false);

        Logger log = new DefaultLogger(mockLogger);
        log.trace("should not be logged");

        verify(mockLogger).isTraceEnabled();
        verifyNoMoreInteractions(mockLogger);
    }

    @Test
    void childCreatesChildLoggerWithAppendedName() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        when(mockLogger.getName()).thenReturn("org.example.MyMojo");

        Logger log = new DefaultLogger(mockLogger);
        Logger child = log.child("sub");

        assertNotSame(log, child);
        assertEquals("org.example.MyMojo.sub", child.name());
    }

    @Test
    void childRejectsEmptyName() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        Logger log = new DefaultLogger(mockLogger);

        assertThrows(IllegalArgumentException.class, () -> log.child(""));
    }
}
