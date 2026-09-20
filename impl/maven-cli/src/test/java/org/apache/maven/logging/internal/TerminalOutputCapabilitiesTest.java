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
package org.apache.maven.logging.internal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.maven.logging.OutputCapabilities.Destination;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminalProvider;
import org.jline.terminal.impl.exec.ExecTerminalProvider;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalExt;
import org.jline.terminal.spi.TerminalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerminalOutputCapabilitiesTest {
    @TempDir
    Path directory;

    @Test
    void negativeExecProbesAreInconclusive() {
        for (SystemStream stream : Arrays.asList(SystemStream.Output, SystemStream.Error)) {
            ExecTerminalProvider provider = mock(ExecTerminalProvider.class);
            TerminalExt terminal = mock(TerminalExt.class);
            when(terminal.getProvider()).thenReturn(provider);
            when(terminal.getSystemStream()).thenReturn(stream);
            when(provider.isSystemStream(stream)).thenReturn(true);
            assertEquals(Destination.CONSOLE, TerminalOutputCapabilities.destination(terminal));
            assertEquals(
                    Destination.CONSOLE, TerminalOutputCapabilities.probe(Collections.singletonList(provider), stream));
            when(provider.isSystemStream(stream)).thenReturn(false);
            assertEquals(Destination.UNKNOWN, TerminalOutputCapabilities.destination(terminal));
            assertEquals(
                    Destination.UNKNOWN, TerminalOutputCapabilities.probe(Collections.singletonList(provider), stream));
        }
    }

    @Test
    void inconclusiveExecProbesDoNotOverrideOtherProviders() {
        for (SystemStream stream : Arrays.asList(SystemStream.Output, SystemStream.Error)) {
            ExecTerminalProvider exec = mock(ExecTerminalProvider.class);
            TerminalProvider other = mock(TerminalProvider.class);
            for (boolean console : Arrays.asList(false, true)) {
                when(other.isSystemStream(stream)).thenReturn(console);
                Destination expected = console ? Destination.CONSOLE : Destination.REDIRECTED;
                assertEquals(expected, TerminalOutputCapabilities.probe(Arrays.asList(exec, other), stream));
                assertEquals(expected, TerminalOutputCapabilities.probe(Arrays.asList(other, exec), stream));
            }
            when(other.isSystemStream(stream)).thenThrow(new UnsatisfiedLinkError());
            assertEquals(Destination.UNKNOWN, TerminalOutputCapabilities.probe(Arrays.asList(exec, other), stream));
            assertEquals(Destination.UNKNOWN, TerminalOutputCapabilities.probe(Arrays.asList(other, exec), stream));
        }
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void missingExecCommandDoesNotMeanRedirection() throws Exception {
        Path emptyPath = Files.createDirectory(directory.resolve("empty-path"));
        Path output = directory.resolve("probe.log");
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ProcessBuilder builder = new ProcessBuilder(java, "-cp", classpath, ExecTerminalProbe.class.getName());
        builder.environment().put("PATH", emptyPath.toString());
        builder.redirectErrorStream(true).redirectOutput(output.toFile());
        Process process = builder.start();
        try {
            process.getOutputStream().close();
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);
            String diagnostics = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
            assertTrue(exited, "Terminal probe timed out: " + diagnostics);
            assertEquals(0, process.exitValue(), diagnostics);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void unavailableProvidersDoNotMeanRedirection() {
        assertEquals(
                Destination.UNKNOWN, TerminalOutputCapabilities.probe(Collections.emptyList(), SystemStream.Output));
        assertEquals(
                Destination.UNKNOWN,
                TerminalOutputCapabilities.probe(
                        Collections.singletonList(new DumbTerminalProvider()), SystemStream.Output));
        TerminalProvider failing = mock(TerminalProvider.class);
        when(failing.isSystemStream(SystemStream.Output)).thenThrow(new UnsatisfiedLinkError());
        assertEquals(
                Destination.UNKNOWN,
                TerminalOutputCapabilities.probe(Collections.singletonList(failing), SystemStream.Output));
    }

    @Test
    void probesTheOutputStreamEvenForDumbTerminalType() {
        TerminalExt terminal = mock(TerminalExt.class);
        TerminalProvider provider = mock(TerminalProvider.class);
        when(terminal.getProvider()).thenReturn(provider);
        when(terminal.getSystemStream()).thenReturn(SystemStream.Error);
        when(terminal.getType()).thenReturn(Terminal.TYPE_DUMB);
        when(provider.isSystemStream(SystemStream.Error)).thenReturn(true);
        assertEquals(Destination.CONSOLE, TerminalOutputCapabilities.destination(terminal));
        when(provider.isSystemStream(SystemStream.Error)).thenReturn(false);
        assertEquals(Destination.REDIRECTED, TerminalOutputCapabilities.destination(terminal));
    }

    @Test
    void customTerminalDoesNotImplyConsole() {
        TerminalExt terminal = mock(TerminalExt.class);
        when(terminal.getProvider()).thenReturn(mock(TerminalProvider.class));
        assertEquals(Destination.UNKNOWN, TerminalOutputCapabilities.destination(terminal));
        assertEquals(Destination.UNKNOWN, TerminalOutputCapabilities.destination(mock(Terminal.class)));
    }

    @Test
    void oneSuccessfulProviderCanEstablishConsoleAttachment() {
        TerminalProvider redirected = mock(TerminalProvider.class);
        TerminalProvider console = mock(TerminalProvider.class);
        when(console.isSystemStream(SystemStream.Output)).thenReturn(true);
        assertEquals(
                Destination.CONSOLE,
                TerminalOutputCapabilities.probe(Arrays.asList(redirected, console), SystemStream.Output));
        assertEquals(
                Destination.REDIRECTED,
                TerminalOutputCapabilities.probe(Collections.singletonList(redirected), SystemStream.Output));
    }
}
