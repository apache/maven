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

import java.io.Console;
import java.lang.reflect.Method;

import org.apache.maven.message.MessageBuilder;
import org.apache.maven.message.MessageBuilderFactory;

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
     * Terminal width is not detected (that needs native code), so this is always {@code -1}, meaning "unknown".
     */
    public static int getTerminalWidth() {
        return -1;
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
