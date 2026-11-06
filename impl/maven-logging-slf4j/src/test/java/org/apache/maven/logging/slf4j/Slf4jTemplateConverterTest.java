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
package org.apache.maven.logging.slf4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class Slf4jTemplateConverterTest {

    @Test
    void nullPassesThrough() {
        assertNull(Slf4jTemplateConverter.convert(null));
    }

    @Test
    void noPlaceholderReturnsSameReference() {
        String template = "No placeholders here";
        assertSame(template, Slf4jTemplateConverter.convert(template));
    }

    @Test
    void singlePlaceholderConverted() {
        assertEquals("Resolved {0} files", Slf4jTemplateConverter.convert("Resolved {} files"));
    }

    @Test
    void multiplePlaceholdersConverted() {
        assertEquals(
                "Hello {0}, you have {1} messages", Slf4jTemplateConverter.convert("Hello {}, you have {} messages"));
    }

    @Test
    void threeArguments() {
        assertEquals("{0} + {1} = {2}", Slf4jTemplateConverter.convert("{} + {} = {}"));
    }

    @Test
    void cachedResultIsSameReference() {
        String input = "Value is {} and {}";
        String first = Slf4jTemplateConverter.convert(input);
        String second = Slf4jTemplateConverter.convert(input);
        assertSame(first, second, "Cache should return the same String instance");
    }

    @Test
    void emptyStringPassesThrough() {
        String empty = "";
        assertSame(empty, Slf4jTemplateConverter.convert(empty));
    }

    @Test
    void placeholderAtStart() {
        assertEquals("{0} is first", Slf4jTemplateConverter.convert("{} is first"));
    }

    @Test
    void placeholderAtEnd() {
        assertEquals("last is {0}", Slf4jTemplateConverter.convert("last is {}"));
    }

    @Test
    void adjacentPlaceholders() {
        assertEquals("{0}{1}", Slf4jTemplateConverter.convert("{}{}"));
    }
}
