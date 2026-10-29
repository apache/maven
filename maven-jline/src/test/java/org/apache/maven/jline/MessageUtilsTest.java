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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jline.jansi.AnsiColors;
import org.jline.jansi.AnsiMode;
import org.jline.jansi.AnsiPrintStream;
import org.jline.jansi.AnsiType;
import org.jline.jansi.io.AnsiOutputStream;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.spi.TerminalExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageUtilsTest {

    @AfterEach
    void reset() {
        MessageUtils.terminal = null;
    }

    @Test
    void awaitTerminalInitializationIsNoOpWhenNoTerminalInstalled() {
        MessageUtils.terminal = null;
        assertDoesNotThrow(MessageUtils::awaitTerminalInitialization);
    }

    @Test
    void awaitTerminalInitializationIsNoOpForPlainTerminal() throws Exception {
        // a plain terminal (e.g. embedded use) has nothing to wait for and must return at once
        try (Terminal terminal = TerminalBuilder.builder().dumb(true).build()) {
            MessageUtils.terminal = terminal;
            assertDoesNotThrow(MessageUtils::awaitTerminalInitialization);
        }
    }

    @Test
    void jlineStillUsesTheExpectedStreamForDumbSystemTerminals() throws Exception {
        // guards the detection in bufferSystemDumbTerminal against JLine upgrades
        Terminal terminal = systemDumbTerminal();
        assertEquals(
                MessageUtils.JLINE_SYSTEM_DUMB_OUTPUT,
                terminal.output().getClass().getName(),
                "JLine changed how it builds dumb system terminals, update MessageUtils.JLINE_SYSTEM_DUMB_OUTPUT");
    }

    @Test
    void systemDumbTerminalWritesLinesInChunksInsteadOfBytes() throws Exception {
        // #13735: with stdout redirected, every byte used to be a separate write(2)
        RecordingOutputStream console = new RecordingOutputStream();
        try (Terminal terminal = MessageUtils.bufferSystemDumbTerminal(systemDumbTerminal(), s -> console)) {
            AnsiPrintStream out = ansiPrintStream(terminal);
            char[] chars = new char[15 * 1024];
            Arrays.fill(chars, 'x');
            String line = new String(chars);

            out.println(line);

            byte[] expected = (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(expected, console.bytes.toByteArray(), "a println must be flushed completely");
            // one write per full buffer plus the flush at the end of the line, instead of one per byte
            assertTrue(
                    console.writes.size() <= 2 + expected.length / MessageUtils.CONSOLE_BUFFER_SIZE,
                    "expected a few chunked writes, got " + console.writes.size());
        }
    }

    @Test
    void systemDumbTerminalStripsAnsiCodesAndFlushesPerLine() throws Exception {
        RecordingOutputStream console = new RecordingOutputStream();
        try (Terminal terminal = MessageUtils.bufferSystemDumbTerminal(systemDumbTerminal(), s -> console)) {
            AnsiPrintStream out = ansiPrintStream(terminal);

            out.println("\u001B[1mfirst\u001B[m");
            assertEquals("first" + System.lineSeparator(), console.text());

            out.print("partial");
            out.flush();
            assertEquals("first" + System.lineSeparator() + "partial", console.text());
        }
    }

    @Test
    void closingBufferedTerminalFlushesPendingOutputButKeepsConsoleOpen() throws Exception {
        RecordingOutputStream console = new RecordingOutputStream();
        Terminal terminal = MessageUtils.bufferSystemDumbTerminal(systemDumbTerminal(), s -> console);
        terminal.output().write("no newline".getBytes(StandardCharsets.UTF_8));
        assertEquals("", console.text(), "output must be buffered until flushed");

        terminal.close();

        assertEquals("no newline", console.text());
        assertFalse(console.closed, "the process console must never be closed");
    }

    @Test
    void bufferedTerminalKeepsIdentityOfReplacedTerminal() throws Exception {
        TerminalExt original = (TerminalExt) systemDumbTerminal();
        List<Object> requested = new ArrayList<>();
        try (Terminal terminal = MessageUtils.bufferSystemDumbTerminal(original, s -> {
            requested.add(String.valueOf(s));
            return new RecordingOutputStream();
        })) {
            assertNotSame(original, terminal);
            // AnsiConsole strips ANSI codes only for a DumbTerminal, so the class must not change
            assertInstanceOf(DumbTerminal.class, terminal);
            assertEquals(original.getName(), terminal.getName());
            assertEquals(original.getType(), terminal.getType());
            assertEquals(original.encoding(), terminal.encoding());
            assertEquals(original.outputEncoding(), terminal.outputEncoding());
            assertSame(original.getProvider(), ((TerminalExt) terminal).getProvider());
            assertEquals(original.getSystemStream(), ((TerminalExt) terminal).getSystemStream());
            // the console is chosen like JLine does it, from the system stream of the original
            assertEquals(Arrays.asList(String.valueOf(original.getSystemStream())), requested);
        }
    }

    @Test
    void dumbTerminalOnSuppliedStreamsIsLeftAlone() throws Exception {
        // e.g. the terminal MavenCli installs for embedded executions, or streams given to the builder
        try (Terminal terminal = new DumbTerminal(
                        "Maven",
                        Terminal.TYPE_DUMB_COLOR,
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        null);
                Terminal built = TerminalBuilder.builder()
                        .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                        .type(Terminal.TYPE_DUMB)
                        .build()) {
            assertSame(terminal, MessageUtils.bufferSystemDumbTerminal(terminal, MessageUtilsTest::noConsole));
            assertSame(built, MessageUtils.bufferSystemDumbTerminal(built, MessageUtilsTest::noConsole));
        }
    }

    private static OutputStream noConsole(Object systemStream) {
        throw new AssertionError("the console must not be opened");
    }

    /**
     * Builds the terminal {@link MessageUtils#systemInstall()} gets from JLine when the console is not a terminal;
     * the dumb provider is forced so that the result does not depend on how the tests are run.
     */
    private static Terminal systemDumbTerminal() throws IOException {
        return TerminalBuilder.builder()
                .name("Maven")
                .provider(TerminalBuilder.PROP_PROVIDER_DUMB)
                .dumb(true)
                .systemOutput(TerminalBuilder.SystemOutput.SysOut)
                .color(true)
                .build();
    }

    /**
     * Mirrors the stream {@code AnsiConsole.systemInstall()} puts on {@code System.out} for a dumb terminal.
     */
    private static AnsiPrintStream ansiPrintStream(Terminal terminal) {
        return new AnsiPrintStream(
                new AnsiOutputStream(
                        terminal.output(),
                        terminal::getWidth,
                        AnsiMode.Strip,
                        null,
                        AnsiType.Unsupported,
                        AnsiColors.Colors16,
                        StandardCharsets.UTF_8,
                        null,
                        null,
                        false),
                true);
    }

    private static final class RecordingOutputStream extends OutputStream {

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final List<Integer> writes = new ArrayList<>();
        boolean closed;

        @Override
        public void write(int b) {
            writes.add(1);
            bytes.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            writes.add(len);
            bytes.write(b, off, len);
        }

        @Override
        public void close() {
            closed = true;
        }

        String text() {
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
