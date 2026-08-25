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

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * {@link MessageUtils#systemInstall} publishes the terminal before the background thread has built
 * it, so anything that thread logs is rendered through a terminal that same thread is still
 * producing. See <a href="https://github.com/apache/maven/issues/12761">#12761</a>.
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
    void renderingAMessageFromTheBuilderDoesNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String> rendered = new CompletableFuture<>();
            installAndAwait(builder ->
                    rendered.complete(MessageUtils.builder().warning("WARNING").build()));
            // the stand-in reports itself as dumb, so the style is dropped rather than emitted blind
            assertEquals("WARNING", rendered.get());
        });
    }

    @Test
    void takingTheWriterFromTheBuilderDoesNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<Object> writer = new CompletableFuture<>();
            installAndAwait(
                    builder -> writer.complete(MessageUtils.getTerminal().writer()));
            assertNotNull(writer.get());
        });
    }

    @Test
    void renderingAMessageFromTheConsumerDoesNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String> rendered = new CompletableFuture<>();
            // the consumer runs on the same thread, before the future is completed
            installAndAwait(
                    builder -> {},
                    terminal -> rendered.complete(
                            MessageUtils.builder().warning("WARNING").build()));
            assertEquals("WARNING", rendered.get());
        });
    }

    private void installAndAwait(java.util.function.Consumer<org.jline.terminal.TerminalBuilder> onBuilder) {
        installAndAwait(onBuilder, terminal -> {});
    }

    private void installAndAwait(
            java.util.function.Consumer<org.jline.terminal.TerminalBuilder> onBuilder,
            java.util.function.Consumer<Terminal> onTerminal) {
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
