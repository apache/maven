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
package org.apache.maven.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.execution.ExecutionEvent;
import org.eclipse.aether.transfer.TransferEvent;

public class BuildReportEventListener implements BuildEventListener {

    private final BuildEventListener delegate;
    private Path topDirectory = Paths.get("");
    private long startTime = System.currentTimeMillis();
    private long endTime;
    private List<String> goals = Collections.emptyList();
    private int degreeOfConcurrency = 1;
    private String status = "SUCCESS";

    private final Map<String, ModuleInfo> modules = new ConcurrentHashMap<>();
    private final List<ModuleInfo> orderedModules = new CopyOnWriteArrayList<>();
    private final Map<Long, MojoInfo> activeMojos = new ConcurrentHashMap<>();
    private final List<ProblemInfo> problems = new CopyOnWriteArrayList<>();
    private final List<FailureInfo> failures = new CopyOnWriteArrayList<>();
    private final List<LogEntry> logs = new CopyOnWriteArrayList<>();

    public BuildReportEventListener(BuildEventListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        delegate.sessionStarted(event);
        if (event.getSession() != null) {
            if (event.getSession().getTopDirectory() != null) {
                topDirectory = event.getSession().getTopDirectory();
            }
            if (event.getSession().getRequest() != null) {
                goals = event.getSession().getRequest().getGoals();
                degreeOfConcurrency = event.getSession().getRequest().getDegreeOfConcurrency();
            }
            if (event.getSession().getStartTime() != null) {
                startTime = event.getSession().getStartTime().getTime();
            }
        }
    }

    @Override
    public void projectStarted(String projectId) {
        delegate.projectStarted(projectId);
        ModuleInfo info = new ModuleInfo(projectId);
        info.startTime = System.currentTimeMillis();
        modules.put(projectId, info);
        orderedModules.add(info);
    }

    @Override
    public void projectFinished(String projectId, String status) {
        delegate.projectFinished(projectId, status);
        ModuleInfo info = modules.get(projectId);
        if (info != null) {
            info.endTime = System.currentTimeMillis();
            info.status = status;
        }
    }

    @Override
    public void executionFailure(String projectId, boolean halted, String exception) {
        delegate.executionFailure(projectId, halted, exception);
        status = "FAILURE";
        MojoInfo activeMojo = activeMojos.get(Thread.currentThread().getId());
        String mojoId = activeMojo != null ? activeMojo.getId() : null;
        failures.add(new FailureInfo(projectId, mojoId, exception != null ? exception : "Unknown Cause"));
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        delegate.mojoStarted(event);
        if (event.getMojoExecution() != null) {
            MojoInfo info = new MojoInfo(
                event.getMojoExecution().getGroupId(),
                event.getMojoExecution().getArtifactId(),
                event.getMojoExecution().getVersion(),
                event.getMojoExecution().getGoal(),
                event.getMojoExecution().getExecutionId(),
                event.getMojoExecution().getLifecyclePhase()
            );
            info.startTime = System.currentTimeMillis();
            activeMojos.put(Thread.currentThread().getId(), info);

            // Add MojoInfo to the corresponding ModuleInfo
            if (event.getProject() != null) {
                ModuleInfo moduleInfo = modules.get(event.getProject().getArtifactId());
                if (moduleInfo != null) {
                    moduleInfo.mojos.add(info);
                }
            }
        }
    }

    @Override
    public void mojoFinished(ExecutionEvent event, String status) {
        long threadId = Thread.currentThread().getId();
        MojoInfo info = activeMojos.remove(threadId);
        if (info != null) {
            info.endTime = System.currentTimeMillis();
            info.status = status;
        }
    }

    @Override
    public void projectLogMessage(String projectId, String event) {
        delegate.projectLogMessage(projectId, event);
        if (event == null) return;

        long timestamp = System.currentTimeMillis();
        String cleanMessage = event.replaceAll("\\u001B\\[[;\\d]*[ -/]*[@-~]", "");

        // Determine log level
        String level = "INFO";
        if (cleanMessage.contains("[TRACE]")) {
            level = "TRACE";
        } else if (cleanMessage.contains("[DEBUG]")) {
            level = "DEBUG";
        } else if (cleanMessage.contains("[WARNING]") || cleanMessage.contains("[WARN]")) {
            level = "WARN";
        } else if (cleanMessage.contains("[ERROR]")) {
            level = "ERROR";
        }

        // Get currently active mojo on this thread
        MojoInfo activeMojo = activeMojos.get(Thread.currentThread().getId());
        String mojoId = activeMojo != null ? activeMojo.getId() : null;

        LogEntry logEntry = new LogEntry(timestamp, level, projectId, mojoId, cleanMessage);
        logs.add(logEntry);

        // Record warnings and errors as problems
        if ("WARN".equals(level) || "ERROR".equals(level)) {
            addProblemFromLog(level, projectId, mojoId, cleanMessage);
        }
    }

