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

import org.apache.maven.message.MessageBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageUtilsTest {

    @AfterEach
    void reset() {
        MessageUtils.setColorEnabled(false);
    }

    @Test
    void parsesWidth() {
        assertEquals(137, MessageUtils.parseWidth("137"));
        assertEquals(137, MessageUtils.parseWidth(" 137\n"));
        assertEquals(-1, MessageUtils.parseWidth("0"));
        assertEquals(-1, MessageUtils.parseWidth("wide"));
        assertEquals(-1, MessageUtils.parseWidth(null));
    }

    @Test
    void resolvesStyleSpecs() {
        assertEquals("\u001B[1;31m", JLineMessageBuilderFactory.resolve("bold,f:red"));
        assertEquals("\u001B[90m", JLineMessageBuilderFactory.resolve("f:bright-black"));
        assertEquals("\u001B[44;38;5;208m", JLineMessageBuilderFactory.resolve("bg:blue,fg:208"));
        assertEquals("\u001B[38;2;255;0;16m", JLineMessageBuilderFactory.resolve("f:#ff0010"));
        assertEquals("", JLineMessageBuilderFactory.resolve("nonsense,f:nocolor"));
    }

    @Test
    void resolvesNamedStyleFromSystemPropertyOrDefault() {
        assertEquals("\u001B[1;34m", JLineMessageBuilderFactory.resolve(".test-unset:-bold,f:blue"));
        System.setProperty("maven.style.test-set", "f:green");
        try {
            assertEquals("\u001B[32m", JLineMessageBuilderFactory.resolve(".test-set:-bold,f:blue"));
        } finally {
            System.clearProperty("maven.style.test-set");
        }
    }

    @Test
    void builderEmitsAnsiOnlyWhenColorEnabled() {
        MessageUtils.setColorEnabled(false);
        assertEquals("a b c", MessageUtils.builder().a("a ").error("b").a(" c").build());

        MessageUtils.setColorEnabled(true);
        MessageBuilder mb = MessageUtils.builder().a("a ").error("b").a(" c");
        assertEquals("a \u001B[1;31mb\u001B[0m c", mb.build());
        assertEquals("a b c", MessageUtils.stripAnsiCodes(mb.build()));
        // an unterminated style is reset at the end
        assertEquals(
                "\u001B[1mx\u001B[0m",
                MessageUtils.builder().style("bold").a("x").build());
    }
}
