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
package org.apache.maven.cling.invoker.mvnup;

import java.nio.charset.StandardCharsets;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ConsoleIcon} enum.
 * Tests icon rendering with different terminal charsets and fallback behavior.
 */
@DisplayName("ConsoleIcon")
class ConsoleIconTest {

    @Test
    @DisplayName("should return Unicode icons when terminal supports UTF-8")
    void shouldReturnUnicodeWhenTerminalSupportsUtf8() {
        Terminal mockTerminal = mock(Terminal.class);
        when(mockTerminal.encoding()).thenReturn(StandardCharsets.UTF_8);

        assertEquals("✓", ConsoleIcon.SUCCESS.getIcon(mockTerminal));
        assertEquals("✗", ConsoleIcon.ERROR.getIcon(mockTerminal));
        assertEquals("⚠", ConsoleIcon.WARNING.getIcon(mockTerminal));
        assertEquals("•", ConsoleIcon.DETAIL.getIcon(mockTerminal));
        assertEquals("→", ConsoleIcon.ACTION.getIcon(mockTerminal));
    }

    @Test
    @DisplayName("should return ASCII fallback when terminal uses US-ASCII")
    void shouldReturnAsciiFallbackWhenTerminalUsesAscii() {
        Terminal mockTerminal = mock(Terminal.class);
        when(mockTerminal.encoding()).thenReturn(StandardCharsets.US_ASCII);

        assertEquals("[OK]", ConsoleIcon.SUCCESS.getIcon(mockTerminal));
        assertEquals("[ERROR]", ConsoleIcon.ERROR.getIcon(mockTerminal));
        assertEquals("[WARNING]", ConsoleIcon.WARNING.getIcon(mockTerminal));
        assertEquals("-", ConsoleIcon.DETAIL.getIcon(mockTerminal));
        assertEquals(">", ConsoleIcon.ACTION.getIcon(mockTerminal));
    }

    @Test
    @DisplayName("should handle null terminal gracefully")
    void shouldHandleNullTerminal() {
        // Should fall back to system default charset
        for (ConsoleIcon icon : ConsoleIcon.values()) {
            String result = icon.getIcon(null);
            assertNotNull(result, "Icon result should not be null for " + icon);

            // Result should be either Unicode or ASCII fallback depending on default charset
            String expectedUnicode = String.valueOf(icon.getUnicodeChar());
            String expectedAscii = icon.getAsciiFallback();
            assertTrue(
                    result.equals(expectedUnicode) || result.equals(expectedAscii),
                    "Result should be either Unicode or ASCII fallback for " + icon + ", got: " + result);
        }
    }

    @Test
    @DisplayName("should handle terminal with null encoding")
    void shouldHandleTerminalWithNullEncoding() {
        Terminal mockTerminal = mock(Terminal.class);
        when(mockTerminal.encoding()).thenReturn(null);

        // Should fall back to system default charset
        for (ConsoleIcon icon : ConsoleIcon.values()) {
            String result = icon.getIcon(mockTerminal);
            assertNotNull(result, "Icon result should not be null for " + icon);

            // Result should be either Unicode or ASCII fallback depending on default charset
            String expectedUnicode = String.valueOf(icon.getUnicodeChar());
            String expectedAscii = icon.getAsciiFallback();
            assertTrue(
                    result.equals(expectedUnicode) || result.equals(expectedAscii),
                    "Result should be either Unicode or ASCII fallback for " + icon + ", got: " + result);
        }
    }

    @Test
    @DisplayName("should return correct Unicode characters")
    void shouldReturnCorrectUnicodeCharacters() {
        assertEquals('✓', ConsoleIcon.SUCCESS.getUnicodeChar());
        assertEquals('✗', ConsoleIcon.ERROR.getUnicodeChar());
        assertEquals('⚠', ConsoleIcon.WARNING.getUnicodeChar());
        assertEquals('•', ConsoleIcon.DETAIL.getUnicodeChar());
        assertEquals('→', ConsoleIcon.ACTION.getUnicodeChar());
    }

    @Test
    @DisplayName("should return correct ASCII fallbacks")
    void shouldReturnCorrectAsciiFallbacks() {
        assertEquals("[OK]", ConsoleIcon.SUCCESS.getAsciiFallback());
        assertEquals("[ERROR]", ConsoleIcon.ERROR.getAsciiFallback());
        assertEquals("[WARNING]", ConsoleIcon.WARNING.getAsciiFallback());
        assertEquals("-", ConsoleIcon.DETAIL.getAsciiFallback());
        assertEquals(">", ConsoleIcon.ACTION.getAsciiFallback());
    }

    @Test
    @DisplayName("should handle different charset encodings correctly")
    void shouldHandleDifferentCharsetEncodingsCorrectly() {
        Terminal mockTerminal = mock(Terminal.class);

        // Test with ISO-8859-1 (Latin-1) - should support some but not all Unicode chars
        when(mockTerminal.encoding()).thenReturn(StandardCharsets.ISO_8859_1);

        for (ConsoleIcon icon : ConsoleIcon.values()) {
            String result = icon.getIcon(mockTerminal);
            assertNotNull(result, "Icon result should not be null for " + icon);

            // Result should be consistent with charset's canEncode capability
            boolean canEncode = StandardCharsets.ISO_8859_1.newEncoder().canEncode(icon.getUnicodeChar());
            String expected = canEncode ? String.valueOf(icon.getUnicodeChar()) : icon.getAsciiFallback();
            assertEquals(expected, result, "Icon should match charset encoding capability for " + icon);
        }
    }

    @Test
    @DisplayName("should be consistent across multiple calls")
    void shouldBeConsistentAcrossMultipleCalls() {
        Terminal mockTerminal = mock(Terminal.class);
        when(mockTerminal.encoding()).thenReturn(StandardCharsets.UTF_8);

        for (ConsoleIcon icon : ConsoleIcon.values()) {
            String firstCall = icon.getIcon(mockTerminal);
            String secondCall = icon.getIcon(mockTerminal);
            assertEquals(firstCall, secondCall, "Icon should be consistent across calls for " + icon);
        }
    }
}
