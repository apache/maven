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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.junit.jupiter.api.io.TempDir;
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
    private CompletableFuture<Void> savedTerminalReady;
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
        savedTerminalReady = getTerminalReadyField();
        // Null out the terminal so leftover DeferredTerminal threads from other tests cannot
        // race with this test's stream setup via their consumer callbacks.
        injectTerminal(null);
        clearTerminalReady();
        for (int i = 0; i < SAVED_PROPERTIES.length; i++) {
            savedPropertyValues[i] = System.getProperty(SAVED_PROPERTIES[i]);
        }
        for (String prop : SAVED_PROPERTIES) {
            System.clearProperty(prop);
        }
        activateSlf4j();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(savedOut);
        injectTerminal(savedTerminal);
        setTerminalReady(savedTerminalReady);
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
     * When {@code -l <logfile>} is used, {@code logging()} must call
     * {@link MessageUtils#awaitTerminalInitialization()} before redirecting {@code System.out}
     * to the log file.  Without the await, the terminal's installed consumer (which may call
     * {@code System.setOut}) could race with the log-file redirect, causing logs to leak to
     * the console instead of the log file (see
     * <a href="https://github.com/apache/maven/issues/12188">#12188</a>).
     * <p>
     * This test verifies that the terminal consumer fires <em>before</em> {@code System.out} is
     * redirected to the log file, so the consumer cannot overwrite the log-file redirect.
     * <p>
     * The {@code terminalReady} future is wired through the {@code onComplete} callback so that
     * {@link MessageUtils#awaitTerminalInitialization()} joins on {@code terminalReady} (the strong
     * path) rather than falling back to {@code deferred.getTerminal()} (which only blocks until the
     * {@link java.util.concurrent.CompletableFuture} inside {@link org.jline.terminal.impl.DeferredTerminal}
     * completes — before the {@code onComplete} callback runs — and would introduce a race).
     */
    @Test
    void logFileBranchAwaitsTerminalBeforeRedirect(@TempDir Path tempDir) throws Exception {
        PrintStream originalOut = System.out;

        Path logFile = tempDir.resolve("maven.log");
        Files.writeString(logFile, "");

        CountDownLatch buildingTerminal = new CountDownLatch(1);
        CountDownLatch releaseTerminal = new CountDownLatch(1);
        AtomicBoolean consumerFiredBeforeRedirect = new AtomicBoolean(false);
        Thread loggingThread = null;
        try (Terminal dumb = new DumbTerminal(
                "Maven",
                Terminal.TYPE_DUMB_COLOR,
                InputStream.nullInputStream(),
                OutputStream.nullOutputStream(),
                null)) {
            DeferredTerminal terminal = new DeferredTerminal();
            CompletableFuture<Void> terminalReady = new CompletableFuture<>();
            terminal.onComplete(installed -> {
                consumerFiredBeforeRedirect.set(System.out == originalOut);
                terminalReady.complete(null);
            });
            Thread buildThread = new Thread(
                    () -> {
                        try {
                            buildingTerminal.countDown();
                            if (!releaseTerminal.await(10, SECONDS)) {
                                IllegalStateException ex =
                                        new IllegalStateException("Terminal installation was not released");
                                terminal.completeExceptionally(ex);
                                terminalReady.completeExceptionally(ex);
                                return;
                            }
                            terminal.complete(dumb);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            terminal.completeExceptionally(e);
                            terminalReady.completeExceptionally(e);
                        }
                    },
                    "deferred-terminal-build");
            buildThread.setDaemon(true);
            injectTerminal(terminal, terminalReady);
            buildThread.start();
            assertTrue(buildingTerminal.await(10, SECONDS), "DeferredTerminal build thread must start");

            MavenCli cli = new MavenCli();
            CliRequest request =
                    new CliRequest(new String[] {"-l", logFile.toAbsolutePath().toString()}, null);
            cli.cli(request);
            cli.properties(request);
            FutureTask<Void> logging = new FutureTask<>(() -> {
                cli.logging(request);
                return null;
            });
            loggingThread = new Thread(logging, "logging-initialization-test");
            loggingThread.setDaemon(true);
            loggingThread.start();

            releaseTerminal.countDown();
            logging.get(10, SECONDS);

            assertTrue(
                    consumerFiredBeforeRedirect.get(),
                    "Terminal consumer must fire before System.out is redirected to the log file; "
                            + "if it fires after, it could overwrite the log-file redirect (#12188)");

            assertFalse(
                    System.out == originalOut,
                    "After logging() with -l, System.out must be redirected to the log file");
        } finally {
            releaseTerminal.countDown();
            if (loggingThread != null) {
                loggingThread.join(10_000);
            }
            PrintStream logStream = System.out;
            System.setOut(originalOut);
            if (logStream != originalOut) {
                logStream.close();
            }
        }
    }

    /**
     * When color is set to {@code never} or {@code auto} (batch mode), {@code logging()} must call
     * {@code awaitTerminalInitialization()} before activating the logging configuration, so that
     * the terminal consumer has already swapped {@code System.out} before the logger caches it.
     * This prevents logs from going to a stale stream instead of the terminal-wrapped one.
     * <p>
     * Unlike the {@code color=always} test, this test verifies that the logger caches the
     * <em>terminal</em> stream, not the early stream.  The held-back {@link DeferredTerminal}
     * approach works cleanly here: {@code logging()} calls {@code awaitTerminalInitialization()}
     * which blocks until the terminal is released.  When released, the consumer swaps
     * {@code System.out}, then {@code config.init()} captures the new stream.
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
            CompletableFuture<Void> terminalReady = new CompletableFuture<>();
            terminal.onComplete(installed -> {
                System.setOut(terminalStream);
                terminalReady.complete(null);
            });
            Thread buildThread = new Thread(
                    () -> {
                        try {
                            buildingTerminal.countDown();
                            if (!releaseTerminal.await(10, SECONDS)) {
                                IllegalStateException ex =
                                        new IllegalStateException("Terminal installation was not released");
                                terminal.completeExceptionally(ex);
                                terminalReady.completeExceptionally(ex);
                                return;
                            }
                            terminal.complete(dumb);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            terminal.completeExceptionally(e);
                            terminalReady.completeExceptionally(e);
                        }
                    },
                    "deferred-terminal-build");
            buildThread.setDaemon(true);
            injectTerminal(terminal, terminalReady);
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
            // terminalReady.join(). At this point, config.init() has NOT yet run (it
            // comes after awaitTerminalInitialization()).
            awaitThreadState(loggingThread, Thread.State.WAITING, 10_000);

            // Release the terminal — the onComplete callback sets System.out=terminalStream
            // and completes terminalReady. awaitTerminalInitialization() returns only after
            // onComplete finishes, then config.init() captures terminalStream.
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
     * Used to detect when a thread is blocked on {@code CompletableFuture.join()} inside
     * {@code DeferredTerminal.getTerminal()}.
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
     * {@code LineReader} from it.
     */
    private static void injectTerminal(Terminal terminal) throws Exception {
        Field field = MessageUtils.class.getDeclaredField("terminal");
        field.setAccessible(true);
        field.set(null, terminal);
    }

    /**
     * Injects the given terminal and its associated {@code terminalReady} future into
     * {@link MessageUtils}, mirroring what {@link MessageUtils#systemInstall} does so that
     * {@link MessageUtils#awaitTerminalInitialization()} correctly waits for the
     * {@code onComplete} callback to finish.
     */
    private static void injectTerminal(Terminal terminal, CompletableFuture<Void> terminalReady) throws Exception {
        Field terminalField = MessageUtils.class.getDeclaredField("terminal");
        terminalField.setAccessible(true);
        terminalField.set(null, terminal);
        Field readyField = MessageUtils.class.getDeclaredField("terminalReady");
        readyField.setAccessible(true);
        readyField.set(null, terminalReady);
    }

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

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Void> getTerminalReadyField() throws Exception {
        Field field = MessageUtils.class.getDeclaredField("terminalReady");
        field.setAccessible(true);
        return (CompletableFuture<Void>) field.get(null);
    }

    private static void clearTerminalReady() throws Exception {
        Field field = MessageUtils.class.getDeclaredField("terminalReady");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static void setTerminalReady(CompletableFuture<Void> value) throws Exception {
        Field field = MessageUtils.class.getDeclaredField("terminalReady");
        field.setAccessible(true);
        field.set(null, value);
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
