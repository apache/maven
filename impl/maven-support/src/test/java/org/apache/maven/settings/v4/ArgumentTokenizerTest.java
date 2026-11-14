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
package org.apache.maven.settings.v4;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgumentTokenizerTest {

    @Test
    void doubleQuotedEmptyArgument() {
        assertEquals(List.of("first", "", "third"), ArgumentTokenizer.tokenize("first \"\" third"));
    }

    @Test
    void singleQuotedEmptyArgument() {
        assertEquals(List.of("first", "", "third"), ArgumentTokenizer.tokenize("first '' third"));
    }

    @Test
    void emptyArgumentsAtBoundaries() {
        assertEquals(List.of(""), ArgumentTokenizer.tokenize("\"\""));
        assertEquals(List.of(""), ArgumentTokenizer.tokenize("''"));
        assertEquals(List.of("", "middle", ""), ArgumentTokenizer.tokenize("  \"\" middle ''  "));
    }

    @Test
    void consecutiveEmptyArguments() {
        assertEquals(List.of("", "", ""), ArgumentTokenizer.tokenize("\"\"\t''\n\"\""));
        // Adjacent quoted fragments without whitespace belong to the same argument.
        assertEquals(List.of(""), ArgumentTokenizer.tokenize("\"\"''"));
    }

    @Test
    void adjacentQuotedFragmentsStayInOneArgument() {
        assertEquals(List.of("first", "third"), ArgumentTokenizer.tokenize("fi\"\"rst ''third"));
        assertEquals(List.of("firstthird"), ArgumentTokenizer.tokenize("\"first\"''third\"\""));
    }

    @Test
    void quotedWhitespaceIsPreserved() {
        assertEquals(List.of(" ", "\t"), ArgumentTokenizer.tokenize("\" \" '\t'"));
    }

    @Test
    void escapedQuotesAreLiteralArguments() {
        assertEquals(List.of("\"\"", "''", " "), ArgumentTokenizer.tokenize("\\\"\\\" \\'\\' \\ "));
    }

    @Test
    void blankInputDoesNotCreateArguments() {
        assertEquals(List.of(), ArgumentTokenizer.tokenize(""));
        assertEquals(List.of(), ArgumentTokenizer.tokenize(" \t\r\n "));
    }

    @Test
    void unclosedQuotesStillFail() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentTokenizer.tokenize("\"\" '"));
        assertThrows(IllegalArgumentException.class, () -> ArgumentTokenizer.tokenize("'' \""));
    }

    @Test
    void resultWithEmptyArgumentIsUnmodifiable() {
        List<String> arguments = ArgumentTokenizer.tokenize("\"\"");
        assertEquals(List.of(""), arguments);
        assertThrows(UnsupportedOperationException.class, () -> arguments.add("extra"));
    }
}
