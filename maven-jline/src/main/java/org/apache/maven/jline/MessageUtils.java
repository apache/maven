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

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.jline.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class MessageUtils {

    static Terminal terminal;
    static LineReader reader;
    static MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
    static boolean colorEnabled = true;
    static Thread shutdownHook;
    static final Object STARTUP_SHUTDOWN_MONITOR = new Object();

    public static void systemInstall(Terminal terminal) {
        MessageUtils.terminal = terminal;
        MessageUtils.reader = createReader(terminal);
    }

    public static void systemInstall() {
        MessageUtils.terminal = new FastTerminal(
                () -> TerminalBuilder.builder().name("Maven").dumb(true).build(), terminal -> {
                    MessageUtils.reader = createReader(terminal);
                    AnsiConsole.setTerminal(terminal);
                    AnsiConsole.systemInstall();
                });
    }

    private static LineReader createReader(Terminal terminal) {
        return LineReaderBuilder.builder().terminal(terminal).build();
    }

    public static void registerShutdownHook() {
        if (shutdownHook == null) {
            shutdownHook = new Thread(() -> {
                synchronized (MessageUtils.STARTUP_SHUTDOWN_MONITOR) {
                    MessageUtils.doSystemUninstall();
                }
            });
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public static void systemUninstall() {
        doSystemUninstall();
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException var3) {
                // ignore
            }
        }
    }

    private static void doSystemUninstall() {
        try {
            AnsiConsole.systemUninstall();
        } finally {
            terminal = null;
        }
    }

    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }

    public static boolean isColorEnabled() {
        return colorEnabled && terminal != null;
    }

    public static int getTerminalWidth() {
        return terminal != null ? terminal.getWidth() : -1;
    }

    public static MessageBuilder builder() {
        return messageBuilderFactory.builder();
    }

    public static Terminal getTerminal() {
        return terminal;
    }
}
