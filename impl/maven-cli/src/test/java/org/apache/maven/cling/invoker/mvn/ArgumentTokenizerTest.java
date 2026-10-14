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
package org.apache.maven.cling.invoker.mvn;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgumentTokenizerTest {

    @Test
    void emptyInput() {
        assertEquals(List.of(), ArgumentTokenizer.tokenize(""));
        assertEquals(List.of(), ArgumentTokenizer.tokenize("   "));
    }

    @Test
    void simpleTokens() {
        assertEquals(List.of("clean", "install"), ArgumentTokenizer.tokenize("clean install"));
    }

    @Test
    void doubleQuotes() {
        assertEquals(List.of("-Dfoo=hello world"), ArgumentTokenizer.tokenize("\"-Dfoo=hello world\""));
    }

    @Test
    void singleQuotes() {
        assertEquals(List.of("-Dfoo=hello world"), ArgumentTokenizer.tokenize("'-Dfoo=hello world'"));
    }

    @Test
    void backslashEscapeSpace() {
        assertEquals(List.of("-Dfoo=hello world"), ArgumentTokenizer.tokenize("-Dfoo=hello\\ world"));
    }

    @Test
    void backslashEscapeDoubleQuote() {
        assertEquals(List.of("-Dfoo=say \"hi\""), ArgumentTokenizer.tokenize("-Dfoo=say\\ \\\"hi\\\""));
    }

    @Test
    void backslashEscapeInsideDoubleQuotes() {
        // \" inside double-quotes produces a literal "
        assertEquals(List.of("a\"b"), ArgumentTokenizer.tokenize("\"a\\\"b\""));
    }

    @Test
    void backslashNoSpecialInsideSingleQuotes() {
        // Inside single-quotes, backslash is literal — \n stays as two chars
        assertEquals(List.of("a\\nb"), ArgumentTokenizer.tokenize("'a\\nb'"));
    }

    @Test
    void backslashEscapeBackslash() {
        assertEquals(List.of("a\\b"), ArgumentTokenizer.tokenize("a\\\\b"));
    }

    @Test
    void mixedQuotes() {
        assertEquals(List.of("-Da=foo bar", "-Db=baz"), ArgumentTokenizer.tokenize("\"-Da=foo bar\" '-Db=baz'"));
    }

    @Test
    void extraWhitespace() {
        assertEquals(List.of("a", "b", "c"), ArgumentTokenizer.tokenize("  a   b   c  "));
    }

    @Test
    void unclosedDoubleQuoteThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ArgumentTokenizer.tokenize("clean \"install"),
                "Unclosed double quote must throw");
    }

    @Test
    void unclosedSingleQuoteThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ArgumentTokenizer.tokenize("clean 'install"),
                "Unclosed single quote must throw");
    }
}
