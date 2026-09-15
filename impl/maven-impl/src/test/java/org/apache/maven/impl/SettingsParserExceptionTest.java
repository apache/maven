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
package org.apache.maven.impl;

import java.util.List;

import org.apache.maven.api.spi.SettingsParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SettingsParserExceptionTest {
    @Test
    void explicitMessageAndLocationArePreserved() {
        var cause = new IllegalArgumentException("Cause message");
        var exception = new SettingsParserException("Parser message", 3, 7, cause);
        assertEquals("Parser message", exception.getMessage());
        assertEquals(3, exception.getLineNumber());
        assertEquals(7, exception.getColumnNumber());
        assertSame(cause, exception.getCause());
        assertEquals("Parser message", new SettingsParserException("Parser message").getMessage());
        assertEquals("Parser message", new SettingsParserException("Parser message", cause).getMessage());
    }

    @Test
    void missingMessageFallsBackToCause() {
        var cause = new IllegalArgumentException("Invalid setting");
        for (var exception : List.of(
                new SettingsParserException(cause),
                new SettingsParserException(null, cause),
                new SettingsParserException(null, 3, 7, cause))) {
            assertEquals("Invalid setting", exception.getMessage());
            assertSame(cause, exception.getCause());
        }
        var located = new SettingsParserException(null, 3, 7, cause);
        assertEquals(3, located.getLineNumber());
        assertEquals(7, located.getColumnNumber());
    }

    @Test
    void missingCauseMessageUsesDefault() {
        var cause = new IllegalArgumentException();
        for (var exception : List.of(
                new SettingsParserException(),
                new SettingsParserException((String) null),
                new SettingsParserException((Throwable) null),
                new SettingsParserException(null, null),
                new SettingsParserException(null, -1, -1, null),
                new SettingsParserException(cause),
                new SettingsParserException(null, cause),
                new SettingsParserException(null, -1, -1, cause))) {
            assertEquals("Unknown settings parsing error", exception.getMessage());
            assertEquals(-1, exception.getLineNumber());
            assertEquals(-1, exception.getColumnNumber());
        }
        assertSame(cause, new SettingsParserException(cause).getCause());
    }
}
