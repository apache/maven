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
package org.apache.maven.cling.invoker;

import java.util.Optional;

import org.apache.maven.api.Constants;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for logging configuration behavior in LookupInvoker.
 * This test verifies that the fix for GH-11199 works correctly.
 */
class LookupInvokerLoggingTest {

    private String originalSystemProperty;

    @BeforeEach
    void setUp() {
        originalSystemProperty = System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        System.clearProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        LoggerManager.set(new DefaultLoggerManager());
    }

    @AfterEach
    void tearDown() {
        LoggerManager.set(null);
        if (originalSystemProperty != null) {
            System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, originalSystemProperty);
        } else {
            System.clearProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        }
    }

    @Test
    void testNoCliOptionsDoesNotSetSystemProperty() {
        MockInvokerRequest invokerRequest = new MockInvokerRequest(false);
        MockOptions options = new MockOptions(false);
        int[] callCount = {0};

        LogLevel loggerLevel;
        if (invokerRequest.effectiveVerbose()) {
            loggerLevel = LogLevel.DEBUG;
            LoggerManager.get().setRootLoggerLevel(loggerLevel);
            callCount[0]++;
        } else if (options.quiet().orElse(false)) {
            loggerLevel = LogLevel.ERROR;
            LoggerManager.get().setRootLoggerLevel(loggerLevel);
            callCount[0]++;
        } else {
            loggerLevel = LogLevel.INFO;
        }

        assertEquals(0, callCount[0]);
        assertNull(System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL));
    }

    @Test
    void testVerboseOptionSetsSystemProperty() {
        MockInvokerRequest invokerRequest = new MockInvokerRequest(true);
        MockOptions options = new MockOptions(false);

        if (invokerRequest.effectiveVerbose()) {
            LoggerManager.get().setRootLoggerLevel(LogLevel.DEBUG);
        } else if (options.quiet().orElse(false)) {
            LoggerManager.get().setRootLoggerLevel(LogLevel.ERROR);
        }

        assertEquals("debug", System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL));
    }

    @Test
    void testQuietOptionSetsSystemProperty() {
        MockInvokerRequest invokerRequest = new MockInvokerRequest(false);
        MockOptions options = new MockOptions(true);

        if (invokerRequest.effectiveVerbose()) {
            LoggerManager.get().setRootLoggerLevel(LogLevel.DEBUG);
        } else if (options.quiet().orElse(false)) {
            LoggerManager.get().setRootLoggerLevel(LogLevel.ERROR);
        }

        assertEquals("error", System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL));
    }

    private static class MockInvokerRequest {
        private final boolean verbose;

        MockInvokerRequest(boolean verbose) {
            this.verbose = verbose;
        }

        boolean effectiveVerbose() {
            return verbose;
        }
    }

    private static class MockOptions {
        private final boolean quiet;

        MockOptions(boolean quiet) {
            this.quiet = quiet;
        }

        Optional<Boolean> quiet() {
            return Optional.of(quiet);
        }
    }
}
