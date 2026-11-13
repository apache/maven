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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.Constants;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.cling.logging.Slf4jConfigurationFactory;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.slf4j.MavenBaseLogger;
import org.apache.maven.slf4j.MavenLoggerFactory;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DeferredTerminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Isolated("Changes JVM-wide streams, system properties, terminal state, and logging configuration")
class MavenCliLoggingTest {

    private static final String[] SAVED_PROPERTIES = {
        MavenBaseLogger.LEGACY_PREFIX + "logFile",
        MavenBaseLogger.LEGACY_PREFIX + "cacheOutputStream",
        MavenBaseLogger.LEGACY_PREFIX + "defaultLogLevel",
        Constants.MAVEN_LOGGER_LOG_FILE,
        Constants.MAVEN_LOGGER_CACHE_OUTPUT_STREAM,
        Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL,
    };

    private PrintStream savedOut;
    private Terminal savedTerminal;
    private boolean savedColor;
    private final String[] savedPropertyValues = new String[SAVED_PROPERTIES.length];

    @BeforeEach
    void setUp() throws Exception {
        // These tests require MavenLoggerFactory as the active SLF4J provider because they
        // rely on stream caching behaviour specific to MavenSimpleConfiguration.activate().
        // When another binding (e.g. Logback) is on the classpath and wins the ServiceLoader
        // race (platform-dependent), the assertions cannot hold and the test must be skipped
        // rather than fail.
        assumeTrue(
                LoggerFactory.getILoggerFactory() instanceof MavenLoggerFactory,
                "MavenLoggerFactory must be the active SLF4J provider");
        savedOut = System.out;
        savedTerminal = getTerminalField();
        savedColor = getColorEnabled();
        // Null out the terminal so leftover DeferredTerminal threads from other tests cannot
        // race with this test's stream setup via their consumer callbacks.
        injectTerminal(null);
        for (int i = 0; i < SAVED_PROPERTIES.length; i++) {
            savedPropertyValues[i] = System.getProperty(SAVED_PROPERTIES[i]);
        }
        // Clear all logger properties and re-initialize the config so previous tests
        // (e.g. MavenCliTest#calculateTransferListener setting maven.logger.defaultLogLevel=error
        // via --quiet) cannot poison this test's config. The re-init is needed because
        // SimpleLoggerConfiguration.init() only updates defaultLogLevel when the property IS set;
        // clearing the property alone leaves the stale value in the config object.
        for (String prop : SAVED_PROPERTIES) {
            System.clearProperty(prop);
        }
        activateSlf4j();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(savedOut);
        injectTerminal(savedTerminal);
        setColorEnabled(savedColor);
        for (int i = 0; i < SAVED_PROPERTIES.length; i++) {
            restoreProperty(SAVED_PROPERTIES[i], savedPropertyValues[i]);
        }
        activateSlf4j();
    }

    /**
     * When color is explicitly forced ({@code -color=always}), {@code logging()} must NOT call
     * {@code awaitTerminalInitialization()} before activating the logging configuration.  The
     * logger must therefore cache the stream that was active at activation time (the "early"
     * stream), not the stream installed by the terminal consumer afterwards.
     * <p>
     * Note: on master the logger reconfiguration ({@code MavenSimpleLogger.configure()}) renders
     * ANSI-coloured level labels via {@code JLineMessageBuilderFactory}, which calls
     * {@code terminal.getType()} and therefore always awaits terminal initialisation.  The test
     * accounts for this by completing the terminal build before running {@code logging()} and
     * then swapping {@code System.out} afterwards, verifying that the logger sticks to the
     * stream it cached at activation time.
     */
    @Test
    void withColorForcedLoggerCachesEarlyStream() throws Exception {
        ByteArrayOutputStream earlyOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream lateOutput = new ByteArrayOutputStream();
        try (PrintStream earlyStream = new PrintStream(earlyOutput, true, UTF_8.name());
                PrintStream lateStream = new PrintStream(lateOutput, true, UTF_8.name());
                Terminal dumb = new DumbTerminal(
                        "Maven",
                        Terminal.TYPE_DUMB_COLOR,
                        InputStream.nullInputStream(),
                        OutputStream.nullOutputStream(),
                        null)) {
            // Install a completed terminal so configure() can call getType() without blocking.
            injectTerminal(dumb);

            // Set up logger to cache System.out at activate() time.
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "cacheOutputStream", "true");
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "logFile", "System.out");
            System.setOut(earlyStream);
            activateSlf4j();

