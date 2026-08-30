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

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MessageUtils#systemInstall} publishes the terminal before the background thread has built
 * it, so anything that thread logs is rendered through a terminal that same thread is still
 * producing. See <a href="https://github.com/apache/maven/issues/12761">#12761</a> and
 * <a href="https://github.com/apache/maven/issues/12912">#12912</a>.
 * <p>
 * A single log statement asks the terminal for two things, its type while rendering the message and
 * its writer while emitting the line, so both are exercised from both halves of the window: the
 * builder callable, and the consumer that runs before the terminal is published.
 * <p>
 * JLine 4.4.0's FFM provider initialization ({@code CLibrary.<clinit>}) can reach back through
 * other terminal methods (e.g. {@code getName()}, {@code getWidth()}, {@code encoding()}) on the
 * build thread, so the fallback must cover all delegate methods, not just {@code writer()} and
 * {@code getType()}.
 * <p>
 * The timeouts are preemptive on purpose: a regression parks the build thread forever, and only an
 * abandoning timeout turns that into a red test rather than a hung fork.
 */
class FastTerminalReentrancyTest {

    @AfterEach
    void tearDown() {
        // MessageUtils.terminal is process-global; leaving it set breaks every later test.
        // On a regression the build thread never finishes and systemUninstall waits for it (see
        // #11048), on the test thread and outside any timeout, so the fork would hang instead of
        // reporting the failure. Leave the state dirty in that case; the run is lost either way.
        if (MessageUtils.getTerminal() instanceof FastTerminal ft && !ft.isBuilt()) {
            return;
        }
        MessageUtils.systemUninstall();
    }

    @Test
    void usingTheTerminalFromTheBuilderDoesNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String[]> probed = new CompletableFuture<>();
            installAndAwait(builder -> probed.complete(probe()), terminal -> {});
            assertProbe(probed.get());
        });
    }

    @Test
    void usingTheTerminalFromTheConsumerDoesNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String[]> probed = new CompletableFuture<>();
            installAndAwait(builder -> {}, terminal -> probed.complete(probe()));
            assertProbe(probed.get());
        });
    }

    /**
     * Exercises terminal methods beyond {@code writer()} and {@code getType()} that JLine's FFM
     * provider initialization can reach on the build thread. Before the fallback terminal was added,
     * these would deadlock. See <a href="https://github.com/apache/maven/issues/12912">#12912</a>.
     */
    @Test
    void arbitraryTerminalMethodsFromTheBuilderDoNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<int[]> probed = new CompletableFuture<>();
            installAndAwait(
                    builder -> {
                        Terminal t = MessageUtils.getTerminal();
                        probed.complete(
                                new int[] {t.getSize().getColumns(), t.getSize().getRows()});
                    },
                    terminal -> {});
            int[] dims = probed.get();
            assertTrue(dims[0] >= 0, "width should be non-negative");
            assertTrue(dims[1] >= 0, "height should be non-negative");
        });
    }

    /**
     * Exercises terminal methods beyond {@code writer()} and {@code getType()} from the consumer
     * callback. See <a href="https://github.com/apache/maven/issues/12912">#12912</a>.
     */
    @Test
    void arbitraryTerminalMethodsFromTheConsumerDoNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String> probed = new CompletableFuture<>();
            installAndAwait(builder -> {}, terminal -> {
                Terminal t = MessageUtils.getTerminal();
                probed.complete(t.getName());
            });
            assertNotNull(probed.get());
        });
    }

    /**
     * Verifies that {@link MessageUtils#getTerminal()} is non-null when called from the builder
     * callback. Before the fix, the {@code FastTerminal} constructor started its build thread
     * before returning, so {@code MessageUtils.terminal} was still {@code null} when the build
     * thread ran the builder callback &mdash; a race between the constructor returning and the
     * thread scheduling. After the fix, {@code MessageUtils} assigns the field before calling
     * {@link FastTerminal#start()}, and {@link Thread#start()} provides the happens-before edge.
     *
     * @see <a href="https://github.com/apache/maven/issues/12912">#12912</a>
     */
    @Test
    void terminalAssignmentIsVisibleFromBuilderCallback() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<Terminal> observed = new CompletableFuture<>();
            installAndAwait(builder -> observed.complete(MessageUtils.getTerminal()), terminal -> {});
            assertNotNull(observed.get(), "MessageUtils.getTerminal() must not return null from the builder callback");
            assertTrue(observed.get() instanceof FastTerminal, "terminal should be the FastTerminal wrapper");
        });
    }

    /**
     * Both terminal calls a single log statement makes, run on the terminal building thread.
     */
    private static String[] probe() {
        String rendered = MessageUtils.builder().warning("WARNING").build();
        assertNotNull(MessageUtils.getTerminal().writer());
        return new String[] {rendered, MessageUtils.getTerminal().getType()};
    }

    private static void assertProbe(String[] probed) {
        // the stand-in reports itself dumb, so the style is dropped rather than emitted blind
        assertEquals(Terminal.TYPE_DUMB, probed[1]);
        assertEquals("WARNING", probed[0]);
    }

    private void installAndAwait(Consumer<TerminalBuilder> onBuilder, Consumer<Terminal> onTerminal) {
        MessageUtils.systemInstall(
                builder -> {
                    onBuilder.accept(builder);
                    builder.dumb(true)
                            .system(false)
                            .streams(InputStream.nullInputStream(), OutputStream.nullOutputStream());
                },
                onTerminal);
        // let the build finish, so a failure here is the build failing rather than a leaked thread
        ((FastTerminal) MessageUtils.getTerminal()).getTerminal();
    }
}
