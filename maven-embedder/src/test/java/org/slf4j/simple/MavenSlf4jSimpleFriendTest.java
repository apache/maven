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
package org.slf4j.simple;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.jline.FastTerminal;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.logging.OutputCapabilities;
import org.apache.maven.logging.OutputCapabilities.Destination;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MavenSlf4jSimpleFriendTest {
    @TempDir
    Path directory;

    private OutputChoice originalChoice;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private String originalLogFile;

    @BeforeEach
    void save() {
        originalChoice = SimpleLogger.CONFIG_PARAMS.outputChoice;
        originalOut = System.out;
        originalErr = System.err;
        originalLogFile = System.getProperty(SimpleLogger.LOG_FILE_KEY);
    }

    @AfterEach
    void restore() {
        SimpleLogger.CONFIG_PARAMS.outputChoice = originalChoice;
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (originalLogFile == null) {
            System.clearProperty(SimpleLogger.LOG_FILE_KEY);
        } else {
            System.setProperty(SimpleLogger.LOG_FILE_KEY, originalLogFile);
        }
    }

    @Test
    void providerFileUsesDefaultCharset() throws Exception {
        Path file = directory.resolve("provider.log");
        System.setProperty(SimpleLogger.LOG_FILE_KEY, file.toString());
        SimpleLogger.init();
        OutputCapabilities capabilities =
                MavenSlf4jSimpleFriend.outputCapabilities(null).get();
        assertEquals(Destination.FILE, capabilities.getDestination());
        assertEquals(Optional.of(Charset.defaultCharset()), capabilities.getEncoding());
        try (PrintStream stream = SimpleLogger.CONFIG_PARAMS.outputChoice.getTargetPrintStream()) {
            stream.print("caf\u00e9");
        }
        assertArrayEquals("caf\u00e9".getBytes(Charset.defaultCharset()), Files.readAllBytes(file));
    }

    @Test
    void failedProviderFileReportsActualFallback() throws Exception {
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        System.setProperty(SimpleLogger.LOG_FILE_KEY, directory.toString());
        SimpleLogger.init();
        OutputCapabilities capabilities =
                MavenSlf4jSimpleFriend.outputCapabilities(null).get();
        assertEquals(Destination.UNKNOWN, capabilities.getDestination());
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    @Test
    void cachedStreamWinsOverLaterSystemStreamReplacement() throws Exception {
        try (PrintStream file = new PrintStream(Files.newOutputStream(directory.resolve("cli.log")))) {
            System.setOut(file);
            SimpleLogger.CONFIG_PARAMS.outputChoice = new OutputChoice(OutputChoice.OutputChoiceType.CACHED_SYS_OUT);
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            assertEquals(
                    Destination.FILE,
                    MavenSlf4jSimpleFriend.outputCapabilities(file).get().getDestination());
        }
    }

    @Test
    void terminalInspectionIsDeferredAndFailureRemainsUnknown() {
        Terminal originalTerminal = MessageUtils.getTerminal();
        try {
            FastTerminal terminal = mock(FastTerminal.class);
            when(terminal.getTerminal()).thenThrow(new IllegalStateException("Terminal unavailable"));
            MessageUtils.systemInstall(terminal);
            SimpleLogger.CONFIG_PARAMS.outputChoice = new OutputChoice(OutputChoice.OutputChoiceType.SYS_ERR);

            Supplier<OutputCapabilities> captured = MavenSlf4jSimpleFriend.outputCapabilities(null);
            verifyNoInteractions(terminal);
            OutputCapabilities capabilities = captured.get();
            assertEquals(Destination.UNKNOWN, capabilities.getDestination());
            assertEquals(Optional.empty(), capabilities.getEncoding());
        } finally {
            MessageUtils.systemInstall(originalTerminal);
        }
    }

    @Test
    void unknownStreamDoesNotInheritJvmEncoding() throws Exception {
        try (PrintStream stream = new PrintStream(new ByteArrayOutputStream(), true, "UTF-16LE")) {
            System.setErr(stream);
            SimpleLogger.CONFIG_PARAMS.outputChoice = new OutputChoice(OutputChoice.OutputChoiceType.SYS_ERR);
            OutputCapabilities capabilities =
                    MavenSlf4jSimpleFriend.outputCapabilities(null).get();
            assertEquals(Destination.UNKNOWN, capabilities.getDestination());
            assertEquals(Optional.empty(), capabilities.getEncoding());
        }
    }
}
