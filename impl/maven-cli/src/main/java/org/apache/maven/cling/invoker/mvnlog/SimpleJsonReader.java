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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON parser that reads a JSON string into
 * {@code Map<String, Object>} / {@code List<Object>} / {@code String} / {@code Number} / {@code Boolean} / null.
 * <p>
 * No external dependencies. Companion to {@code BuildReportJsonWriter} which writes
 * JSON without any library; this reader follows the same zero-dependency principle.
 * <p>
 * This parser handles the full JSON spec (objects, arrays, strings with escapes,
 * numbers, booleans, null) and is sufficient for reading Maven build report files.
 */
final class SimpleJsonReader {

    private final String json;
    private int pos;

    private SimpleJsonReader(String json) {
        this.json = json;
        this.pos = 0;
    }

    /**
     * Parse a JSON string into a nested structure of Maps, Lists, and primitives.
     *
     * @param json the JSON string to parse
     * @return the parsed value (typically a {@code Map<String, Object>} for a JSON object)
     * @throws IllegalArgumentException if the JSON is malformed
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String json) {
        SimpleJsonReader reader = new SimpleJsonReader(json.strip());
        Object result = reader.parseValue();
        if (!(result instanceof Map)) {
            throw new IllegalArgumentException("Expected JSON object at root");
        }
        return (Map<String, Object>) result;
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length()) {
            throw error("Unexpected end of input");
        }
        char c = json.charAt(pos);
        if (c == '{') {
            return parseObject();
        }
        if (c == '[') {
            return parseArray();
        }
        if (c == '"') {
            return parseString();
        }
        if (c == 't' || c == 'f') {
            return parseBoolean();
        }
        if (c == 'n') {
            return parseNull();
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return parseNumber();
        }
        throw error("Unexpected character: " + c);
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (pos < json.length() && json.charAt(pos) == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            } else {
                break;
            }
        }
        skipWhitespace();
        expect('}');
        return map;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (pos < json.length() && json.charAt(pos) == ']') {
            pos++;
            return list;
        }
        while (true) {
            list.add(parseValue());
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            } else {
                break;
            }
        }
        skipWhitespace();
        expect(']');
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= json.length()) {
                    throw error("Unexpected end of string escape");
                }
                char escaped = json.charAt(pos++);
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'u':
                        if (pos + 4 > json.length()) {
                            throw error("Incomplete unicode escape");
                        }
                        String hex = json.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }
        throw error("Unterminated string");
    }

    private Number parseNumber() {
        int start = pos;
        if (pos < json.length() && json.charAt(pos) == '-') {
            pos++;
        }
        while (pos < json.length() && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
            pos++;
        }
        boolean isFloat = false;
        if (pos < json.length() && json.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            while (pos < json.length() && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
                pos++;
            }
        }
        if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < json.length() && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
                pos++;
            }
        }
        String numStr = json.substring(start, pos);
        if (isFloat) {
            return Double.parseDouble(numStr);
        }
        long value = Long.parseLong(numStr);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }
        return value;
    }

    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (json.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw error("Expected boolean");
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw error("Expected null");
    }

    private void skipWhitespace() {
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private void expect(char expected) {
        if (pos >= json.length() || json.charAt(pos) != expected) {
            throw error("Expected '" + expected + "'");
        }
        pos++;
    }

    private IllegalArgumentException error(String message) {
        int contextStart = Math.max(0, pos - 20);
        int contextEnd = Math.min(json.length(), pos + 20);
        String context = json.substring(contextStart, contextEnd);
        return new IllegalArgumentException(message + " at position " + pos + " near: ..." + context + "...");
    }
}
