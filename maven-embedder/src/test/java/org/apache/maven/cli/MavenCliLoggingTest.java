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
package org.apache.maven.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import org.apache.maven.cli.logging.impl.Slf4jSimpleConfiguration;
import org.apache.maven.jline.FastTerminal;
import org.apache.maven.jline.MessageUtils;
import org.jline.terminal.Terminal;
import org.jline.terminal.spi.TerminalExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Isolated("Changes JVM-wide streams, system properties, terminal state, and logging configuration")
class MavenCliLoggingTest {
    @Test
    void cachesOutputAfterTerminalInstallationWithColorForced() throws Exception {
        cachesOutputAfterTerminalInstallation("always");
    }

    @Test
    void cachesOutputAfterTerminalInstallationWithColorDisabled() throws Exception {
        cachesOutputAfterTerminalInstallation("never");
    }

    @Test
    void cachesOutputAfterTerminalInstallationInBatchMode() throws Exception {
        cachesOutputAfterTerminalInstallation("batch");
    }

    private void cachesOutputAfterTerminalInstallation(String color) throws Exception {
        PrintStream originalOut = System.out;
        Terminal originalTerminal = MessageUtils.getTerminal();
        boolean originalColor;
        try {
            // isColorEnabled also requires a terminal, even when the color flag is set.
            MessageUtils.systemInstall(mock(Terminal.class));
            originalColor = MessageUtils.isColorEnabled();
        } finally {
            MessageUtils.systemInstall(originalTerminal);
        }
        String cacheProperty = "org.slf4j.simpleLogger.cacheOutputStream";
        String fileProperty = "org.slf4j.simpleLogger.logFile";
        String originalCache = System.getProperty(cacheProperty);
        String originalFile = System.getProperty(fileProperty);
        CountDownLatch buildingTerminal = new CountDownLatch(1);
        CountDownLatch releaseTerminal = new CountDownLatch(1);
        CountDownLatch awaitingTerminalOrLoggingFinished = new CountDownLatch(1);
        ByteArrayOutputStream earlyOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream terminalOutput = new ByteArrayOutputStream();
        Thread loggingThread = null;
        try (PrintStream earlyStream = new PrintStream(earlyOutput, true, UTF_8.name());
                PrintStream terminalStream = new PrintStream(terminalOutput, true, UTF_8.name())) {
            System.setProperty(cacheProperty, "true");
            System.setProperty(fileProperty, "System.out");
            System.setOut(earlyStream);
            new Slf4jSimpleConfiguration().activate();

            FastTerminal terminal =
                    new FastTerminal(
                            () -> {
                                buildingTerminal.countDown();
                                if (!releaseTerminal.await(10, SECONDS)) {
                                    throw new IllegalStateException("Terminal installation was not released");
                                }
                                return mock(TerminalExt.class);
                            },
                            installed -> System.setOut(terminalStream)) {
                        @Override
                        public TerminalExt getTerminal() {
                            awaitingTerminalOrLoggingFinished.countDown();
                            return super.getTerminal();
                        }
                    };
            MessageUtils.systemInstall(terminal);
            assertTrue(buildingTerminal.await(10, SECONDS));

            MavenCli cli = new MavenCli();
            CliRequest request = new CliRequest(
                    "batch".equals(color) ? new String[] {"-B"} : new String[] {"-Dstyle.color=" + color}, null);
            cli.cli(request);
            cli.properties(request);
            FutureTask<Void> logging = new FutureTask<>(() -> {
                try {
                    cli.logging(request);
                    return null;
                } finally {
                    awaitingTerminalOrLoggingFinished.countDown();
                }
            });
            loggingThread = new Thread(logging, "logging-initialization-test");
            loggingThread.setDaemon(true);
            loggingThread.start();
            assertTrue(awaitingTerminalOrLoggingFinished.await(10, SECONDS));
            releaseTerminal.countDown();
            logging.get(10, SECONDS);
            terminal.getTerminal();

            LoggerFactory.getLogger(MavenCliLoggingTest.class).warn("terminal-output-marker");
            assertTrue(terminalOutput.toString(UTF_8.name()).contains("terminal-output-marker"));
            assertFalse(earlyOutput.toString(UTF_8.name()).contains("terminal-output-marker"));
        } finally {
            releaseTerminal.countDown();
            if (loggingThread != null) {
                loggingThread.join(10000);
            }
            System.setOut(originalOut);
            MessageUtils.systemInstall(originalTerminal);
            MessageUtils.setColorEnabled(originalColor);
            restoreProperty(cacheProperty, originalCache);
            restoreProperty(fileProperty, originalFile);
            new Slf4jSimpleConfiguration().activate();
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
