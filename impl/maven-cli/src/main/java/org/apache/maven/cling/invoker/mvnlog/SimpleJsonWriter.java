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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON writer for serializing {@code Map<String, Object>} structures
 * back to well-formatted JSON. Used to output filtered build reports when
 * {@code --json} is combined with filters.
 * <p>
 * Supports: {@code Map}, {@code List}, {@code String}, {@code Number},
 * {@code Boolean}, and {@code null}.
 *
 * @since 4.1.0
 */
final class SimpleJsonWriter {

    private static final String INDENT = "  ";

    private SimpleJsonWriter() {}

    /**
     * Serialize a value to a pretty-printed JSON string.
     */
    static String toJson(Object value) {
        StringBuilder sb = new StringBuilder(4096);
        writeValue(sb, value, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value, int depth) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            writeObject(sb, (Map<String, Object>) value, depth);
        } else if (value instanceof List) {
            writeArray(sb, (List<Object>) value, depth);
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<String, Object> map, int depth) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }

        sb.append("{\n");
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            indent(sb, depth + 1);
            writeString(sb, entry.getKey());
            sb.append(": ");
            writeValue(sb, entry.getValue(), depth + 1);
            if (it.hasNext()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        indent(sb, depth);
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<Object> list, int depth) {
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }

        sb.append("[\n");
        Iterator<Object> it = list.iterator();
        while (it.hasNext()) {
            indent(sb, depth + 1);
            writeValue(sb, it.next(), depth + 1);
            if (it.hasNext()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        indent(sb, depth);
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void indent(StringBuilder sb, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append(INDENT);
        }
    }
}
