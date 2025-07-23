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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.maven.api.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenBaseLoggerTimestampTest {
    private ByteArrayOutputStream logOutput;
    private PrintStream originalErr;
    private static final String LOGGER_NAME = "test.logger";
    private MavenBaseLogger logger;

    @BeforeEach
    void setUp() {
        // Reset configuration before each test
        System.clearProperty(Constants.MAVEN_LOGGER_SHOW_DATE_TIME);
        System.clearProperty(Constants.MAVEN_LOGGER_DATE_TIME_FORMAT);

        // Reset static initialization flag
        MavenBaseLogger.initialized = false;

        // Capture System.err
        logOutput = new ByteArrayOutputStream();
        originalErr = System.err;
        System.setErr(new PrintStream(logOutput));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
        System.clearProperty(Constants.MAVEN_LOGGER_SHOW_DATE_TIME);
        System.clearProperty(Constants.MAVEN_LOGGER_DATE_TIME_FORMAT);
        MavenBaseLogger.initialized = false;
    }

    @Test
    void whenShowDateTimeIsFalseShouldNotIncludeTimestamp() {
        // Given
        System.setProperty(Constants.MAVEN_LOGGER_SHOW_DATE_TIME, "false");
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        assertTrue(
                output.matches("^\\[main\\] INFO test.logger - Test message$"),
                "Should not include timestamp but was: " + output);
    }

    @Test
    void whenShowDateTimeIsTrueWithoutFormatShouldShowElapsedTime() { // Changed test name and expectation
        // Given
        System.setProperty(Constants.MAVEN_LOGGER_SHOW_DATE_TIME, "true");
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        assertTrue(
                output.matches("^\\d+ \\[main\\] INFO test.logger - Test message$"),
                "Should show elapsed time when no format specified but was: " + output);
    }

    @ParameterizedTest
    @ValueSource(strings = {"yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss.SSS", "HH:mm:ss"})
    void whenCustomDateFormatShouldFormatCorrectly(String dateFormat) {
        // Given
        System.setProperty(Constants.MAVEN_LOGGER_SHOW_DATE_TIME, "true");
        System.setProperty(Constants.MAVEN_LOGGER_DATE_TIME_FORMAT, dateFormat);
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        String patternStr = dateFormat
                .replace("yyyy", "\\d{4}")
                .replace("MM", "\\d{2}")
                .replace("dd", "\\d{2}")
                .replace("HH", "\\d{2}")
                .replace("mm", "\\d{2}")
                .replace("ss", "\\d{2}")
                .replace("SSS", "\\d{3}")
                .replace("/", "\\/")
                .replace(".", "\\.");

        assertTrue(
                output.matches("^" + patternStr + " \\[main\\] INFO test.logger - Test message$"),
                "Should match custom date format but was: " + output);
    }

    @Test
    void whenInvalidDateFormatShouldUseElapsedMillis() {
        // Given
        System.setProperty(Constants.MAVEN_LOGGER_SHOW_DATE_TIME, "true");
        System.setProperty(Constants.MAVEN_LOGGER_DATE_TIME_FORMAT, "invalid-format");
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        assertTrue(
                output.matches("^\\d+ \\[main\\] INFO test.logger - Test message$"),
                "Should show elapsed milliseconds when format is invalid but was: " + output);
    }

    private void initializeLogger() {
        MavenBaseLogger.CONFIG_PARAMS.init();
        logger = new MavenBaseLogger(LOGGER_NAME);
        logOutput.reset();
    }

    private String getLastLine(String output) {
        String[] lines = output.split("\\R");
        return lines[lines.length - 1].trim();
    }
}
