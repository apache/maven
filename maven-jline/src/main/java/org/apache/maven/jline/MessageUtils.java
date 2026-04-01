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

import java.util.function.Consumer;

import org.apache.maven.message.MessageBuilder;
import org.apache.maven.message.MessageBuilderFactory;
import org.jline.jansi.AnsiConsole;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class MessageUtils {

    static Terminal terminal;
    static MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
    static boolean colorEnabled = true;
    static Thread shutdownHook;
    static final Object STARTUP_SHUTDOWN_MONITOR = new Object();

    public static void systemInstall(Terminal terminal) {
        MessageUtils.terminal = terminal;
    }

    public static void systemInstall() {
        systemInstall(null, null);
    }

    public static void systemInstall(Consumer<TerminalBuilder> builderConsumer, Consumer<Terminal> terminalConsumer) {
        MessageUtils.terminal = new FastTerminal(
                () -> {
                    TerminalBuilder builder =
                            TerminalBuilder.builder().name("Maven").dumb(true);
                    if (builderConsumer != null) {
                        builderConsumer.accept(builder);
                    }
                    return builder.build();
                },
                terminal -> {
                    AnsiConsole.setTerminal(terminal);
                    AnsiConsole.systemInstall();
                    if (terminalConsumer != null) {
                        terminalConsumer.accept(terminal);
                    }
                });
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
            if (terminal instanceof FastTerminal) {
                // wait for the asynchronous systemInstall call before we uninstall to ensure a consistent state
                ((FastTerminal) terminal).getTerminal();
            }
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

    /**
     * Remove any ANSI code from a message (colors or other escape sequences).
     *
     * @param msg message eventually containing ANSI codes
     * @return the message with ANSI codes removed
     */
    public static String stripAnsiCodes(String msg) {
        return msg.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }
}
