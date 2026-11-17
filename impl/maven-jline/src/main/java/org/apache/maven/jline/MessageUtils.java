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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.apache.maven.api.Constants;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.jline.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DeferredTerminal;
import org.jline.utils.OSUtils;

public class MessageUtils {

    private static final int SIGINT_EXIT_CODE = 130;

    static volatile Terminal terminal;
    static volatile LineReader reader;
    static MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
    static volatile Boolean colorEnabled;
    static Thread shutdownHook;
    static final Object STARTUP_SHUTDOWN_MONITOR = new Object();

    public static void systemInstall(Terminal terminal) {
        MessageUtils.terminal = terminal;
    }

    public static void systemInstall() {
        systemInstall((Consumer<TerminalBuilder>) null);
    }

    public static void systemInstall(Consumer<TerminalBuilder> builderConsumer) {
        systemInstall(builderConsumer, false);
    }

    public static void systemInstall(Consumer<TerminalBuilder> builderConsumer, boolean embedded) {
        TerminalBuilder builder = TerminalBuilder.builder().name("Maven").dumb(true);
        if (builderConsumer != null) {
            builderConsumer.accept(builder);
        }
        boolean deferred =
                !embedded && Boolean.parseBoolean(System.getProperty(Constants.MAVEN_TERMINAL_DEFERRED, "true"));
        if (!deferred) {
            try {
                MessageUtils.terminal = builder.build();
                AnsiConsole.setTerminal(MessageUtils.terminal);
                if (OSUtils.IS_WINDOWS) {
                    MessageUtils.terminal.handle(Terminal.Signal.INT, signal -> System.exit(SIGINT_EXIT_CODE));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            DeferredTerminal deferredTerminal = (DeferredTerminal) builder.buildDeferred();
            deferredTerminal.onComplete(terminal -> {
                if (OSUtils.IS_WINDOWS) {
                    terminal.handle(Terminal.Signal.INT, signal -> System.exit(SIGINT_EXIT_CODE));
                }
                AnsiConsole.setTerminal(terminal);
            });
            MessageUtils.terminal = deferredTerminal;
        }
    }

    static LineReader getReader() {
        LineReader r = reader;
        if (r == null) {
            synchronized (STARTUP_SHUTDOWN_MONITOR) {
                r = reader;
                if (r == null) {
                    r = LineReaderBuilder.builder().terminal(terminal).build();
                    reader = r;
                }
            }
        }
        return r;
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
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            // best-effort cleanup
        } finally {
            terminal = null;
            colorEnabled = null;
            reader = null;
        }
    }

    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }

    public static boolean isColorEnabled() {
        if (terminal == null) {
            return false;
        }
        Boolean c = colorEnabled;
        return c == null || c;
    }

    public static int getTerminalWidth() {
        return terminal != null ? terminal.getColumns() : -1;
    }

    public static MessageBuilder builder() {
        return messageBuilderFactory.builder();
    }

    public static Terminal getTerminal() {
        return terminal;
    }
}
