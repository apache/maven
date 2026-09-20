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
package org.apache.maven.cling.invoker;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.cling.invoker.mvn.MavenContext;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.logging.OutputCapabilities;
import org.apache.maven.logging.OutputCapabilities.Destination;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LookupInvokerOutputCapabilitiesTest {
    @TempDir
    Path directory;

    @Test
    void logFileUsesUtf8AndDrainsBeforeClosing() throws Exception {
        Path file = directory.resolve("build.log");
        try (MavenContext context = context(Optional.of(file.toString()))) {
            Consumer<String> writer = new TestInvoker().writer(context);
            OutputCapabilities capabilities = context.outputCapabilities.get();
            assertEquals(Destination.FILE, capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.UTF_8), capabilities.getEncoding());
            writer.accept("caf\u00e9");
        }
        assertArrayEquals(
                ("caf\u00e9" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
    }

    @Test
    void failedFileDoesNotPublishFileCapabilities() throws Exception {
        try (MavenContext context = context(Optional.of(directory.toString()))) {
            assertThrows(MavenException.class, () -> new TestInvoker().writer(context));
            assertEquals(Destination.UNKNOWN, context.outputCapabilities.get().getDestination());
            assertEquals(Optional.empty(), context.outputCapabilities.get().getEncoding());
        }
    }

    @Test
    void embeddedWriterHasKnownEncodingButUnknownDestination() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MavenContext context = context(Optional.empty())) {
            Terminal terminal = mock(Terminal.class);
            when(terminal.outputEncoding()).thenReturn(StandardCharsets.ISO_8859_1);
            when(terminal.writer())
                    .thenReturn(new PrintWriter(new OutputStreamWriter(bytes, StandardCharsets.ISO_8859_1)));
            context.terminal = terminal;
            Consumer<String> writer = new TestInvoker().writer(context);
            assertEquals(Destination.UNKNOWN, context.outputCapabilities.get().getDestination());
            assertEquals(
                    Optional.of(StandardCharsets.ISO_8859_1),
                    context.outputCapabilities.get().getEncoding());
            writer.accept("caf\u00e9");
        }
        assertArrayEquals(
                ("caf\u00e9" + System.lineSeparator()).getBytes(StandardCharsets.ISO_8859_1), bytes.toByteArray());
    }

    @Test
    void customWriterRemainsUnknown() throws Exception {
        try (MavenContext context = context(Optional.empty())) {
            Consumer<String> custom = ignored -> {};
            context.writer = custom;
            assertSame(custom, new TestInvoker().writer(context));
            assertEquals(Destination.UNKNOWN, context.outputCapabilities.get().getDestination());
            assertEquals(Optional.empty(), context.outputCapabilities.get().getEncoding());
        }
    }

    private MavenContext context(Optional<String> logFile) {
        InvokerRequest request = mock(InvokerRequest.class);
        when(request.cwd()).thenReturn(directory);
        when(request.installationDirectory()).thenReturn(directory);
        when(request.userHomeDirectory()).thenReturn(directory);
        when(request.topDirectory()).thenReturn(directory);
        when(request.parserRequest()).thenReturn(mock(ParserRequest.class));
        when(request.embedded()).thenReturn(true);
        MavenOptions options = mock(MavenOptions.class);
        when(options.logFile()).thenReturn(logFile);
        return new MavenContext(request, false, options);
    }

    private static class TestInvoker extends MavenInvoker {
        TestInvoker() {
            super(ProtoLookup.builder().build(), null);
        }

        Consumer<String> writer(MavenContext context) {
            return determineWriter(context);
        }
    }
}
