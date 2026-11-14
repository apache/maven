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

/**
 * Quote-aware argument tokenizer for the shorthand form of {@code <options>} and {@code <alias>} elements
 * in {@code settings.xml}.
 *
 * <p>Delegates to {@link org.apache.maven.settings.v4.ArgumentTokenizer}, which preserves quoted empty
 * strings as empty arguments.</p>
 *
 * @since 4.2.0
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
        return org.apache.maven.settings.v4.ArgumentTokenizer.tokenize(input);
    }
}
