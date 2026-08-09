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
package org.apache.maven.internal.build;

import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.build.report.MojoReport;
import org.apache.maven.api.services.BuilderProblem;

/**
 * Serializes a {@link BuildReport} to JSON without any external library dependency.
 * <p>
 * The output is human-readable (indented with 2 spaces) and designed to be
 * stable across Maven versions — field order is fixed, and new fields are
 * appended at the end of each object.
 */
final class BuildReportJsonWriter {

    private BuildReportJsonWriter() {}

    /**
     * Serialize the given report to a pretty-printed JSON string.
     */
    static String toJson(BuildReport report) {
        StringBuilder sb = new StringBuilder(4096);
        writeReport(sb, report, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void writeReport(StringBuilder sb, BuildReport report, int indent) {
        sb.append("{\n");
        writeField(sb, indent + 1, "formatVersion", report.formatVersion());
        writeField(sb, indent + 1, "status", report.status().name());
        writeField(sb, indent + 1, "duration", report.duration().toString());
        writeField(sb, indent + 1, "startTime", report.startTime().toString());
        writeField(sb, indent + 1, "mavenVersion", report.mavenVersion());
        writeField(sb, indent + 1, "javaVersion", report.javaVersion());
        writeStringArray(sb, indent + 1, "goals", report.goals());
        writeField(sb, indent + 1, "project", report.project());
        writeField(sb, indent + 1, "multiModule", report.multiModule());
        writeField(sb, indent + 1, "threads", report.threads());

        // modules array
        writeIndent(sb, indent + 1);
        sb.append("\"modules\": ");
        if (report.modules().isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[\n");
            for (int i = 0; i < report.modules().size(); i++) {
                writeIndent(sb, indent + 2);
                writeModule(sb, report.modules().get(i), indent + 2);
                if (i < report.modules().size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            writeIndent(sb, indent + 1);
            sb.append(']');
        }
        sb.append(",\n");

        // failures array
        writeIndent(sb, indent + 1);
        sb.append("\"failures\": ");
        if (report.failures().isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[\n");
            for (int i = 0; i < report.failures().size(); i++) {
                writeIndent(sb, indent + 2);
                writeFailure(sb, report.failures().get(i), indent + 2);
                if (i < report.failures().size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            writeIndent(sb, indent + 1);
            sb.append(']');
        }
        sb.append(",\n");

        // problems array
        writeIndent(sb, indent + 1);
        sb.append("\"problems\": ");
        if (report.problems().isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[\n");
            for (int i = 0; i < report.problems().size(); i++) {
                writeIndent(sb, indent + 2);
                writeProblem(sb, report.problems().get(i), indent + 2);
                if (i < report.problems().size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            writeIndent(sb, indent + 1);
            sb.append(']');
        }
        sb.append(",\n");

        // output array — build-level log lines (outside any module)
        writeOutputArray(sb, indent + 1, report.output());
        sb.append('\n');

        writeIndent(sb, indent);
        sb.append('}');
    }

    private static void writeProblem(StringBuilder sb, BuilderProblem problem, int indent) {
        sb.append("{\n");
        writeField(sb, indent + 1, "severity", problem.getSeverity().name());
        writeField(sb, indent + 1, "message", problem.getMessage());
        String source = problem.getSource();
        if (source != null && !source.isEmpty()) {
            writeField(sb, indent + 1, "source", source);
        }
        if (problem.getLineNumber() > 0) {
            writeField(sb, indent + 1, "line", problem.getLineNumber());
        }
        if (problem.getColumnNumber() > 0) {
            writeField(sb, indent + 1, "column", problem.getColumnNumber());
        }
        // Remove the trailing comma from the last written field
        int lastComma = sb.lastIndexOf(",\n");
        if (lastComma > 0) {
            sb.replace(lastComma, lastComma + 1, "");
        }
        writeIndent(sb, indent);
        sb.append('}');
    }

    private static void writeModule(StringBuilder sb, ModuleReport module, int indent) {
        sb.append("{\n");
        writeField(sb, indent + 1, "groupId", module.groupId());
        writeField(sb, indent + 1, "artifactId", module.artifactId());
        writeField(sb, indent + 1, "version", module.version());
        writeField(sb, indent + 1, "status", module.status().name());
        writeField(sb, indent + 1, "startTime", module.startTime().toString());
        writeField(sb, indent + 1, "duration", module.duration().toString());

        // mojos array
        writeIndent(sb, indent + 1);
        sb.append("\"mojos\": ");
        if (module.mojos().isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[\n");
            for (int i = 0; i < module.mojos().size(); i++) {
                writeIndent(sb, indent + 2);
                writeMojo(sb, module.mojos().get(i), indent + 2);
                if (i < module.mojos().size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            writeIndent(sb, indent + 1);
            sb.append(']');
        }
        sb.append(",\n");

        // output array — module-level log lines (between mojos)
        writeOutputArray(sb, indent + 1, module.output());
        sb.append('\n');

        writeIndent(sb, indent);
        sb.append('}');
    }

    private static void writeMojo(StringBuilder sb, MojoReport mojo, int indent) {
        sb.append("{\n");
        writeField(sb, indent + 1, "groupId", mojo.groupId());
        writeField(sb, indent + 1, "artifactId", mojo.artifactId());
        writeField(sb, indent + 1, "version", mojo.version());
        writeField(sb, indent + 1, "goal", mojo.goal());
        writeNullableField(sb, indent + 1, "executionId", mojo.executionId(), true);
        writeNullableField(sb, indent + 1, "phase", mojo.phase(), true);
        writeField(sb, indent + 1, "status", mojo.status().name());
        writeField(sb, indent + 1, "startTime", mojo.startTime().toString());
        writeField(sb, indent + 1, "duration", mojo.duration().toString());

        // output array — captured log lines
        writeOutputArray(sb, indent + 1, mojo.output());
        sb.append('\n');

        writeIndent(sb, indent);
        sb.append('}');
    }

    private static void writeFailure(StringBuilder sb, FailureReport failure, int indent) {
        sb.append("{\n");
        writeField(sb, indent + 1, "module", failure.module());
        writeNullableField(sb, indent + 1, "mojo", failure.mojo(), true);
        writeField(sb, indent + 1, "timestamp", failure.timestamp().toString());
        writeNullableField(sb, indent + 1, "exceptionType", failure.exceptionType(), true);
        if (failure.stackTrace() != null) {
            writeField(sb, indent + 1, "message", failure.message());
            writeLastField(sb, indent + 1, "stackTrace", failure.stackTrace());
        } else {
            writeLastField(sb, indent + 1, "message", failure.message());
        }
        writeIndent(sb, indent);
        sb.append('}');
    }

    /**
     * Writes an {@code "output": [...]} array of structured log events
     * (used by report, module, and mojo).
     * This is always the last field in its object, so no trailing comma.
     */
    private static void writeOutputArray(StringBuilder sb, int indent, java.util.List<LogEvent> events) {
        writeIndent(sb, indent);
        sb.append("\"output\": ");
        if (events.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[\n");
            for (int i = 0; i < events.size(); i++) {
                writeIndent(sb, indent + 1);
                writeLogEvent(sb, events.get(i), indent + 1);
                if (i < events.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            writeIndent(sb, indent);
            sb.append(']');
        }
    }

    private static void writeLogEvent(StringBuilder sb, LogEvent event, int indent) {
        sb.append("{\n");
        writeField(sb, indent + 1, "timestamp", event.timestamp().toString());
        writeField(sb, indent + 1, "level", event.level().name());
        if (event.loggerName() != null) {
            writeField(sb, indent + 1, "loggerName", event.loggerName());
        }
        writeField(sb, indent + 1, "message", event.message());
        if (event.stackTrace() != null) {
            writeField(sb, indent + 1, "stackTrace", event.stackTrace());
        }
        // Source metadata — present for Log API and JUL events
        if (event.sourceClassName() != null) {
            writeField(sb, indent + 1, "sourceClassName", event.sourceClassName());
        }
        if (event.sourceMethodName() != null) {
            writeField(sb, indent + 1, "sourceMethodName", event.sourceMethodName());
        }
        if (event.threadId() >= 0) {
            writeField(sb, indent + 1, "threadId", event.threadId());
        }
        if (event.sequenceNumber() >= 0) {
            writeField(sb, indent + 1, "sequenceNumber", event.sequenceNumber());
        }
        removeTrailingComma(sb);
        writeIndent(sb, indent);
        sb.append('}');
    }

    // ---- Low-level JSON writing helpers ----

    private static void writeField(StringBuilder sb, int indent, String key, String value) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": ");
        writeJsonString(sb, value);
        sb.append(",\n");
    }

    private static void writeField(StringBuilder sb, int indent, String key, int value) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": ").append(value).append(",\n");
    }

    private static void writeField(StringBuilder sb, int indent, String key, long value) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": ").append(value).append(",\n");
    }

    private static void writeField(StringBuilder sb, int indent, String key, boolean value) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": ").append(value).append(",\n");
    }

    /**
     * Removes the trailing comma from the last field in a JSON object.
     * Turns {@code "field": value,\n} into {@code "field": value\n}.
     */
    private static void removeTrailingComma(StringBuilder sb) {
        int len = sb.length();
        if (len >= 2 && sb.charAt(len - 2) == ',' && sb.charAt(len - 1) == '\n') {
            sb.deleteCharAt(len - 2);
        }
    }

    private static void writeLastField(StringBuilder sb, int indent, String key, String value) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": ");
        writeJsonString(sb, value);
        sb.append('\n');
    }

    private static void writeNullableField(
            StringBuilder sb, int indent, String key, String value, @SuppressWarnings("unused") boolean hasMore) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": ");
        if (value != null) {
            writeJsonString(sb, value);
        } else {
            sb.append("null");
        }
        sb.append(",\n");
    }

    private static void writeStringArray(StringBuilder sb, int indent, String key, java.util.List<String> values) {
        writeIndent(sb, indent);
        sb.append('"').append(key).append("\": [");
        for (int i = 0; i < values.size(); i++) {
            writeJsonString(sb, values.get(i));
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("],\n");
    }

    private static void writeJsonString(StringBuilder sb, String value) {
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
                        sb.append("\\u");
                        sb.append(String.format("%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void writeIndent(StringBuilder sb, int level) {
        sb.append("  ".repeat(level));
    }
}
