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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleJsonReaderTest {

    @Test
    void testParseEmptyObject() {
        Map<String, Object> result = SimpleJsonReader.parse("{}");
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseSimpleObject() {
        Map<String, Object> result = SimpleJsonReader.parse("""
                {"name": "test", "version": "1.0"}""");
        assertEquals("test", result.get("name"));
        assertEquals("1.0", result.get("version"));
    }

    @Test
    void testParseNumbers() {
        Map<String, Object> result = SimpleJsonReader.parse("""
                {"count": 42, "ratio": 3.14, "negative": -7}""");
        assertEquals(42, result.get("count"));
        assertEquals(3.14, result.get("ratio"));
        assertEquals(-7, result.get("negative"));
    }

    @Test
    void testParseBooleanAndNull() {
        Map<String, Object> result = SimpleJsonReader.parse("""
                {"active": true, "deleted": false, "extra": null}""");
        assertEquals(true, result.get("active"));
        assertEquals(false, result.get("deleted"));
        assertNull(result.get("extra"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParseArray() {
        Map<String, Object> result = SimpleJsonReader.parse("""
                {"goals": ["clean", "install"]}""");
        List<Object> goals = (List<Object>) result.get("goals");
        assertEquals(2, goals.size());
        assertEquals("clean", goals.get(0));
        assertEquals("install", goals.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParseNestedObject() {
        Map<String, Object> result = SimpleJsonReader.parse("""
                {"module": {"artifactId": "core", "status": "SUCCESS"}}""");
        Map<String, Object> module = (Map<String, Object>) result.get("module");
        assertEquals("core", module.get("artifactId"));
        assertEquals("SUCCESS", module.get("status"));
    }

    @Test
    void testParseStringEscapes() {
        Map<String, Object> result = SimpleJsonReader.parse("""
                {"msg": "line1\\nline2", "path": "C:\\\\Users"}""");
        assertEquals("line1\nline2", result.get("msg"));
        assertEquals("C:\\Users", result.get("path"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testParseBuildReportFragment() {
        String json = """
                {
                  "formatVersion": "1.0",
                  "status": "SUCCESS",
                  "duration": "PT6.7S",
                  "mavenVersion": "4.1.0-SNAPSHOT",
                  "modules": [
                    {
                      "artifactId": "maven-api-core",
                      "status": "SUCCESS",
                      "duration": "PT2.1S",
                      "mojos": []
                    },
                    {
                      "artifactId": "maven-core",
                      "status": "SUCCESS",
                      "duration": "PT3.4S",
                      "mojos": []
                    }
                  ],
                  "problems": [],
                  "failures": []
                }""";

        Map<String, Object> report = SimpleJsonReader.parse(json);
        assertEquals("1.0", report.get("formatVersion"));
        assertEquals("SUCCESS", report.get("status"));
        assertEquals("PT6.7S", report.get("duration"));

        List<Map<String, Object>> modules = (List<Map<String, Object>>) (List<?>) report.get("modules");
        assertEquals(2, modules.size());
        assertEquals("maven-api-core", modules.get(0).get("artifactId"));
        assertEquals("maven-core", modules.get(1).get("artifactId"));
    }

    @Test
    void testParseInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> SimpleJsonReader.parse("not json"));
    }

    @Test
    void testParseNonObjectRoot() {
        assertThrows(IllegalArgumentException.class, () -> SimpleJsonReader.parse("[1, 2, 3]"));
    }
}
