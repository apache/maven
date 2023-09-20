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
package org.apache.maven.cli.jansi;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.internal.impl.DefaultMessageBuilder;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiMode;

/**
 * Colored message utils, to manage colors.  This is the core implementation of the
 * {@link JansiMessageBuilderFactory} and {@link JansiMessageBuilder} classes.
 * This class should not be used outside of maven-embedder and the public
 * {@link org.apache.maven.api.services.MessageBuilderFactory} should be used instead.
 * <p>
 * Internally, <a href="http://fusesource.github.io/jansi/">Jansi</a> is used to render
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#Colors">ANSI colors</a> on any platform.
 * <p>
 *
 * @see MessageBuilder
 * @see org.apache.maven.api.services.MessageBuilderFactory
 * @see JansiMessageBuilderFactory
 * @see JansiMessageBuilder
 * @since 4.0.0
 */
public class MessageUtils {
    private static final boolean JANSI;

    /** Reference to the JVM shutdown hook, if registered */
    private static Thread shutdownHook;

    /** Synchronization monitor for the "uninstall" */
    private static final Object STARTUP_SHUTDOWN_MONITOR = new Object();

    static {
        boolean jansi = true;
        try {
            // Jansi is provided by Maven core since 3.5.0
            Class.forName("org.fusesource.jansi.Ansi");
        } catch (ClassNotFoundException cnfe) {
            jansi = false;
        }
        JANSI = jansi;
    }

    /**
     * Install color support.
     * This method is called by Maven core, and calling it is not necessary in plugins.
     */
    public static void systemInstall() {
        if (JANSI) {
            AnsiConsole.systemInstall();
        }
    }

    /**
     * Undo a previous {@link #systemInstall()}.  If {@link #systemInstall()} was called
     * multiple times, {@link #systemUninstall()} must be called call the same number of times before
     * it is actually uninstalled.
     */
    public static void systemUninstall() {
        synchronized (STARTUP_SHUTDOWN_MONITOR) {
            doSystemUninstall();

            // hook can only set when Jansi is true
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ex) {
                    // ignore - VM is already shutting down
                }
            }
        }
    }

    private static void doSystemUninstall() {
        if (JANSI) {
            AnsiConsole.systemUninstall();
        }
    }

    /**
     * Enables message color (if Jansi is available).
     * @param flag to enable Jansi
     */
    public static void setColorEnabled(boolean flag) {
        if (JANSI) {
            AnsiConsole.out().setMode(flag ? AnsiMode.Force : AnsiMode.Strip);
            Ansi.setEnabled(flag);
            System.setProperty(
                    AnsiConsole.JANSI_MODE, flag ? AnsiConsole.JANSI_MODE_FORCE : AnsiConsole.JANSI_MODE_STRIP);
            boolean installed = AnsiConsole.isInstalled();
            while (AnsiConsole.isInstalled()) {
                AnsiConsole.systemUninstall();
            }
            if (installed) {
                AnsiConsole.systemInstall();
            }
        }
    }

    /**
     * Is message color enabled: requires Jansi available (through Maven) and the color has not been disabled.
     * @return whether colored messages are enabled
     */
    public static boolean isColorEnabled() {
        return JANSI ? Ansi.isEnabled() : false;
    }

    /**
     * Create a default message buffer.
     * @return a new buffer
     */
    public static MessageBuilder builder() {
        return builder(new StringBuilder());
    }

    /**
     * Create a message buffer with an internal buffer of defined size.
     * @param size size of the buffer
     * @return a new buffer
     */
    public static MessageBuilder builder(int size) {
        return builder(new StringBuilder(size));
    }

    /**
     * Create a message buffer with defined String builder.
     * @param builder initial content of the message buffer
     * @return a new buffer
     */
    public static MessageBuilder builder(StringBuilder builder) {
        return JANSI && isColorEnabled() ? new JansiMessageBuilder(builder) : new DefaultMessageBuilder(builder);
    }

    /**
     * Remove any ANSI code from a message (colors or other escape sequences).
     * @param msg message eventually containing ANSI codes
     * @return the message with ANSI codes removed
     */
    public static String stripAnsiCodes(String msg) {
        return msg.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }

    /**
     * Register a shutdown hook with the JVM runtime, uninstalling Ansi support on
     * JVM shutdown unless is has already been uninstalled at that time.
     * <p>Delegates to {@link #doSystemUninstall()} for the actual uninstall procedure
     *
     * @see Runtime#addShutdownHook(Thread)
     * @see MessageUtils#systemUninstall()
     * @see #doSystemUninstall()
     */
    public static void registerShutdownHook() {
        if (JANSI && shutdownHook == null) {
            // No shutdown hook registered yet.
            shutdownHook = new Thread() {
                @Override
                public void run() {
                    synchronized (STARTUP_SHUTDOWN_MONITOR) {
                        while (AnsiConsole.isInstalled()) {
                            doSystemUninstall();
                        }
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    /**
     * Get the terminal width or -1 if the width cannot be determined.
     *
     * @return the terminal width
     */
    public static int getTerminalWidth() {
        if (JANSI) {
            int width = AnsiConsole.getTerminalWidth();
            return width > 0 ? width : -1;
        } else {
            return -1;
        }
    }
}
