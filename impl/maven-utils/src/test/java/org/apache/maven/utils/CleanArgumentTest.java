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
package org.apache.maven.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CleanArgumentTest {
    @Test
    void preservesEmptyArguments() {
        assertArrayEquals(new String[0], CleanArgument.cleanArgs(new String[0]));
    }

    @Test
    void preservesUnquotedArguments() {
        String[] args = {"test", "-Dfoo=bar"};
        assertArrayEquals(args, CleanArgument.cleanArgs(args));
    }

    @Test
    void removesSurroundingDoubleQuotes() {
        String[] args = {"\"-Dfoo=bar two\"", "test"};
        assertArrayEquals(new String[] {"-Dfoo=bar two", "test"}, CleanArgument.cleanArgs(args));
    }

    @Test
    void handlesAnEmptyQuotedArgument() {
        assertArrayEquals(new String[] {""}, CleanArgument.cleanArgs(new String[] {"\"\""}));
    }

    @Test
    void handlesASingleDoubleQuote() {
        assertArrayEquals(new String[] {""}, CleanArgument.cleanArgs(new String[] {"\""}));
    }

    @Test
    void handlesUnterminatedQuotesBeforeAnotherQuotedArgument() {
        String[] args = {"\"-Dfoo=bar", "\"-Dfoo2=bar two\""};
        assertArrayEquals(new String[] {"-Dfoo=bar", "-Dfoo2=bar two"}, CleanArgument.cleanArgs(args));
    }

    @Test
    void combinesFragmentsAfterAnOpeningQuote() {
        String[] args = {"\"-Dfoo=bar", "two"};
        assertArrayEquals(new String[] {"-Dfoo=bar two"}, CleanArgument.cleanArgs(args));
    }

    @Test
    void preservesDoubleQuotesInsideAnArgument() {
        String[] args = {"-Dinformation=\"The Information is important.\""};
        assertArrayEquals(args, CleanArgument.cleanArgs(args));
    }

    @Test
    void preservesSingleQuotesInsideAnArgument() {
        String[] args = {"-Dinformation='The Information is important.'"};
        assertArrayEquals(args, CleanArgument.cleanArgs(args));
    }
}
