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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DeferredTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link DeferredTerminal}'s non-blocking methods work correctly
 * when called before the real terminal is ready.
 */
class FastTerminalReentrancyTest {

    @AfterEach
    void tearDown() {
        try {
            MessageUtils.systemUninstall();
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    @Test
    void usingTheTerminalFromTheConsumerDoesNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String[]> probed = new CompletableFuture<>();
            installAndAwait(terminal -> probed.complete(probe()));
            assertProbe(probed.get());
        });
    }

    @Test
    void arbitraryTerminalMethodsFromTheConsumerDoNotDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<String> probed = new CompletableFuture<>();
            installAndAwait(terminal -> {
                Terminal t = MessageUtils.getTerminal();
                probed.complete(t.getName());
            });
            assertNotNull(probed.get());
        });
    }

    @Test
    void terminalAssignmentIsVisibleImmediately() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            installAndAwait(terminal -> {});
            Terminal t = MessageUtils.getTerminal();
            assertNotNull(t, "MessageUtils.getTerminal() must not return null after systemInstall()");
            assertTrue(t instanceof DeferredTerminal, "terminal should be the DeferredTerminal wrapper");
        });
    }

    @Test
    void writerWorksBeforeTerminalIsReady() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            installAndAwait(terminal -> {});
            Terminal t = MessageUtils.getTerminal();
            assertNotNull(t.writer(), "writer() must not return null");
            t.writer().print("test");
            t.writer().flush();
        });
    }

    @Test
    void getTypeReturnsNonNullBeforeReady() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            installAndAwait(terminal -> {});
            Terminal t = MessageUtils.getTerminal();
            assertNotNull(t.getType(), "getType() must not return null");
        });
    }

    private static String[] probe() {
        String rendered = MessageUtils.builder().warning("WARNING").build();
        assertNotNull(MessageUtils.getTerminal().writer());
        return new String[] {rendered, MessageUtils.getTerminal().getType()};
    }

    private static void assertProbe(String[] probed) {
        assertEquals("WARNING", probed[0]);
        assertNotNull(probed[1]);
    }

    private void installAndAwait(java.util.function.Consumer<Terminal> onTerminal) {
        MessageUtils.systemInstall(builder -> {}, onTerminal);
        MessageUtils.awaitTerminalInitialization();
    }
}
