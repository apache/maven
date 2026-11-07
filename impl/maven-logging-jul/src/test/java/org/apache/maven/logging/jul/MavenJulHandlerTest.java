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
package org.apache.maven.logging.jul;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.maven.api.logging.LogLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenJulHandlerTest {

    @ParameterizedTest(name = "JUL {0} -> LogLevel.{1}")
    @CsvSource({
        "FINEST, TRACE",
        "FINER, DEBUG",
        "FINE, DEBUG",
        "CONFIG, INFO",
        "INFO, INFO",
        "WARNING, WARN",
        "SEVERE, ERROR",
    })
    void julLevelMapsToCorrectLogLevel(String julLevelName, String expectedLogLevel) {
        Level julLevel = Level.parse(julLevelName);
        LogLevel actual = MavenJulHandler.julLevelToLogLevel(julLevel);
        assertEquals(LogLevel.valueOf(expectedLogLevel), actual);
    }

    @Test
    void finestMapsToTrace() {
        assertEquals(LogLevel.TRACE, MavenJulHandler.julLevelToLogLevel(Level.FINEST));
    }

    @Test
    void configMapsToInfo() {
        assertEquals(LogLevel.INFO, MavenJulHandler.julLevelToLogLevel(Level.CONFIG));
    }

    @Test
    void publishIsReentrantSafe() {
        MavenJulHandler handler = new MavenJulHandler();
        LogRecord record = new LogRecord(Level.INFO, "nested");

        MavenJulHandler.setInPublishForTest(true);
        try {
            handler.publish(record);
        } finally {
            MavenJulHandler.setInPublishForTest(false);
        }
    }
}