    private void addProblemFromLog(String level, String projectId, String mojoId, String message) {
        String docUrl = "";
        int httpIdx = message.indexOf("http://");
        if (httpIdx == -1) {
            httpIdx = message.indexOf("https://");
        }
        if (httpIdx != -1) {
            int endIdx = httpIdx;
            while (endIdx < message.length() && !Character.isWhitespace(message.charAt(endIdx)) 
                   && message.charAt(endIdx) != ')' && message.charAt(endIdx) != ']') {
                endIdx++;
            }
            docUrl = message.substring(httpIdx, endIdx);
        }

        String key = "GENERAL";
        int mngIdx = message.indexOf("MNG-");
        if (mngIdx != -1) {
            int endIdx = mngIdx;
            while (endIdx < message.length() && (Character.isLetterOrDigit(message.charAt(endIdx)) || message.charAt(endIdx) == '-')) {
                endIdx++;
            }
            key = message.substring(mngIdx, endIdx);
        } else if (mojoId != null) {
            key = mojoId;
        }

        String source = projectId != null ? projectId : "core";
        String suggestion = "Review log details for resolution.";
        if (message.toLowerCase().contains("use ") || message.toLowerCase().contains("should be")) {
            suggestion = "Follow the recommended configuration change in the message.";
        }

        problems.add(new ProblemInfo(level, message, key, source, suggestion, docUrl));
    }

    @Override
    public void finish(int exitCode) throws Exception {
        endTime = System.currentTimeMillis();
        if (exitCode != 0) {
            status = "FAILURE";
        }
        writeReport();
        delegate.finish(exitCode);
    }

    @Override
    public void fail(Throwable t) throws Exception {
        endTime = System.currentTimeMillis();
        status = "FAILURE";
        failures.add(new FailureInfo("session", null, t != null ? t.toString() : "Build Exception"));
        writeReport();
        delegate.fail(t);
    }

    @Override
    public void log(String msg) {
        delegate.log(msg);
    }

    @Override
    public void transfer(String projectId, TransferEvent e) {
        delegate.transfer(projectId, e);
    }

