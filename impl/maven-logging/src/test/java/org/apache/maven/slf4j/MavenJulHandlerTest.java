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
package org.apache.maven.slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.spi.LocationAwareLogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link MavenJulHandler}, focused on the JUL→SLF4J level
 * mapping and metadata lifecycle.
 */
class MavenJulHandlerTest {

    /**
     * Table test for JUL level → SLF4J level mapping.
     * Verifies the mapping documented in the class Javadoc.
     */
    @ParameterizedTest(name = "JUL {0} -> SLF4J level {1}")
    @CsvSource({
        "FINEST, 0", // TRACE_INT = 0
        "FINER, 10", // DEBUG_INT = 10
        "FINE, 10", // DEBUG_INT = 10
        "CONFIG, 20", // INFO_INT = 20
        "INFO, 20", // INFO_INT = 20
        "WARNING, 30", // WARN_INT = 30
        "SEVERE, 40", // ERROR_INT = 40
    })
    void julLevelMapsToCorrectSlf4jLevel(String julLevelName, int expectedSlf4jLevel) throws Exception {
        Level julLevel = Level.parse(julLevelName);
        int actual = invokeJulLevelToSlf4j(julLevel);
        assertEquals(
                expectedSlf4jLevel, actual, "JUL " + julLevelName + " should map to SLF4J level " + expectedSlf4jLevel);
    }

    /**
     * Verify that FINEST maps to TRACE (not DEBUG) — this is the key
     * distinction for the TRACE/DEBUG separation.
     */
    @Test
    void finestMapsToTrace() throws Exception {
        assertEquals(
                LocationAwareLogger.TRACE_INT,
                invokeJulLevelToSlf4j(Level.FINEST),
                "FINEST should map to TRACE, not DEBUG");
    }

    /**
     * Verify that CONFIG maps to INFO (not DEBUG) — CONFIG is JUL's
     * informational level for static configuration, not a debug level.
     */
    @Test
    void configMapsToInfo() throws Exception {
        assertEquals(LocationAwareLogger.INFO_INT, invokeJulLevelToSlf4j(Level.CONFIG), "CONFIG should map to INFO");
    }

    /**
     * Verify that JUL metadata is null when no log event is being processed.
     */
    @Test
    void julMetadataIsNullOutsidePublish() {
        assertNull(MavenJulHandler.getJulMetadata(), "JUL metadata should be null outside a publish() call");
    }

    /**
     * Invoke the private julLevelToSlf4j method via reflection for testing.
     */
    private static int invokeJulLevelToSlf4j(Level julLevel) throws Exception {
        try {
            Method method = MavenJulHandler.class.getDeclaredMethod("julLevelToSlf4j", Level.class);
            method.setAccessible(true);
            return (int) method.invoke(null, julLevel);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
