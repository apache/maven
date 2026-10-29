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

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.maven.message.MessageBuilder;
import org.apache.maven.message.MessageBuilderFactory;
import org.jline.jansi.AnsiConsole;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalExt;

public class MessageUtils {

    static Terminal terminal;
    static MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
    static boolean colorEnabled = true;
    static Thread shutdownHook;
    static final Object STARTUP_SHUTDOWN_MONITOR = new Object();

    /**
     * Same size as the buffer Jansi 2.x (Maven 3.9.x) put in front of the console file descriptors.
     */
    static final int CONSOLE_BUFFER_SIZE = 8192;

    /**
     * The stream JLine's {@code DumbTerminalProvider} wraps the console file descriptor in for a system terminal.
     */
    static final String JLINE_SYSTEM_DUMB_OUTPUT =
            "org.jline.terminal.impl.DumbTerminalProvider$NonCloseableOutputStream";

    public static void systemInstall(Terminal terminal) {
        MessageUtils.terminal = terminal;
    }

    public static void systemInstall() {
        systemInstall(null, null);
    }

    public static void systemInstall(Consumer<TerminalBuilder> builderConsumer, Consumer<Terminal> terminalConsumer) {
        MessageUtils.terminal = new FastTerminal(
                () -> {
                    TerminalBuilder builder = TerminalBuilder.builder()
                            .name("Maven")
                            .dumb(true)
                            .systemOutput(TerminalBuilder.SystemOutput.SysOut)
                            .color(true);
                    if (builderConsumer != null) {
                        builderConsumer.accept(builder);
                    }
                    return bufferSystemDumbTerminal(builder.build());
                },
                terminal -> {
                    AnsiConsole.setTerminal(terminal);
                    AnsiConsole.systemInstall();
                    if (terminalConsumer != null) {
                        terminalConsumer.accept(terminal);
                    }
                });
    }

    /**
     * Replaces JLine's dumb system terminal with an equivalent one whose output is buffered.
     * <p>
     * When the console is not a terminal (output piped or redirected to a file, as in CI) JLine falls back to a
     * dumb terminal writing to an unbuffered {@link FileOutputStream}, behind a {@link FilterOutputStream} that
     * splits every array write into single bytes. {@code AnsiOutputStream}, which {@link AnsiConsole#systemInstall()}
     * puts on top, only implements {@code write(int)} as well, so every byte became its own {@code write(2)} system
     * call (#13735). Writing to the same file descriptor through a buffer restores the Maven 3.9.x behaviour: the
     * auto-flushing {@code AnsiPrintStream} still flushes on every line and explicit flush, and closing the terminal
     * on uninstall flushes what is left. The buffer is synchronized because {@code System.out} and
     * {@code System.err} share it. Any other terminal is returned unchanged: JLine buffers real terminals itself,
     * and streams supplied through the builder are not ours to replace.
     *
     * @param terminal the terminal JLine built
     * @return the terminal to use
     * @throws IOException if the replacement terminal cannot be created
     */
    static Terminal bufferSystemDumbTerminal(Terminal terminal) throws IOException {
        return bufferSystemDumbTerminal(
                terminal,
                stream -> new FileOutputStream(stream == SystemStream.Error ? FileDescriptor.err : FileDescriptor.out));
    }

    static Terminal bufferSystemDumbTerminal(Terminal terminal, Function<SystemStream, OutputStream> console)
            throws IOException {
        if (!(terminal instanceof DumbTerminal)
                || !JLINE_SYSTEM_DUMB_OUTPUT.equals(terminal.output().getClass().getName())) {
            return terminal;
        }
        TerminalExt dumb = (TerminalExt) terminal;
        // the system stream is null when no stream is a terminal, JLine then writes to stdout as well
        OutputStream output = new NonClosingOutputStream(console.apply(dumb.getSystemStream()));
        // the replacement takes over the input of the original, which therefore must not be closed
        return new DumbTerminal(
                dumb.getProvider(),
                dumb.getSystemStream(),
                terminal.getName(),
                terminal.getType(),
                terminal.input(),
                new BufferedOutputStream(output, CONSOLE_BUFFER_SIZE),
                terminal.encoding(),
                terminal.inputEncoding(),
                terminal.outputEncoding(),
                Terminal.SignalHandler.SIG_DFL);
    }

    /**
     * Waits for the terminal to finish installing on its background thread.
     * <p>
     * Terminal setup runs asynchronously and replaces {@code System.out}/{@code System.err} along
     * the way. Call this before you redirect those streams yourself (for example for the {@code -l}
     * log file), otherwise the late terminal install can overwrite your redirection and logs end up
     * back on the console.
     */
    public static void awaitTerminalInitialization() {
        if (terminal instanceof FastTerminal) {
            ((FastTerminal) terminal).getTerminal();
        }
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

    private static final class NonClosingOutputStream extends FilterOutputStream {

        NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // FilterOutputStream would write byte by byte, which is exactly what has to be avoided here
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            // keep the console open, but do not lose anything still pending
            out.flush();
        }
    }
}
