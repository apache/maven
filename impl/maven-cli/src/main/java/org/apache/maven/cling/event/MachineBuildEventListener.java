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
package org.apache.maven.cling.event;

import java.util.function.Consumer;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.logging.BuildEventListener;
import org.eclipse.aether.transfer.TransferEvent;

/**
 * A machine-readable build event listener that outputs one JSON object per line
 * to the configured writer. Each line is a self-contained JSON object with an
 * {@code "event"} field identifying its type.
 * <p>
 * This listener handles the {@link BuildEventListener} events: log messages,
 * transfer progress, and execution failures. Session and project lifecycle events
 * are emitted by the companion {@link MachineExecutionEventLogger}.
 * <p>
 * The JSON lines format is designed for piping to external tools (CI systems,
 * LLM agents, IDE integrations) that consume structured build events in real time.
 * <p>
 * Example output:
 * <pre>
 * {"event":"log","timestamp":"...","message":"Compiling 42 source files"}
 * {"event":"transfer.started","timestamp":"...","artifact":"core-4.1.0.jar","size":524288}
 * {"event":"transfer.progressed","timestamp":"...","artifact":"core-4.1.0.jar","transferred":262144,"total":524288}
 * {"event":"transfer.completed","timestamp":"...","artifact":"core-4.1.0.jar","transferred":524288}
 * </pre>
 *
 * Selected via {@code --console=machine}.
 *
 * @since 4.1.0
 * @see MachineExecutionEventLogger
 */
public class MachineBuildEventListener implements BuildEventListener {

    private final Consumer<String> output;

    /**
     * Creates a new MachineBuildEventListener.
     *
     * @param output the consumer that receives each JSON line (typically writes to terminal/stdout)
     */
    public MachineBuildEventListener(Consumer<String> output) {
        this.output = output;
    }

    /**
     * Emit a pre-built JSON line to the output. Thread-safe — output is serialized
     * to prevent interleaved lines from parallel builds.
     *
     * @param json the complete JSON object string (no trailing newline)
     */
    public synchronized void emitEvent(String json) {
        output.accept(json);
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        // Handled by MachineExecutionEventLogger.sessionStarted()
    }

    @Override
    public void projectStarted(String projectId) {
        // Handled by MachineExecutionEventLogger.projectStarted()
    }

    @Override
    public void projectLogMessage(String projectId, LogEvent event) {
        emitEvent(new JsonLine("log")
                .field("level", event.level().name())
                .field("module", projectId)
                .field("mojo", event.mojoId())
                .field("logger", event.loggerName())
                .field("message", event.message())
                .build());
    }

    @Override
    public void projectFinished(String projectId) {
        // Handled by MachineExecutionEventLogger.projectSucceeded/Failed/Skipped()
    }

    @Override
    public void executionFailure(String projectId, boolean halted, String exception) {
        emitEvent(new JsonLine("execution.failure")
                .field("module", projectId)
                .field("halted", halted)
                .field("error", exception)
                .build());
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        // Handled by MachineExecutionEventLogger.mojoStarted()
    }

    @Override
    public void finish(int exitCode) throws Exception {
        // No-op — build.finished is emitted by MachineExecutionEventLogger.sessionEnded()
    }

    @Override
    public void fail(Throwable t) throws Exception {
        // No-op — build.finished is emitted by MachineExecutionEventLogger.sessionEnded()
    }

    @Override
    public void log(String msg) {
        emitEvent(new JsonLine("log").field("message", msg).build());
    }

    @Override
    public void transfer(String projectId, TransferEvent event) {
        String resource = event.getResource().getResourceName();
        String artifactName = extractArtifactName(resource);
        long contentLength = event.getResource().getContentLength();

        switch (event.getType()) {
            case INITIATED:
            case STARTED:
                JsonLine started = new JsonLine("transfer.started").field("artifact", artifactName);
                if (projectId != null) {
                    started.field("module", projectId);
                }
                if (contentLength > 0) {
                    started.field("size", contentLength);
                }
                started.field("url", resource);
                emitEvent(started.build());
                break;
            case PROGRESSED:
                JsonLine progressed = new JsonLine("transfer.progressed").field("artifact", artifactName);
                progressed.field("transferred", event.getTransferredBytes());
                if (contentLength > 0) {
                    progressed.field("total", contentLength);
                }
                emitEvent(progressed.build());
                break;
            case SUCCEEDED:
                JsonLine succeeded = new JsonLine("transfer.completed").field("artifact", artifactName);
                succeeded.field("transferred", event.getTransferredBytes());
                emitEvent(succeeded.build());
                break;
            case FAILED:
                JsonLine failed = new JsonLine("transfer.failed").field("artifact", artifactName);
                if (event.getException() != null) {
                    failed.field("error", event.getException().getMessage());
                }
                emitEvent(failed.build());
                break;
            default:
                break;
        }
    }

    // ---- Helpers ----

    private static String extractArtifactName(String resourceName) {
        if (resourceName == null) {
            return "unknown";
        }
        int lastSlash = resourceName.lastIndexOf('/');
        return lastSlash >= 0 ? resourceName.substring(lastSlash + 1) : resourceName;
    }

    // ---- JSON line builder ----

    /**
     * Lightweight builder for single-line JSON objects. Builds a flat JSON object
     * with an {@code "event"} type and a {@code "timestamp"} field, plus any
     * additional fields. Thread-safe when used within a single thread per instance.
     */
    static class JsonLine {
        private final StringBuilder sb;
        private boolean hasFields;

        JsonLine(String eventType) {
            sb = new StringBuilder(256);
            sb.append("{\"event\":\"");
            sb.append(eventType);
            sb.append("\",\"timestamp\":\"");
            sb.append(MonotonicClock.now().toString());
            sb.append('"');
            hasFields = true;
        }

        JsonLine field(String key, String value) {
            if (value != null) {
                sb.append(",\"").append(key).append("\":");
                writeJsonString(sb, value);
            }
            return this;
        }

        JsonLine field(String key, long value) {
            sb.append(",\"").append(key).append("\":").append(value);
            return this;
        }

        JsonLine field(String key, double value) {
            sb.append(",\"").append(key).append("\":").append(value);
            return this;
        }

        JsonLine field(String key, boolean value) {
            sb.append(",\"").append(key).append("\":").append(value);
            return this;
        }

        String build() {
            sb.append('}');
            return sb.toString();
        }

        /**
         * Write a JSON-escaped string value (with surrounding quotes) to the builder.
         */
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
    }
}
