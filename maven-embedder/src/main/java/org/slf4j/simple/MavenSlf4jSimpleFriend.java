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

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import org.apache.maven.jline.FastTerminal;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.logging.OutputCapabilities;
import org.apache.maven.logging.OutputCapabilities.Destination;
import org.apache.maven.logging.internal.DefaultOutputCapabilities;
import org.apache.maven.logging.internal.TerminalOutputCapabilities;
import org.jline.jansi.AnsiConsole;
import org.jline.terminal.Terminal;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Utility for Maven to access Slf4j-Simple internals through package access.
 * Use with precaution, since this is not normally intended for production use.
 */
public class MavenSlf4jSimpleFriend {
    /**
     * Captures the activated provider, including file-open fallback and cached streams.
     * Terminal inspection is deferred until container initialization: waiting here would
     * change which stream receives output printed during asynchronous Jansi installation.
     */
    public static Supplier<OutputCapabilities> outputCapabilities(PrintStream logFileStream) {
        OutputChoice choice = SimpleLogger.CONFIG_PARAMS.outputChoice;
        if (choice == null) {
            return () -> DefaultOutputCapabilities.UNKNOWN;
        }
        PrintStream stream = choice.getTargetPrintStream();
        if (choice.outputChoiceType == OutputChoice.OutputChoiceType.FILE
                || (logFileStream != null && stream == logFileStream)) {
            OutputCapabilities capabilities =
                    DefaultOutputCapabilities.snapshot(Destination.FILE, Charset.defaultCharset());
            return () -> capabilities;
        }
        Terminal terminal = MessageUtils.getTerminal();
        return () -> {
            try {
                if (terminal != null) {
                    // Resolving a FastTerminal also waits for Jansi stream installation.
                    if (terminal instanceof FastTerminal) {
                        ((FastTerminal) terminal).getTerminal();
                    }
                    if (AnsiConsole.isInstalled() && (stream == AnsiConsole.out() || stream == AnsiConsole.err())) {
                        // Both Jansi streams write through terminal.output(), even when SLF4J
                        // selected System.err. Describe that destination, not the stderr descriptor.
                        return DefaultOutputCapabilities.snapshot(
                                TerminalOutputCapabilities.destination(terminal), terminal.encoding());
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                // Capability inspection must not introduce a new logging failure.
                return DefaultOutputCapabilities.UNKNOWN;
            }
            // SLF4J can also have cached the original stream before asynchronous Jansi installation
            // completed (for example in batch mode). Its encoding need not match the terminal's.
            // An arbitrary PrintStream supplied by an embedder does not expose its charset on Java 8.
            return DefaultOutputCapabilities.UNKNOWN;
        };
    }

    public static void init() {
        SimpleLogger.init();
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (loggerFactory instanceof SimpleLoggerFactory) {
            ((SimpleLoggerFactory) loggerFactory).reset();
        }
    }
}
