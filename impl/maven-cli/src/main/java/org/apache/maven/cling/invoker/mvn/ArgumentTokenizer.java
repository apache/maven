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

import java.util.ArrayList;
import java.util.List;

/**
 * Quote-aware argument tokenizer for the shorthand form of {@code <options>} and {@code <alias>} elements
 * in {@code .mvn/reactor.xml}.
 *
 * <p>The algorithm is identical to {@code JvmConfigParser.parseArguments()} (the single-file source launcher
 * used by shell scripts to parse {@code .mvn/jvm.config}):
 * <ul>
 *   <li>Whitespace is the delimiter outside quotes.</li>
 *   <li>{@code "..."} and {@code '...'} are both supported — quotes are stripped, content is kept as a
 *       single token.</li>
 *   <li>Outside single-quotes, a backslash ({@code \}) escapes the next character literally
 *       (e.g. {@code \"} → {@code "}, {@code \\} → {@code \}, {@code \ } → a literal space).
 *       Inside single-quotes, backslash has no special meaning.</li>
 * </ul>
 *
 * <p><strong>Note:</strong> {@code JvmConfigParser.java} is a self-contained single-file source launcher
 * invoked directly by shell scripts; it must not share classpath code with Maven. This class is an
 * independent re-implementation of the same algorithm for use within Maven's Java classpath.
 *
 * @since 4.1.0
 */
public final class ArgumentTokenizer {

    private ArgumentTokenizer() {
        // utility class
    }

    /**
     * Splits a string into a list of argument tokens using quote-aware whitespace splitting.
     *
     * @param input the input string to tokenize, must not be {@code null}
     * @return an unmodifiable list of token strings; empty if the input contains only whitespace
     */
    public static List<String> tokenize(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && !inSingleQuotes && i + 1 < input.length()) {
                // Backslash escape: outside single-quotes, \X is always a literal X.
                // This matches POSIX behaviour in both unquoted and double-quoted contexts.
                current.append(input.charAt(++i));
            } else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (Character.isWhitespace(c) && !inDoubleQuotes && !inSingleQuotes) {
                // Whitespace outside quotes — end of current token
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        // Add last token if any
        if (current.length() > 0) {
            args.add(current.toString());
        }

        if (inDoubleQuotes || inSingleQuotes) {
            throw new IllegalArgumentException(
                    "Unclosed " + (inDoubleQuotes ? "double" : "single") + " quote in: " + input);
        }

        return List.copyOf(args);
    }
}
