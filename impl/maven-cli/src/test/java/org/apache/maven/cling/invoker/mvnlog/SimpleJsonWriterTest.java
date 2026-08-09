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
package org.apache.maven.cling.invoker.mvnlog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleJsonWriterTest {

    @Test
    void emptyObject() {
        assertEquals("{}", SimpleJsonWriter.toJson(Map.of()));
    }

    @Test
    void emptyArray() {
        assertEquals("[]", SimpleJsonWriter.toJson(List.of()));
    }

    @Test
    void simpleString() {
        assertEquals("\"hello\"", SimpleJsonWriter.toJson("hello"));
    }

    @Test
    void stringWithEscapes() {
        String json = SimpleJsonWriter.toJson("line1\nline2\t\"quoted\"");
        assertEquals("\"line1\\nline2\\t\\\"quoted\\\"\"", json);
    }

    @Test
    void numbers() {
        assertEquals("42", SimpleJsonWriter.toJson(42));
        assertEquals("3.14", SimpleJsonWriter.toJson(3.14));
    }

    @Test
    void booleans() {
        assertEquals("true", SimpleJsonWriter.toJson(true));
        assertEquals("false", SimpleJsonWriter.toJson(false));
    }

    @Test
    void nullValue() {
        assertEquals("null", SimpleJsonWriter.toJson(null));
    }

    @Test
    void simpleObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "test");
        map.put("count", 42);

        String json = SimpleJsonWriter.toJson(map);
        assertTrue(json.contains("\"name\": \"test\""));
        assertTrue(json.contains("\"count\": 42"));
    }

    @Test
    void nestedObject() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("key", "value");

        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("nested", inner);

        String json = SimpleJsonWriter.toJson(outer);
        assertTrue(json.contains("\"nested\": {"));
        assertTrue(json.contains("\"key\": \"value\""));
    }

    @Test
    void arrayOfStrings() {
        String json = SimpleJsonWriter.toJson(List.of("a", "b", "c"));
        assertTrue(json.contains("\"a\""));
        assertTrue(json.contains("\"b\""));
        assertTrue(json.contains("\"c\""));
    }

    @Test
    void roundTripWithSimpleJsonReader() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("status", "SUCCESS");
        original.put("count", 42);
        original.put("items", List.of("a", "b"));

        String json = SimpleJsonWriter.toJson(original);
        Map<String, Object> parsed = SimpleJsonReader.parse(json);

        assertEquals("SUCCESS", parsed.get("status"));
        // SimpleJsonReader parses small integers as Integer
        assertEquals(42, ((Number) parsed.get("count")).intValue());
    }
}
