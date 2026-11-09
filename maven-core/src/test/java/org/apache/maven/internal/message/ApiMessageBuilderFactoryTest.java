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
package org.apache.maven.internal.message;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiMessageBuilderFactoryTest {

    private final AtomicBoolean color = new AtomicBoolean();

    private final ApiMessageBuilderFactory factory = new ApiMessageBuilderFactory(color::get);

    @Test
    void resolvesStyleSpecsLikeMaven310() {
        assertEquals("\u001B[31;1m", ApiMessageBuilderFactory.resolve("bold,f:red"));
        assertEquals("\u001B[36;1m", ApiMessageBuilderFactory.resolve("f:cyan,bold"));
        assertEquals("\u001B[4;31;44;1m", ApiMessageBuilderFactory.resolve("bold,underline,f:red,bg:blue"));
        assertEquals("\u001B[90m", ApiMessageBuilderFactory.resolve("f:bright-black"));
        assertEquals("\u001B[38;5;208;44m", ApiMessageBuilderFactory.resolve("bg:blue,fg:208"));
        assertEquals("\u001B[38;2;255;0;16m", ApiMessageBuilderFactory.resolve("f:#ff0010"));
        assertEquals("", ApiMessageBuilderFactory.resolve("nonsense,f:nocolor"));
    }

    @Test
    void namedStylesFollowSystemProperties() {
        assertEquals("\u001B[34;1m", ApiMessageBuilderFactory.resolve(".test-unset:-bold,f:blue"));
        System.setProperty("maven.style.test-set", "f:green");
        try {
            assertEquals("\u001B[32m", ApiMessageBuilderFactory.resolve(".test-set:-bold,f:blue"));
        } finally {
            System.clearProperty("maven.style.test-set");
        }
    }

    @Test
    void colorsOnlyWhenEnabled() {
        color.set(false);
        assertEquals("a b c", factory.builder().a("a ").failure("b").a(" c").build());

        color.set(true);
        assertEquals(
                "a \u001B[31;1mb\u001B[0m c",
                factory.builder().a("a ").failure("b").a(" c").build());
        assertEquals(
                "\u001B[1mx\u001B[0m", factory.builder(8).style("bold").a("x").build());
    }
}
