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
package org.apache.maven.jline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;

/**
 * Colorized message support. Maven only needs ANSI colors in its log output, so there is no terminal library here:
 * styles are emitted as plain SGR escape sequences, and only when color is enabled.
 */
public class MessageUtils {

    static MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
    static volatile boolean tty;
    static volatile boolean colorEnabled;

    /**
     * Detects whether standard output is an ANSI capable terminal and enables color accordingly. Only the CLI calls
     * this; embedded use never does, so "auto" color stays off there.
     */
    public static void systemInstall() {
        tty = detectAnsiTerminal();
        colorEnabled = tty;
        width = 0;
    }

    /**
     * Returns {@code true} if {@link #systemInstall()} detected an ANSI capable terminal on standard output.
     */
    public static boolean isTty() {
        return tty;
    }

    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }

    public static boolean isColorEnabled() {
        return colorEnabled;
    }

    /**
     * Returns the terminal width in columns, or {@code -1} if unknown. Taken from the {@code COLUMNS} environment
     * variable if set, otherwise (on a detected non-Windows terminal) from {@code stty size}. Computed once.
     */
    public static int getTerminalWidth() {
        int w = width;
        if (w == 0) {
            w = detectWidth();
            width = w;
        }
        return w;
    }

    static volatile int width; // 0 = not yet detected

    private static int detectWidth() {
        int columns = parseWidth(System.getenv("COLUMNS"));
        if (columns > 0) {
            return columns;
        }
        if (!tty || System.getProperty("os.name", "").startsWith("Windows")) {
            return -1;
        }
        // ponytail: read once, window resizes are not tracked; re-query periodically if a consumer ever needs it
        try {
            Process p = new ProcessBuilder("stty", "size")
                    .redirectInput(new File("/dev/tty"))
                    .redirectError(new File("/dev/null"))
                    .start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String size = r.readLine(); // "rows cols"
                p.waitFor();
                return size == null ? -1 : parseWidth(size.substring(size.indexOf(' ') + 1));
            }
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    static int parseWidth(String s) {
        try {
            int w = s == null ? -1 : Integer.parseInt(s.trim());
            return w > 0 ? w : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static MessageBuilder builder() {
        return messageBuilderFactory.builder();
    }

    /**
     * Remove any ANSI code from a message (colors or other escape sequences).
     *
     * @param msg message eventually containing ANSI codes
     * @return the message with ANSI codes removed
     */
    public static String stripAnsiCodes(String msg) {
        return msg.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }

    static boolean detectAnsiTerminal() {
        if ("dumb".equals(System.getenv("TERM"))) {
            return false;
        }
        Console console = System.console();
        if (console == null) {
            return false; // stdin or stdout is redirected
        }
        try {
            // Java 22+ returns a console even when redirected; isTerminal() tells the truth
            Method isTerminal = Console.class.getMethod("isTerminal");
            if (!(Boolean) isTerminal.invoke(console)) {
                return false;
            }
        } catch (ReflectiveOperationException e) {
            // Java < 22: a non-null console already means a terminal
        }
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            // ponytail: no native call to switch on VT processing, so trust only consoles known to do ANSI already
            // (Windows Terminal, ConEmu/Cmder, ANSICON, VS Code, mintty & co); legacy conhost users use --color=always
            return System.getenv("WT_SESSION") != null
                    || "ON".equals(System.getenv("ConEmuANSI"))
                    || System.getenv("ANSICON") != null
                    || "vscode".equals(System.getenv("TERM_PROGRAM"))
                    || System.getenv("TERM") != null;
        }
        return true;
    }
}
