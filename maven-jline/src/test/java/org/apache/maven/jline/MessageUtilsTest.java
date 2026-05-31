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

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MessageUtilsTest {

    @AfterEach
    void reset() {
        MessageUtils.terminal = null;
    }

    @Test
    void awaitTerminalInitializationIsNoOpWhenNoTerminalInstalled() {
        MessageUtils.terminal = null;
        assertDoesNotThrow(MessageUtils::awaitTerminalInitialization);
    }

    @Test
    void awaitTerminalInitializationIsNoOpForPlainTerminal() throws Exception {
        // a plain terminal (e.g. embedded use) has nothing to wait for and must return at once
        try (Terminal terminal = TerminalBuilder.builder().dumb(true).build()) {
            MessageUtils.terminal = terminal;
            assertDoesNotThrow(MessageUtils::awaitTerminalInitialization);
        }
    }
}
