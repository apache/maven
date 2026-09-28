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
package org.apache.maven.cling.invoker.mvnval;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.ModelProblem;

/**
 * Renders validation results as a JSON array, one object per validated file. Assembled by hand
 * so the tool needs no dependency the distribution does not already carry.
 */
class JsonReporter implements Reporter {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static final char FIRST_PRINTABLE_ASCII = 0x20;
    private static final char LAST_PRINTABLE_ASCII = 0x7e;

    /** The two quotes, plus room for a few escapes. */
    private static final int ESCAPE_HEADROOM = 16;

    @Override
    public void report(List<Report> reports, Consumer<String> out) {
        out.accept(reports.stream().map(JsonReporter::toJson).collect(Collectors.joining(",", "[", "]")));
    }

    private static String toJson(Report report) {
        StringBuilder sb =
                new StringBuilder("{\"pom\":").append(quote(report.pom().toString()));
        if (report.failure() != null) {
            sb.append(",\"failure\":").append(quote(report.failure()));
        }
        return sb.append(",\"problems\":")
                .append(report.problems().stream()
                        .map(problem -> toJson(problem, report))
                        .collect(Collectors.joining(",", "[", "]")))
                .append('}')
                .toString();
    }

    private static String toJson(ModelProblem problem, Report report) {
        StringBuilder sb = new StringBuilder("{\"severity\":")
                .append(quote(problem.getSeverity().name()))
                .append(",\"message\":")
                .append(quote(problem.getMessage()));
        // Absent values are left out, not written as the -1 the model builder uses for "no location".
        String source = report.sourceOf(problem);
        if (source != null) {
            sb.append(",\"source\":").append(quote(source));
        }
        if (problem.getLineNumber() > 0) {
            sb.append(",\"line\":").append(problem.getLineNumber());
        }
        if (problem.getColumnNumber() > 0) {
            sb.append(",\"column\":").append(problem.getColumnNumber());
        }
        return sb.append('}').toString();
    }

    private static String quote(@Nullable String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String escaped = escaped(c);
            if (escaped == null) {
                if (sb != null) {
                    sb.append(c);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(value.length() + ESCAPE_HEADROOM)
                            .append('"')
                            .append(value, 0, i);
                }
                sb.append(escaped);
            }
        }
        return sb == null ? '"' + value + '"' : sb.append('"').toString();
    }

    /**
     * The replacement for a character that cannot stand for itself, or {@code null} when it can.
     * <p>
     * Everything outside printable ASCII is escaped, not just the control characters JSON forbids:
     * the writer's charset is the console's, which on Windows is routinely not UTF-8, and a path
     * can carry an unpaired surrogate that no charset can encode.
     */
    @Nullable
    private static String escaped(char c) {
        return switch (c) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default ->
                c < FIRST_PRINTABLE_ASCII || c > LAST_PRINTABLE_ASCII
                        ? new String(new char[] {
                            '\\', 'u', HEX[c >> 12 & 0xf], HEX[c >> 8 & 0xf], HEX[c >> 4 & 0xf], HEX[c & 0xf]
                        })
                        : null;
        };
    }
}