            // Run logging() with -color=always. It must NOT call awaitTerminalInitialization()
            // before activate(), so it completes immediately (no blocking terminal).
            MavenCli cli = new MavenCli();
            CliRequest request = new CliRequest(new String[] {"-Dstyle.color=always"}, null);
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);

            // Swap System.out AFTER logging() has cached earlyStream.
            System.setOut(lateStream);

            // The logger must still write to earlyStream.
            LoggerFactory.getLogger(MavenCliLoggingTest.class).warn("terminal-output-marker");
            assertTrue(
                    earlyOutput.toString(UTF_8.name()).contains("terminal-output-marker"),
                    "Logger must write to the stream that was active when logging() ran with -color=always");
            assertFalse(
                    lateOutput.toString(UTF_8.name()).contains("terminal-output-marker"),
                    "Logger must not switch to the stream installed after logging() completed");
        }
    }

    /**
     * When color is set to {@code never} or {@code auto} (batch mode), {@code logging()} must call
     * {@code awaitTerminalInitialization()} before activating the logging configuration, so that
     * the terminal consumer has already swapped {@code System.out} before the logger caches it.
     * This prevents logs from going to a stale stream instead of the terminal-wrapped one.
     *
     */
    @Test
    void cachesOutputAfterTerminalInstallationWithColorDisabled() throws Exception {
        cachesOutputAfterTerminalInstallation("never");
    }

    @Test
    void cachesOutputAfterTerminalInstallationInBatchMode() throws Exception {
        cachesOutputAfterTerminalInstallation("batch");
    }

    private void cachesOutputAfterTerminalInstallation(String color) throws Exception {
        CountDownLatch buildingTerminal = new CountDownLatch(1);
        CountDownLatch releaseTerminal = new CountDownLatch(1);
        ByteArrayOutputStream earlyOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream terminalOutput = new ByteArrayOutputStream();
        Thread loggingThread = null;
        try (PrintStream earlyStream = new PrintStream(earlyOutput, true, UTF_8.name());
                PrintStream terminalStream = new PrintStream(terminalOutput, true, UTF_8.name());
                Terminal dumb = new DumbTerminal(
                        "Maven",
                        Terminal.TYPE_DUMB_COLOR,
                        InputStream.nullInputStream(),
                        OutputStream.nullOutputStream(),
                        null)) {
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "cacheOutputStream", "true");
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "logFile", "System.out");
            System.setOut(earlyStream);
            activateSlf4j();

            DeferredTerminal terminal = new DeferredTerminal();
            terminal.onComplete(installed -> System.setOut(terminalStream));
            Thread buildThread = new Thread(
                    () -> {
                        try {
                            buildingTerminal.countDown();
                            if (!releaseTerminal.await(10, SECONDS)) {
                                terminal.completeExceptionally(
                                        new IllegalStateException("Terminal installation was not released"));
                                return;
                            }
                            terminal.complete(dumb);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            terminal.completeExceptionally(e);
                        }
                    },
                    "deferred-terminal-build");
            buildThread.setDaemon(true);
            injectTerminal(terminal);
            buildThread.start();
            assertTrue(buildingTerminal.await(10, SECONDS));

            MavenCli cli = new MavenCli();
            CliRequest request = new CliRequest(
                    "batch".equals(color) ? new String[] {"-B"} : new String[] {"-Dstyle.color=" + color}, null);
            cli.cli(request);
            cli.properties(request);
            FutureTask<Void> logging = new FutureTask<>(() -> {
                cli.logging(request);
                return null;
            });
            loggingThread = new Thread(logging, "logging-initialization-test");
            loggingThread.setDaemon(true);
            loggingThread.start();

            // Wait until the logging thread is blocked on awaitTerminalInitialization() →
            // DeferredTerminal.getTerminal() → CompletableFuture.join(). At this point,
            // config.init() has NOT yet run (it comes after awaitTerminalInitialization()).
            awaitThreadState(loggingThread, Thread.State.WAITING, 10_000);

            // Release the terminal — the consumer sets System.out=terminalStream.
            // awaitTerminalInitialization() returns, then config.init() captures terminalStream.
            releaseTerminal.countDown();
            logging.get(10, SECONDS);

            LoggerFactory.getLogger(MavenCliLoggingTest.class).warn("terminal-output-marker");
            assertTrue(terminalOutput.toString(UTF_8.name()).contains("terminal-output-marker"));
            assertFalse(earlyOutput.toString(UTF_8.name()).contains("terminal-output-marker"));
        } finally {
            releaseTerminal.countDown();
            if (loggingThread != null) {
                loggingThread.join(10000);
            }
        }
    }

    /**
     * Waits until the given thread enters the expected state or the timeout expires.
     * Used to detect when a thread is blocked on {@code CompletableFuture.get()} inside
     * {@code DeferredTerminal.getTerminal()}.  This is more reliable than a fixed sleep and
     * avoids depending on platform-specific scheduling behaviour.
     *
     * @param thread    the thread to monitor
     * @param expected  the expected thread state (typically {@link Thread.State#WAITING})
     * @param timeoutMs maximum time to wait in milliseconds
     */
    private static void awaitThreadState(Thread thread, Thread.State expected, long timeoutMs)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (thread.getState() != expected) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Thread " + thread.getName() + " did not reach state " + expected + " within "
                        + timeoutMs + "ms; actual state: " + thread.getState());
            }
            Thread.sleep(10);
        }
    }

    /**
     * Injects the given terminal directly into {@link MessageUtils#terminal} without creating a
     * {@code LineReader} from it.  This avoids calling {@link MessageUtils#systemInstall(Terminal)}
     * which on master tries to build a {@code LineReader} — that calls
     * {@link DeferredTerminal#getTerminal()} which blocks until the build thread completes.
     */
    private static void injectTerminal(Terminal terminal) throws Exception {
        Field field = MessageUtils.class.getDeclaredField("terminal");
        field.setAccessible(true);
        field.set(null, terminal);
    }

    /**
     * Reads the raw {@code colorEnabled} field from {@link MessageUtils} without requiring a
     * terminal to be installed (unlike {@link MessageUtils#isColorEnabled()} which also checks
     * {@code terminal != null}).
     */
    private static boolean getColorEnabled() throws Exception {
        Field field = MessageUtils.class.getDeclaredField("colorEnabled");
        field.setAccessible(true);
        return (boolean) field.get(null);
    }

    private static void setColorEnabled(boolean value) throws Exception {
        Field field = MessageUtils.class.getDeclaredField("colorEnabled");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Terminal getTerminalField() throws Exception {
        Field field = MessageUtils.class.getDeclaredField("terminal");
        field.setAccessible(true);
        return (Terminal) field.get(null);
    }

    private static void activateSlf4j() {
        Slf4jConfiguration config = Slf4jConfigurationFactory.getConfiguration(LoggerFactory.getILoggerFactory());
        config.activate();
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
