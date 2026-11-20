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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.api.logging.LogEventBuilder;
import org.apache.maven.api.logging.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.spi.LoggingEventBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    // ---- Fluent builder: key-value pairs ----

    @Test
    void fluentBuilderWithKeyValueDelegatesToSlf4jAddKeyValue() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        LoggingEventBuilder mockBuilder = mock(LoggingEventBuilder.class);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(mockLogger.atInfo()).thenReturn(mockBuilder);
        when(mockBuilder.addKeyValue("module", "compiler")).thenReturn(mockBuilder);
        when(mockBuilder.addKeyValue("count", 3L)).thenReturn(mockBuilder);

        Logger log = new DefaultLogger(mockLogger);
        log.atInfo()
                .withKeyValue("module", "compiler")
                .withKeyValue("count", 3L)
                .log("Building {} modules", 3);

        verify(mockBuilder).addKeyValue("module", "compiler");
        verify(mockBuilder).addKeyValue("count", 3L);
        verify(mockBuilder).log("Building {} modules", 3);
    }

    @Test
    void fluentBuilderBooleanKeyValueDelegatesToSlf4j() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        LoggingEventBuilder mockBuilder = mock(LoggingEventBuilder.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(mockLogger.atDebug()).thenReturn(mockBuilder);
        when(mockBuilder.addKeyValue("incremental", true)).thenReturn(mockBuilder);

        Logger log = new DefaultLogger(mockLogger);
        log.atDebug().withKeyValue("incremental", true).log("Compile");

        verify(mockBuilder).addKeyValue("incremental", true);
        verify(mockBuilder).log("Compile");
    }

    // ---- NOP builder laziness ----

    @Test
    void nopBuilderDoesNotInvokeMessageSupplier() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        when(mockLogger.isTraceEnabled()).thenReturn(false);

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Logger log = new DefaultLogger(mockLogger);
        log.atTrace().log(() -> {
            supplierCalled.set(true);
            return "expensive message";
        });

        assertFalse(supplierCalled.get(), "Supplier must not be called when TRACE is disabled");
    }

    @Test
    void nopBuilderReturnsSameInstanceForAllChainedCalls() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        Logger log = new DefaultLogger(mockLogger);
        LogEventBuilder b1 = log.atDebug();
        LogEventBuilder b2 = b1.withKeyValue("k", "v");
        LogEventBuilder b3 = b2.withKeyValue("n", 42L);
        LogEventBuilder b4 = b3.withThrowable(null);

        assertSame(b1, b2, "NOP builder must return itself from withKeyValue(String,String)");
        assertSame(b2, b3, "NOP builder must return itself from withKeyValue(String,long)");
        assertSame(b3, b4, "NOP builder must return itself from withThrowable");
    }

    // ---- withThrowable after builder already initialised ----

    @Test
    void withThrowableAfterKeyValueSetsSlf4jCause() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        LoggingEventBuilder mockBuilder = mock(LoggingEventBuilder.class);
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        when(mockLogger.atError()).thenReturn(mockBuilder);
        when(mockBuilder.addKeyValue("phase", "compile")).thenReturn(mockBuilder);
        when(mockBuilder.setCause(org.mockito.ArgumentMatchers.any())).thenReturn(mockBuilder);

        RuntimeException ex = new RuntimeException("fail");
        Logger log = new DefaultLogger(mockLogger);
        log.atError()
                .withKeyValue("phase", "compile") // triggers ensureBuilder() → slf4jBuilder is now set
                .withThrowable(ex) // must call slf4jBuilder.setCause(ex), not store in field
                .log("Build failed");

        verify(mockBuilder).setCause(ex);
        verify(mockBuilder).log("Build failed");
    }

    @Test
    void withThrowableBeforeKeyValueStoresAndPassesToBuilder() {
        org.slf4j.Logger mockLogger = mock(org.slf4j.Logger.class);
        LoggingEventBuilder mockBuilder = mock(LoggingEventBuilder.class);
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        when(mockLogger.atWarn()).thenReturn(mockBuilder);
        when(mockBuilder.addKeyValue("key", "val")).thenReturn(mockBuilder);
        when(mockBuilder.setCause(org.mockito.ArgumentMatchers.any())).thenReturn(mockBuilder);

        RuntimeException ex = new RuntimeException("warn");
        Logger log = new DefaultLogger(mockLogger);
        log.atWarn()
                .withThrowable(ex) // stored in field — builder not yet initialised
                .withKeyValue("key", "val") // triggers ensureBuilder() → must pass stored throwable
                .log("Warning");

        // The throwable is passed via setCause inside ensureBuilder() when slf4jBuilder is first created
        verify(mockBuilder).setCause(ex);
        verify(mockBuilder).addKeyValue("key", "val");
        verify(mockBuilder).log("Warning");
    }

    // ---- ModelProcessor bridge ----

    @Test
    void modelProcessorReadBridgeUsesStrictTrueByDefault() throws Exception {
        // Verify that the default read(Source, Map) bridge passes strict=true when no option is set
        // by checking the deprecated read(XmlReaderRequest) path is invoked with isStrict()==true.
        // We use a test double that records the request rather than actually reading XML.
        final boolean[] strictSeen = {false};
        org.apache.maven.api.services.model.ModelProcessor processor =
                new org.apache.maven.api.services.model.ModelProcessor() {
                    @Override
                    public java.nio.file.Path locateExistingPom(java.nio.file.Path project) {
                        return null;
                    }

                    @Override
                    public org.apache.maven.api.model.Model read(
                            org.apache.maven.api.services.xml.XmlReaderRequest request)
                            throws java.io.IOException, org.apache.maven.api.services.xml.XmlReaderException {
                        strictSeen[0] = request.isStrict();
                        return org.apache.maven.api.model.Model.newInstance();
                    }
                };

        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("pom", ".xml");
        try {
            java.nio.file.Files.writeString(tmp, "<project/>"); // minimal content to satisfy the bridge
            org.apache.maven.api.services.Source source = org.apache.maven.api.services.Source.fromPath(tmp);
            try {
                processor.read(source, null); // null options → strict defaults to true
            } catch (Exception ignored) {
                // XML parse failure is expected for this stub content; we only care about strict
            }
            assert strictSeen[0] : "strict must be true when options is null";
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    @Test
    void modelProcessorReadBridgeRespectsStrictFalseOption() throws Exception {
        final boolean[] strictSeen = {true};
        org.apache.maven.api.services.model.ModelProcessor processor =
                new org.apache.maven.api.services.model.ModelProcessor() {
                    @Override
                    public java.nio.file.Path locateExistingPom(java.nio.file.Path project) {
                        return null;
                    }

                    @Override
                    public org.apache.maven.api.model.Model read(
                            org.apache.maven.api.services.xml.XmlReaderRequest request)
                            throws java.io.IOException, org.apache.maven.api.services.xml.XmlReaderException {
                        strictSeen[0] = request.isStrict();
                        return org.apache.maven.api.model.Model.newInstance();
                    }
                };

        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("pom", ".xml");
        try {
            java.nio.file.Files.writeString(tmp, "<project/>");
            org.apache.maven.api.services.Source source = org.apache.maven.api.services.Source.fromPath(tmp);
            java.util.Map<String, Object> options =
                    java.util.Map.of(org.apache.maven.api.spi.ModelParser.STRICT, "false");
            try {
                processor.read(source, options);
            } catch (Exception ignored) {
            }
            assertFalse(strictSeen[0], "strict must be false when options contains \"strict\"=\"false\"");
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }
}
