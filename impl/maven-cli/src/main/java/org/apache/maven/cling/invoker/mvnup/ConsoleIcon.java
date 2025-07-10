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

import java.nio.charset.Charset;

import org.jline.terminal.Terminal;

/**
 * Console icons for Maven upgrade tool output.
 * Each icon has a Unicode character and an ASCII fallback.
 * The appropriate representation is chosen based on the terminal's charset capabilities.
 */
public enum ConsoleIcon {
    /**
     * Success/completion icon.
     */
    SUCCESS('✓', "[OK]"),

    /**
     * Error/failure icon.
     */
    ERROR('✗', "[ERROR]"),

    /**
     * Warning icon.
     */
    WARNING('⚠', "[WARNING]"),

    /**
     * Detail/bullet point icon.
     */
    DETAIL('•', "-"),

    /**
     * Action/arrow icon.
     */
    ACTION('→', ">");

    private final char unicodeChar;
    private final String asciiFallback;

    ConsoleIcon(char unicodeChar, String asciiFallback) {
        this.unicodeChar = unicodeChar;
        this.asciiFallback = asciiFallback;
    }

    /**
     * Returns the appropriate icon representation for the given terminal.
     * Tests if the terminal's charset can encode the Unicode character,
     * falling back to ASCII if not.
     *
     * @param terminal the terminal to get the icon for
     * @return the Unicode character if supported, otherwise the ASCII fallback
     */
    public String getIcon(Terminal terminal) {
        Charset charset = getTerminalCharset(terminal);
        return charset.newEncoder().canEncode(unicodeChar) ? String.valueOf(unicodeChar) : asciiFallback;
    }

    /**
     * Gets the charset used by the terminal for output.
     * Falls back to the system default charset if terminal charset is not available.
     *
     * @param terminal the terminal to get the charset from
     * @return the terminal's output charset or the system default charset
     */
    private static Charset getTerminalCharset(Terminal terminal) {
        if (terminal != null && terminal.encoding() != null) {
            return terminal.encoding();
        }
        return Charset.defaultCharset();
    }

    /**
     * Returns the Unicode character for this icon.
     *
     * @return the Unicode character
     */
    public char getUnicodeChar() {
        return unicodeChar;
    }

    /**
     * Returns the ASCII fallback text for this icon.
     *
     * @return the ASCII fallback text
     */
    public String getAsciiFallback() {
        return asciiFallback;
    }
}
