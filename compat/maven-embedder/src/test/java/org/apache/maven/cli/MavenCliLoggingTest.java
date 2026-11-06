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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.cling.logging.Slf4jConfigurationFactory;
import org.apache.maven.jline.FastTerminal;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.slf4j.MavenBaseLogger;
import org.jline.terminal.Terminal;
import org.jline.terminal.spi.TerminalExt;
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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@Isolated("Changes JVM-wide streams, system properties, terminal state, and logging configuration")
class MavenCliLoggingTest {

    private PrintStream savedOut;
    private Terminal savedTerminal;
    private boolean savedColor;

    @BeforeEach
    void setUp() throws Exception {
        savedOut = System.out;
        savedTerminal = getTerminalField();
        savedColor = getColorEnabled();
        // Null out the terminal so leftover FastTerminal threads from other tests cannot
        // race with this test's stream setup via their consumer callbacks.
        injectTerminal(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(savedOut);
        injectTerminal(savedTerminal);
        setColorEnabled(savedColor);
        // Clear both properties and reactivate the logger so it writes to the restored
        // System.err (the default).  Always clear rather than restore: activateSlf4j()
        // must run against a clean slate to reset the logger state correctly.
        System.clearProperty(MavenBaseLogger.LEGACY_PREFIX + "logFile");
        System.clearProperty(MavenBaseLogger.LEGACY_PREFIX + "cacheOutputStream");
        activateSlf4j();
    }

    /**
     * When color is explicitly forced ({@code -Dstyle.color=always}), {@code logging()} must NOT
     * call {@code awaitTerminalInitialization()} before activating the logging configuration.
     * The logger must therefore cache the stream that was active at activation time (the "early"
     * stream), not the stream installed by the terminal consumer afterwards.
     * <p>
     * On master, {@code MavenSimpleLogger.configure()} renders ANSI-coloured level labels via
     * {@code JLineMessageBuilderFactory}, which calls {@code terminal.getType()} and therefore
     * always awaits terminal initialisation inside {@code activate()}.  The key behavioral
     * difference being tested is that with {@code color=always}, {@code config.init()} captures
     * {@code System.out} <em>before</em> the terminal consumer runs (because
     * {@code awaitTerminalInitialization()} is skipped before {@code activate()}).
     * <p>
     * This test uses a held-back {@link FastTerminal} instead of a {@code DumbTerminal}.  The
     * terminal build blocks until the test releases it.  Since {@code logging()} with
     * {@code color=always} does not call {@code awaitTerminalInitialization()} before
     * {@code activate()}, the logger caches {@code earlyStream} in {@code config.init()}, then
     * blocks inside {@code configure()} waiting for the terminal.  When the test releases the
     * terminal, the consumer swaps {@code System.out} to {@code terminalStream}, but the logger
     * has already cached {@code earlyStream}.
     */
    @Test
    void withColorForcedLoggerCachesEarlyStream() throws Exception {
        CountDownLatch buildingTerminal = new CountDownLatch(1);
        CountDownLatch releaseTerminal = new CountDownLatch(1);
        ByteArrayOutputStream earlyOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream terminalOutput = new ByteArrayOutputStream();
        Thread loggingThread = null;
        try (PrintStream earlyStream = new PrintStream(earlyOutput, true, UTF_8.name());
                PrintStream terminalStream = new PrintStream(terminalOutput, true, UTF_8.name())) {
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "cacheOutputStream", "true");
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "logFile", "System.out");
            System.setOut(earlyStream);
            activateSlf4j();

            FastTerminal terminal = new FastTerminal(
                    () -> {
                        buildingTerminal.countDown();
                        if (!releaseTerminal.await(10, SECONDS)) {
                            throw new IllegalStateException("Terminal installation was not released");
                        }
                        return mock(TerminalExt.class, RETURNS_DEEP_STUBS);
                    },
                    installed -> System.setOut(terminalStream));
            injectTerminal(terminal);
            terminal.start();
            assertTrue(buildingTerminal.await(10, SECONDS), "FastTerminal build thread must start");

            // Run logging() on a separate thread — it will block inside configure() when
            // toAnsi() calls FastTerminal.getType() → getTerminal() → terminal.get().
            MavenCli cli = new MavenCli();
            CliRequest request = new CliRequest(new String[] {"-Dstyle.color=always"}, null);
            cli.cli(request);
            cli.properties(request);
            FutureTask<Void> logging = new FutureTask<>(() -> {
                cli.logging(request);
                return null;
            });
            loggingThread = new Thread(logging, "logging-initialization-test");
            loggingThread.setDaemon(true);
            loggingThread.start();

            // Wait until the logging thread is blocked on the terminal's CompletableFuture.
            // At that point, config.init() has already captured earlyStream (it runs before
            // configure() which is where the block occurs).
            awaitThreadState(loggingThread, Thread.State.WAITING, 10_000);

            // Release the terminal — the consumer sets System.out=terminalStream, but the
            // logger has already cached earlyStream in config.init().
            releaseTerminal.countDown();
            logging.get(10, SECONDS);

            // The logger must still write to earlyStream.
            LoggerFactory.getLogger(MavenCliLoggingTest.class).warn("terminal-output-marker");
            assertTrue(
                    earlyOutput.toString(UTF_8.name()).contains("terminal-output-marker"),
                    "Logger must write to the stream that was active when logging() ran with -color=always");
            assertFalse(
                    terminalOutput.toString(UTF_8.name()).contains("terminal-output-marker"),
                    "Logger must not switch to the stream installed after logging() completed");
        } finally {
            releaseTerminal.countDown();
            if (loggingThread != null) {
                loggingThread.join(10_000);
            }
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
     */
    @Test
    void logFileBranchAwaitsTerminalBeforeRedirect(@TempDir Path tempDir) throws Exception {
        PrintStream originalOut = System.out;

        Path logFile = tempDir.resolve("maven.log");
        Files.writeString(logFile, "");

        CountDownLatch buildingTerminal = new CountDownLatch(1);
        CountDownLatch releaseTerminal = new CountDownLatch(1);
        // Set by the terminal consumer; true means the consumer fired before System.out was
        // redirected to the log file — which is the correct behaviour: the consumer fires
        // BEFORE the log redirect, so it cannot overwrite it.
        AtomicBoolean consumerFiredBeforeRedirect = new AtomicBoolean(false);
        Thread loggingThread = null;
        try {
            FastTerminal terminal = new FastTerminal(
                    () -> {
                        buildingTerminal.countDown();
                        if (!releaseTerminal.await(10, SECONDS)) {
                            throw new IllegalStateException("Terminal installation was not released");
                        }
                        return mock(TerminalExt.class, RETURNS_DEEP_STUBS);
                    },
                    installed -> {
                        // The consumer fires as part of terminal.complete() inside the build thread.
                        // If awaitTerminalInitialization() was called, the consumer fires BEFORE
                        // logging() redirects System.out to the log file.  We detect this by
                        // checking whether System.out is still the original stream at this point.
                        consumerFiredBeforeRedirect.set(System.out == originalOut);
                    });
            injectTerminal(terminal);
            terminal.start();
            assertTrue(buildingTerminal.await(10, SECONDS), "FastTerminal build thread must start");

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

            // Release the terminal; logging() will unblock from awaitTerminalInitialization(),
            // the consumer fires, then the redirect happens.
            releaseTerminal.countDown();
            logging.get(10, SECONDS);

            // The consumer must have fired before System.out was redirected to the log file.
            assertTrue(
                    consumerFiredBeforeRedirect.get(),
                    "Terminal consumer must fire before System.out is redirected to the log file; "
                            + "if it fires after, it could overwrite the log-file redirect (#12188)");

            // After logging() returns, System.out must be redirected to the log file (not original).
            assertFalse(
                    System.out == originalOut,
                    "After logging() with -l, System.out must be redirected to the log file");
        } finally {
            releaseTerminal.countDown();
            if (loggingThread != null) {
                loggingThread.join(10_000);
            }
            // Close the PrintStream wrapping maven.log so @TempDir can delete the temp directory on Windows.
            // An open FileOutputStream prevents directory deletion on Windows.
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
     * <em>terminal</em> stream, not the early stream.  The held-back {@link FastTerminal}
     * approach works cleanly here: {@code logging()} calls {@code awaitTerminalInitialization()}
     * which blocks until the terminal is released.  When released, the consumer swaps
     * {@code System.out}, then {@code config.init()} captures the new stream.
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
                PrintStream terminalStream = new PrintStream(terminalOutput, true, UTF_8.name())) {
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "cacheOutputStream", "true");
            System.setProperty(MavenBaseLogger.LEGACY_PREFIX + "logFile", "System.out");
            System.setOut(earlyStream);
            activateSlf4j();

            FastTerminal terminal = new FastTerminal(
                    () -> {
                        buildingTerminal.countDown();
                        if (!releaseTerminal.await(10, SECONDS)) {
                            throw new IllegalStateException("Terminal installation was not released");
                        }
                        return mock(TerminalExt.class, RETURNS_DEEP_STUBS);
                    },
                    installed -> System.setOut(terminalStream));
            // Inject the FastTerminal directly into MessageUtils.terminal before starting the
            // build thread, to avoid calling systemInstall(Terminal) which also creates a LineReader
            // from the terminal — that would call FastTerminal.getTerminal() and block.
            injectTerminal(terminal);
            terminal.start();
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
            // FastTerminal.getTerminal() → CompletableFuture.get().  At this point,
            // config.init() has NOT yet run (it comes after awaitTerminalInitialization()).
            awaitThreadState(loggingThread, Thread.State.WAITING, 10_000);

            // Release the terminal — the consumer sets System.out=terminalStream.
            // awaitTerminalInitialization() returns, then config.init() captures terminalStream.
            releaseTerminal.countDown();
            logging.get(10, SECONDS);

            LoggerFactory.getLogger(MavenCliLoggingTest.class).warn("terminal-output-marker");
            assertTrue(
                    terminalOutput.toString(UTF_8.name()).contains("terminal-output-marker"),
                    "Logger must write to the stream installed by the terminal consumer");
            assertFalse(
                    earlyOutput.toString(UTF_8.name()).contains("terminal-output-marker"),
                    "Logger must not write to the early stream that was active before terminal installation");
        } finally {
            releaseTerminal.countDown();
            if (loggingThread != null) {
                loggingThread.join(10_000);
            }
        }
    }

    /**
     * Waits until the given thread enters the expected state or the timeout expires.
     * Used to detect when a thread is blocked on {@code CompletableFuture.get()} inside
     * {@code FastTerminal.getTerminal()}.  This is more reliable than a fixed sleep and
     * avoids depending on platform-specific scheduling behavior.
     *
     * @param thread   the thread to monitor
     * @param expected the expected thread state (typically {@link Thread.State#WAITING})
     * @param timeoutMs maximum time to wait in milliseconds
     */
    private static void awaitThreadState(Thread thread, Thread.State expected, long timeoutMs)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
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
     * {@link FastTerminal#getTerminal()} which blocks until the build thread completes.
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
}
