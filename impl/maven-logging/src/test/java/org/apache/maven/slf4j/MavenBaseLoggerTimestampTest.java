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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

class MavenBaseLoggerTimestampTest {
    private ByteArrayOutputStream logOutput;
    private PrintStream originalErr;
    private static final String LOGGER_NAME = "test.logger";
    private MavenBaseLogger logger;

    @BeforeEach
    void setUp() {
        // Reset configuration before each test
        System.clearProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY);
        System.clearProperty(MavenBaseLogger.DATE_TIME_FORMAT_KEY);

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
        System.clearProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY);
        System.clearProperty(MavenBaseLogger.DATE_TIME_FORMAT_KEY);
        MavenBaseLogger.initialized = false;
    }

    @Test
    void whenShowDateTimeIsFalse_shouldNotIncludeTimestamp() {
        // Given
        System.setProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY, "false");
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        assertThat(
                "Should not include timestamp", output, matchesPattern("^\\[main\\] INFO test.logger - Test message$"));
    }

    @Test
    void whenShowDateTimeIsTrue_withoutFormat_shouldShowElapsedTime() { // Changed test name and expectation
        // Given
        System.setProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY, "true");
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        assertThat(
                "Should show elapsed time when no format specified",
                output,
                matchesPattern("^\\d+ \\[main\\] INFO test.logger - Test message$"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss.SSS", "HH:mm:ss"})
    void whenCustomDateFormat_shouldFormatCorrectly(String dateFormat) {
        // Given
        System.setProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(MavenBaseLogger.DATE_TIME_FORMAT_KEY, dateFormat);
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

        assertThat(
                "Should match custom date format",
                output,
                matchesPattern("^" + patternStr + " \\[main\\] INFO test.logger - Test message$"));
    }

    @Test
    void whenInvalidDateFormat_shouldUseElapsedMillis() {
        // Given
        System.setProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(MavenBaseLogger.DATE_TIME_FORMAT_KEY, "invalid-format");
        initializeLogger();

        // When
        logger.info("Test message");
        String output = getLastLine(logOutput.toString());

        // Then
        assertThat(
                "Should show elapsed milliseconds when format is invalid",
                output,
                matchesPattern("^\\d+ \\[main\\] INFO test.logger - Test message$"));
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
