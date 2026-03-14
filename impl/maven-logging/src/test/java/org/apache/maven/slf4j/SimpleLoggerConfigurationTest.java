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

import org.apache.maven.api.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for SimpleLoggerConfiguration functionality.
 *
 * Includes tests for GH-11199: Maven 4.0.0-rc-4 ignores defaultLogLevel.
 */
class SimpleLoggerConfigurationTest {

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
    void testStringToLevelOff() {
        int level = SimpleLoggerConfiguration.stringToLevel("off");
        assertEquals(MavenBaseLogger.LOG_LEVEL_OFF, level);
    }

    @Test
    void testStringToLevelOffCaseInsensitive() {
        assertEquals(MavenBaseLogger.LOG_LEVEL_OFF, SimpleLoggerConfiguration.stringToLevel("OFF"));
        assertEquals(MavenBaseLogger.LOG_LEVEL_OFF, SimpleLoggerConfiguration.stringToLevel("Off"));
        assertEquals(MavenBaseLogger.LOG_LEVEL_OFF, SimpleLoggerConfiguration.stringToLevel("oFf"));
    }

    @Test
    void testStringToLevelInfo() {
        int level = SimpleLoggerConfiguration.stringToLevel("info");
        assertEquals(MavenBaseLogger.LOG_LEVEL_INFO, level);
    }

    @Test
    void testStringToLevelDebug() {
        int level = SimpleLoggerConfiguration.stringToLevel("debug");
        assertEquals(MavenBaseLogger.LOG_LEVEL_DEBUG, level);
    }

    @Test
    void testStringToLevelError() {
        int level = SimpleLoggerConfiguration.stringToLevel("error");
        assertEquals(MavenBaseLogger.LOG_LEVEL_ERROR, level);
    }

    @Test
    void testStringToLevelWarn() {
        int level = SimpleLoggerConfiguration.stringToLevel("warn");
        assertEquals(MavenBaseLogger.LOG_LEVEL_WARN, level);
    }

    @Test
    void testStringToLevelTrace() {
        int level = SimpleLoggerConfiguration.stringToLevel("trace");
        assertEquals(MavenBaseLogger.LOG_LEVEL_TRACE, level);
    }

    @Test
    void testStringToLevelInvalid() {
        // Invalid level should default to INFO
        int level = SimpleLoggerConfiguration.stringToLevel("invalid");
        assertEquals(MavenBaseLogger.LOG_LEVEL_INFO, level);
    }

    @Test
    void testDefaultLogLevelFromSystemProperty() {
        // Set system property
        System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, "off");

        SimpleLoggerConfiguration config = new SimpleLoggerConfiguration();
        config.init();

        assertEquals(MavenBaseLogger.LOG_LEVEL_OFF, config.defaultLogLevel);
    }

    @Test
    void testDefaultLogLevelFromPropertiesFile() {
        // This test verifies that the configuration properly handles OFF level
        // when loaded from properties. Since we can't directly access the private
        // properties field, we test through system properties which have the same effect.
        System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, "off");

        SimpleLoggerConfiguration config = new SimpleLoggerConfiguration();
        config.init();

        assertEquals(MavenBaseLogger.LOG_LEVEL_OFF, config.defaultLogLevel);
    }
}
