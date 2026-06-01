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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link FastTerminal#getTerminal()} waits for the background setup to finish before it
 * returns. {@link MessageUtils#awaitTerminalInitialization()} relies on this so the {@code -l} log
 * file redirection cannot be overwritten by the late terminal install
 * (see <a href="https://github.com/apache/maven/issues/12188">#12188</a>).
 */
class FastTerminalTest {

    /** Safety net so a broken implementation fails fast instead of hanging the build forever. */
    private static final Duration MAX_WAIT = Duration.ofSeconds(30);

    @Test
    void getTerminalBlocksUntilConsumerHasCompleted() throws Exception {
        Terminal dumb = TerminalBuilder.builder().dumb(true).build();

        CountDownLatch builderProceed = new CountDownLatch(1);
        AtomicBoolean consumerCompleted = new AtomicBoolean(false);

        FastTerminal fastTerminal = new FastTerminal(
                () -> {
                    // pretend the terminal is slow to build, like it often is on macOS
                    builderProceed.await();
                    return dumb;
                },
                terminal -> {
                    // this is where AnsiConsole.systemInstall() and the stream swap would happen
                    consumerCompleted.set(true);
                });

        // the build is still blocked, so the consumer has not run yet
        assertFalse(consumerCompleted.get(), "consumer must not run before the terminal is built");

        // let the setup finish
        builderProceed.countDown();

        // getTerminal() should only come back once the consumer is done
        assertTimeoutPreemptively(MAX_WAIT, fastTerminal::getTerminal);
        assertTrue(consumerCompleted.get(), "getTerminal() must block until the consumer has completed");

        dumb.close();
    }

    @Test
    void getTerminalReturnsSameReadyTerminalOnRepeatedCalls() throws Exception {
        Terminal dumb = TerminalBuilder.builder().dumb(true).build();
        AtomicInteger consumerInvocations = new AtomicInteger();

        FastTerminal fastTerminal = new FastTerminal(() -> dumb, terminal -> consumerInvocations.incrementAndGet());

        assertTimeoutPreemptively(MAX_WAIT, () -> {
            assertSame(dumb, fastTerminal.getTerminal());
            // calling it again returns the same terminal without re-running the setup
            assertSame(dumb, fastTerminal.getTerminal());
        });
        assertEquals(1, consumerInvocations.get(), "the consumer must run exactly once");

        dumb.close();
    }
}
