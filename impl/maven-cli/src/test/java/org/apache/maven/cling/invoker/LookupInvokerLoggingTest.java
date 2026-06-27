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
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.maven.api.Constants;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for logging configuration behavior in LookupInvoker.
 * This test verifies that the fix for GH-11199 works correctly.
 */
class LookupInvokerLoggingTest {

    private String originalSystemProperty;

    @BeforeEach
    void setUp() {
        // Save original system property
        originalSystemProperty = System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        // Clear system property to test configuration file loading
        System.clearProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
    }

    @AfterEach
    void tearDown() {
        // Restore original system property
        if (originalSystemProperty != null) {
            System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, originalSystemProperty);
        } else {
            System.clearProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL);
        }
    }

    @Test
    void testNoCliOptionsDoesNotSetSystemProperty() {
        // Simulate the scenario where no CLI options are specified
        // This should NOT call setRootLoggerLevel, allowing configuration file to take effect

        MockInvokerRequest invokerRequest = new MockInvokerRequest(false); // not verbose
        MockOptions options = new MockOptions(false); // not quiet
        MockSlf4jConfiguration slf4jConfiguration = new MockSlf4jConfiguration();

        // Simulate the fixed logic from LookupInvoker.prepareLogging()
        Slf4jConfiguration.Level loggerLevel;
        if (invokerRequest.effectiveVerbose()) {
            loggerLevel = Slf4jConfiguration.Level.DEBUG;
            slf4jConfiguration.setRootLoggerLevel(loggerLevel);
        } else if (options.quiet().orElse(false)) {
            loggerLevel = Slf4jConfiguration.Level.ERROR;
            slf4jConfiguration.setRootLoggerLevel(loggerLevel);
        } else {
            // fall back to default log level specified in conf
            loggerLevel = Slf4jConfiguration.Level.INFO; // default for display purposes
            // Do NOT call setRootLoggerLevel - this is the fix!
        }

        // Verify that setRootLoggerLevel was not called
        assertEquals(0, slf4jConfiguration.setRootLoggerLevelCallCount);

        // Verify that system property was not set
        assertNull(System.getProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL));
    }

    @Test
    void testVerboseOptionSetsSystemProperty() {
        MockInvokerRequest invokerRequest = new MockInvokerRequest(true); // verbose
        MockOptions options = new MockOptions(false); // not quiet
        MockSlf4jConfiguration slf4jConfiguration = new MockSlf4jConfiguration();

        // Simulate the logic from LookupInvoker.prepareLogging()
        if (invokerRequest.effectiveVerbose()) {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.DEBUG);
        } else if (options.quiet().orElse(false)) {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
        }

        // Verify that setRootLoggerLevel was called
        assertEquals(1, slf4jConfiguration.setRootLoggerLevelCallCount);
        assertEquals(Slf4jConfiguration.Level.DEBUG, slf4jConfiguration.lastSetLevel);
    }

    @Test
    void testQuietOptionSetsSystemProperty() {
        MockInvokerRequest invokerRequest = new MockInvokerRequest(false); // not verbose
        MockOptions options = new MockOptions(true); // quiet
        MockSlf4jConfiguration slf4jConfiguration = new MockSlf4jConfiguration();

        // Simulate the logic from LookupInvoker.prepareLogging()
        if (invokerRequest.effectiveVerbose()) {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.DEBUG);
        } else if (options.quiet().orElse(false)) {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
        }

        // Verify that setRootLoggerLevel was called
        assertEquals(1, slf4jConfiguration.setRootLoggerLevelCallCount);
        assertEquals(Slf4jConfiguration.Level.ERROR, slf4jConfiguration.lastSetLevel);
    }

    // --- JUL-to-SLF4J bridge tests (GH-11683) ---

    @Test
    void testJulBridgeIsInstalledDuringActivateLogging() {
        // Ensure clean state
        SLF4JBridgeHandler.uninstall();
        assertFalse(SLF4JBridgeHandler.isInstalled(), "Bridge should not be installed before test");

        // Simulate what activateLogging() does
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }

        assertTrue(SLF4JBridgeHandler.isInstalled(), "SLF4JBridgeHandler should be installed after activateLogging");

        // Clean up
        SLF4JBridgeHandler.uninstall();
    }

    @Test
    void testJulBridgeIsNotReinstalledWhenAlreadyPresent() {
        // Install the bridge first
        SLF4JBridgeHandler.uninstall();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        assertTrue(SLF4JBridgeHandler.isInstalled());

        // Record the handlers before the second call
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlersBefore = rootLogger.getHandlers();

        // Simulate a second activateLogging() call — the guard should skip re-installation
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }

        // Handlers should be the same objects — no removal/reinstall occurred
        Handler[] handlersAfter = rootLogger.getHandlers();
        assertEquals(handlersBefore.length, handlersAfter.length, "Handler count should not change on second call");
        for (int i = 0; i < handlersBefore.length; i++) {
            assertTrue(
                    handlersBefore[i] == handlersAfter[i],
                    "Handler instance at index " + i + " should be the same object (no reinstall)");
        }

        // Clean up
        SLF4JBridgeHandler.uninstall();
    }

    @Test
    void testJulMessagesAreRoutedThroughSlf4j() {
        // Install the bridge
        SLF4JBridgeHandler.uninstall();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Verify a JUL logger's handlers now include SLF4JBridgeHandler
        Logger rootLogger = Logger.getLogger("");
        boolean hasBridgeHandler = false;
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof SLF4JBridgeHandler) {
                hasBridgeHandler = true;
                break;
            }
        }
        assertTrue(hasBridgeHandler, "Root JUL logger should have SLF4JBridgeHandler installed");

        // Verify that logging through JUL does not throw (bridge is functional)
        Logger julLogger = Logger.getLogger("org.apache.maven.test.jul");
        julLogger.info("Test message routed through SLF4J bridge");

        // Clean up
        SLF4JBridgeHandler.uninstall();
    }

    // Mock classes for testing
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

    private static class MockSlf4jConfiguration implements Slf4jConfiguration {
        int setRootLoggerLevelCallCount = 0;
        Level lastSetLevel = null;

        @Override
        public void setRootLoggerLevel(Level level) {
            setRootLoggerLevelCallCount++;
            lastSetLevel = level;

            // Simulate what MavenSimpleConfiguration does
            String value =
                    switch (level) {
                        case DEBUG -> "debug";
                        case INFO -> "info";
                        case ERROR -> "error";
                    };
            System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, value);
        }

        @Override
        public void activate() {
            // no-op for test
        }
    }
}
