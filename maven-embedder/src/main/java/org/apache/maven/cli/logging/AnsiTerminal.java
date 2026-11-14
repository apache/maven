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
package org.apache.maven.cli.logging;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Detects whether standard output is an ANSI capable terminal. Maven only needs ANSI colors in its log output, so
 * there is no terminal library behind this: {@link org.fusesource.jansi.Ansi} writes plain SGR escape sequences when
 * color is enabled, and the CLI enables color in "auto" mode only when {@link #detect()} found a terminal. Embedded
 * use never calls {@link #detect()}, so "auto" stays plain there.
 */
public final class AnsiTerminal {

    private static volatile boolean tty;

    private static volatile int width; // 0 = not yet detected

    private AnsiTerminal() {}

    /**
     * Detects whether standard output is an ANSI capable terminal. Only the CLI calls this.
     */
    public static void detect() {
        tty = detectAnsiTerminal();
        width = 0;
    }

    /**
     * Returns {@code true} if {@link #detect()} found an ANSI capable terminal on standard output.
     */
    public static boolean isTty() {
        return tty;
    }

    /**
     * Returns the terminal width in columns, or {@code -1} if unknown. Taken from the {@code COLUMNS} environment
     * variable if set, otherwise (on a detected non-Windows terminal) from {@code stty size}. Computed once.
     */
    public static int getWidth() {
        int w = width;
        if (w == 0) {
            w = detectWidth();
            width = w;
        }
        return w;
    }

    private static int detectWidth() {
        int columns = parseWidth(System.getenv("COLUMNS"));
        if (columns > 0) {
            return columns;
        }
        if (!tty || System.getProperty("os.name", "").startsWith("Windows")) {
            return -1;
        }
        // read once, window resizes are not tracked
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
            // no native call to switch on VT processing, so trust only consoles known to do ANSI already
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