    private void writeReport() {
        try {
            Path reportsDir = topDirectory.resolve(".mvn").resolve("reports");
            Files.createDirectories(reportsDir);

            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            Path reportFile = reportsDir.resolve("build-report-" + timestamp + ".json");

            try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
                writer.write("{\n");
                writer.write("  \"metadata\": {\n");
                writer.write("    \"status\": \"" + status + "\",\n");
                writer.write("    \"mavenVersion\": \"" + escapeJson(System.getProperty("maven.version", "4.1.0-SNAPSHOT")) + "\",\n");
                writer.write("    \"javaVersion\": \"" + escapeJson(System.getProperty("java.version", "unknown")) + "\",\n");
                writer.write("    \"startTime\": " + startTime + ",\n");
                writer.write("    \"endTime\": " + endTime + ",\n");
                writer.write("    \"durationMs\": " + (endTime - startTime) + ",\n");
                writer.write("    \"goals\": [" + formatStringList(goals) + "],\n");
                writer.write("    \"threadCount\": " + degreeOfConcurrency + "\n");
                writer.write("  },\n");

                writer.write("  \"modules\": [\n");
                for (int i = 0; i < orderedModules.size(); i++) {
                    ModuleInfo mod = orderedModules.get(i);
                    writer.write("    {\n");
                    writer.write("      \"id\": \"" + escapeJson(mod.id) + "\",\n");
                    writer.write("      \"status\": \"" + mod.status + "\",\n");
                    writer.write("      \"startTime\": " + mod.startTime + ",\n");
                    writer.write("      \"endTime\": " + mod.endTime + ",\n");
                    writer.write("      \"durationMs\": " + (mod.endTime - mod.startTime) + ",\n");
                    writer.write("      \"mojos\": [\n");
                    for (int j = 0; j < mod.mojos.size(); j++) {
                        MojoInfo mojo = mod.mojos.get(j);
                        writer.write("        {\n");
                        writer.write("          \"pluginGroupId\": \"" + escapeJson(mojo.groupId) + "\",\n");
                        writer.write("          \"pluginArtifactId\": \"" + escapeJson(mojo.artifactId) + "\",\n");
                        writer.write("          \"pluginVersion\": \"" + escapeJson(mojo.version) + "\",\n");
                        writer.write("          \"goal\": \"" + escapeJson(mojo.goal) + "\",\n");
                        writer.write("          \"executionId\": \"" + escapeJson(mojo.executionId) + "\",\n");
                        writer.write("          \"phase\": \"" + escapeJson(mojo.phase) + "\",\n");
                        writer.write("          \"status\": \"" + mojo.status + "\",\n");
                        writer.write("          \"startTime\": " + mojo.startTime + ",\n");
                        writer.write("          \"endTime\": " + mojo.endTime + ",\n");
                        writer.write("          \"durationMs\": " + (mojo.endTime - mojo.startTime) + "\n");
                        writer.write("        }" + (j < mod.mojos.size() - 1 ? "," : "") + "\n");
                    }
                    writer.write("      ]\n");
                    writer.write("    }" + (i < orderedModules.size() - 1 ? "," : "") + "\n");
                }
                writer.write("  ],\n");

                writer.write("  \"problems\": [\n");
                for (int i = 0; i < problems.size(); i++) {
                    ProblemInfo prob = problems.get(i);
                    writer.write("    {\n");
                    writer.write("      \"severity\": \"" + prob.severity + "\",\n");
                    writer.write("      \"message\": \"" + escapeJson(prob.message) + "\",\n");
                    writer.write("      \"key\": \"" + escapeJson(prob.key) + "\",\n");
                    writer.write("      \"source\": \"" + escapeJson(prob.source) + "\",\n");
                    writer.write("      \"suggestion\": \"" + escapeJson(prob.suggestion) + "\",\n");
                    writer.write("      \"documentationUrl\": \"" + escapeJson(prob.documentationUrl) + "\"\n");
                    writer.write("    }" + (i < problems.size() - 1 ? "," : "") + "\n");
                }
                writer.write("  ],\n");

                writer.write("  \"failures\": [\n");
                for (int i = 0; i < failures.size(); i++) {
                    FailureInfo fail = failures.get(i);
                    writer.write("    {\n");
                    writer.write("      \"moduleId\": \"" + escapeJson(fail.moduleId) + "\",\n");
                    writer.write("      \"mojo\": " + (fail.mojoId != null ? "\"" + escapeJson(fail.mojoId) + "\"" : "null") + ",\n");
                    writer.write("      \"message\": \"" + escapeJson(fail.message) + "\"\n");
                    writer.write("    }" + (i < failures.size() - 1 ? "," : "") + "\n");
                }
                writer.write("  ],\n");

                writer.write("  \"logs\": [\n");
                for (int i = 0; i < logs.size(); i++) {
                    LogEntry entry = logs.get(i);
                    writer.write("    {\n");
                    writer.write("      \"timestamp\": " + entry.timestamp + ",\n");
                    writer.write("      \"level\": \"" + entry.level + "\",\n");
                    writer.write("      \"moduleId\": " + (entry.moduleId != null ? "\"" + escapeJson(entry.moduleId) + "\"" : "null") + ",\n");
                    writer.write("      \"mojoId\": " + (entry.mojoId != null ? "\"" + escapeJson(entry.mojoId) + "\"" : "null") + ",\n");
                    writer.write("      \"message\": \"" + escapeJson(entry.message) + "\"\n");
                    writer.write("    }" + (i < logs.size() - 1 ? "," : "") + "\n");
                }
                writer.write("  ]\n");
                writer.write("}\n");
            }
        } catch (IOException e) {
            // Silently ignore report writing failures to not crash maven build
        }
    }

    private static String formatStringList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private static String escapeJson(String string) {
        if (string == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u" + t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    private static class ModuleInfo {
        final String id;
        String status = "SKIPPED";
        long startTime;
        long endTime;
        final List<MojoInfo> mojos = new CopyOnWriteArrayList<>();

        ModuleInfo(String id) {
            this.id = id;
        }
    }

    private static class MojoInfo {
        final String groupId;
        final String artifactId;
        final String version;
        final String goal;
        final String executionId;
        final String phase;
        String status = "SKIPPED";
        long startTime;
        long endTime;

        MojoInfo(String groupId, String artifactId, String version, String goal, String executionId, String phase) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.goal = goal;
            this.executionId = executionId;
            this.phase = phase;
        }

        String getId() {
            return groupId + ":" + artifactId + ":" + version + ":" + goal + ":" + executionId;
        }
    }

    private static class ProblemInfo {
        final String severity;
        final String message;
        final String key;
        final String source;
        final String suggestion;
        final String documentationUrl;

        ProblemInfo(String severity, String message, String key, String source, String suggestion, String documentationUrl) {
            this.severity = severity;
            this.message = message;
            this.key = key;
            this.source = source;
            this.suggestion = suggestion;
            this.documentationUrl = documentationUrl != null ? documentationUrl : "";
        }
    }

    private static class FailureInfo {
        final String moduleId;
        final String mojoId;
        final String message;

        FailureInfo(String moduleId, String mojoId, String message) {
            this.moduleId = moduleId;
            this.mojoId = mojoId;
            this.message = message;
        }
    }

    private static class LogEntry {
        final long timestamp;
        final String level;
        final String moduleId;
        final String mojoId;
        final String message;

        LogEntry(long timestamp, String level, String moduleId, String mojoId, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.moduleId = moduleId;
            this.mojoId = mojoId;
            this.message = message;
        }
    }
}
