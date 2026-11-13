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
import java.util.function.Consumer;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.jline.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DeferredTerminal;

public class MessageUtils {

    static Terminal terminal;
    static LineReader reader;
    static MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
    static boolean colorEnabled = true;
    static Thread shutdownHook;
    static final Object STARTUP_SHUTDOWN_MONITOR = new Object();

    public static void systemInstall(Terminal terminal) {
        MessageUtils.terminal = terminal;
        MessageUtils.reader = terminal != null ? createReader(terminal) : null;
    }

    public static void systemInstall() {
        systemInstall(null, null);
    }

    public static void systemInstall(Consumer<TerminalBuilder> builderConsumer, Consumer<Terminal> terminalConsumer) {
        TerminalBuilder builder = TerminalBuilder.builder().name("Maven").dumb(true);
        if (builderConsumer != null) {
            builderConsumer.accept(builder);
        }
        // buildDeferred() returns a DeferredTerminal proxy immediately (~10ms).
        // The real terminal is built on a background daemon thread; all proxy
        // methods block transparently until it is ready.
        DeferredTerminal deferred = (DeferredTerminal) builder.buildDeferred();
        // Register a callback that runs on the build thread once the real
        // terminal is ready, before any blocked caller is unblocked.
        deferred.onComplete(terminal -> {
            MessageUtils.reader = createReader(terminal);
            AnsiConsole.setTerminal(terminal);
            AnsiConsole.systemInstall();
            if (terminalConsumer != null) {
                terminalConsumer.accept(terminal);
            }
        });
        MessageUtils.terminal = deferred;
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
            if (terminal instanceof DeferredTerminal deferred) {
                if (!deferred.isReady()) {
                    // Don't wait — drain buffered output to stdout and let
                    // the daemon build thread die with the JVM.
                    deferred.close();
                    return;
                }
            }
            AnsiConsole.systemUninstall();
        } catch (IOException | IllegalStateException e) {
            // best-effort cleanup
        } finally {
            terminal = null;
        }
    }

    /**
     * Blocks until the asynchronous terminal installation completes.
     * Call this before redirecting {@code System.out}/{@code System.err} to avoid the terminal
     * thread overwriting your redirection, and before activating logging configuration to ensure
     * it caches the correct output stream.
     *
     * @see <a href="https://github.com/apache/maven/issues/12188">#12188</a>
     */
    public static void awaitTerminalInitialization() {
        if (terminal instanceof DeferredTerminal deferred) {
            deferred.getTerminal();
        }
    }

    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }

    public static boolean isColorEnabled() {
        return colorEnabled && terminal != null;
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
