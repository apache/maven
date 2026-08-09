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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;

/**
 * Renders a build report (parsed from JSON into a {@code Map<String, Object>})
 * as formatted terminal output.
 * <p>
 * Shared between the standalone {@code mvnlog} command and the {@code mvnsh} subcommand.
 *
 * @since 4.1.0
 */
public class BuildReportRenderer {

    private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;

    private final MessageBuilderFactory messageBuilderFactory;
    private final Consumer<String> output;

    public BuildReportRenderer(MessageBuilderFactory messageBuilderFactory, Consumer<String> output) {
        this.messageBuilderFactory = messageBuilderFactory;
        this.output = output;
    }

    /**
     * Render the default summary view of a build report.
     */
    @SuppressWarnings("unchecked")
    public void renderSummary(Map<String, Object> report) {
        renderHeader(report);

        // Module summary
        List<Map<String, Object>> modules = getList(report, "modules");
        if (!modules.isEmpty()) {
            for (Map<String, Object> module : modules) {
                renderModuleLine(module);
            }
            output.accept("");

            // Stats line
            int passed = 0, failed = 0, skipped = 0;
            for (Map<String, Object> module : modules) {
                String status = getString(module, "status");
                switch (status) {
                    case "SUCCESS":
                        passed++;
                        break;
                    case "FAILURE":
                        failed++;
                        break;
                    default:
                        skipped++;
                        break;
                }
            }
            StringBuilder stats = new StringBuilder();
            stats.append(modules.size()).append(" modules");
            stats.append(" | ").append(passed).append(" passed");
            if (failed > 0) {
                stats.append(" | ").append(failed).append(" failed");
            }
            if (skipped > 0) {
                stats.append(" | ").append(skipped).append(" skipped");
            }
            output.accept(stats.toString());
        }

        // Problems — show structured warnings/errors so the user doesn't have to re-run the build
        List<Map<String, Object>> problems = getList(report, "problems");
        if (!problems.isEmpty()) {
            long warnings = problems.stream()
                    .filter(d -> "WARNING".equals(getString(d, "severity")))
                    .count();
            long errors = problems.stream()
                    .filter(d -> "ERROR".equals(getString(d, "severity")))
                    .count();
            if (warnings > 0 || errors > 0) {
                output.accept("");
                MessageBuilder header = messageBuilderFactory.builder();
                header.a("Problems: ");
                if (errors > 0) {
                    header.failure(errors + " error" + (errors > 1 ? "s" : ""));
                }
                if (errors > 0 && warnings > 0) {
                    header.a(", ");
                }
                if (warnings > 0) {
                    header.warning(warnings + " warning" + (warnings > 1 ? "s" : ""));
                }
                output.accept(header.toString());

                // Show each problem with structured details
                for (Map<String, Object> p : problems) {
                    renderProblemCompact(p);
                }
            }
        }

        // Failures count
        List<Map<String, Object>> failures = getList(report, "failures");
        if (!failures.isEmpty()) {
            output.accept(messageBuilderFactory
                    .builder()
                    .failure(failures.size() + " failure" + (failures.size() > 1 ? "s" : ""))
                    .toString());
        }

        output.accept("");
        String duration = getString(report, "duration");
        output.accept("Total time:  " + (duration != null ? formatDuration(duration) : "?"));
    }

    /**
     * Render the detailed diagnostics view.
     */
    @SuppressWarnings("unchecked")
    public void renderDiagnostics(Map<String, Object> report) {
        renderHeader(report);

        List<Map<String, Object>> problems = getList(report, "problems");
        if (problems.isEmpty()) {
            output.accept(messageBuilderFactory
                    .builder()
                    .success("No problems recorded.")
                    .toString());
            return;
        }

        // Count by severity
        long errors = problems.stream()
                .filter(p -> "ERROR".equals(getString(p, "severity")))
                .count();
        long warnings = problems.stream()
                .filter(p -> "WARNING".equals(getString(p, "severity")))
                .count();
        long infos = problems.size() - errors - warnings;

        MessageBuilder header = messageBuilderFactory.builder();
        header.strong("Problems (" + problems.size() + ")");
        header.a(": ");
        List<String> parts = new ArrayList<>();
        if (errors > 0) {
            parts.add(errors + " error" + (errors > 1 ? "s" : ""));
        }
        if (warnings > 0) {
            parts.add(warnings + " warning" + (warnings > 1 ? "s" : ""));
        }
        if (infos > 0) {
            parts.add(infos + " info");
        }
        header.a(String.join(", ", parts));
        output.accept(header.toString());
        output.accept("");

        for (Map<String, Object> problem : problems) {
            renderProblemDetailed(problem);
        }
    }

    /**
     * Render the detailed failures view.
     */
    @SuppressWarnings("unchecked")
    public void renderFailures(Map<String, Object> report) {
        renderHeader(report);

        List<Map<String, Object>> failures = getList(report, "failures");
        if (failures.isEmpty()) {
            output.accept(messageBuilderFactory
                    .builder()
                    .success("No failures recorded.")
                    .toString());
            return;
        }

        output.accept("Failures (" + failures.size() + "):");
        for (Map<String, Object> failure : failures) {
            output.accept("");
            String module = getString(failure, "module");
            String mojo = getString(failure, "mojo");
            MessageBuilder mb = messageBuilderFactory.builder();
            mb.failure("  [FAIL] ").a(module);
            if (mojo != null && !mojo.isEmpty()) {
                mb.a(" - ").a(mojo);
            }
            output.accept(mb.toString());

            String message = getString(failure, "message");
            if (message != null) {
                output.accept("    " + message);
            }

            String stackTrace = getString(failure, "stackTrace");
            if (stackTrace != null && !stackTrace.isEmpty()) {
                // Show first few lines of stack trace
                String[] lines = stackTrace.split("\n");
                int limit = Math.min(lines.length, 10);
                for (int i = 0; i < limit; i++) {
                    output.accept("    " + lines[i]);
                }
                if (lines.length > limit) {
                    output.accept("    ... " + (lines.length - limit) + " more lines");
                }
            }
        }
    }

    /**
     * Render the full per-mojo timing breakdown.
     */
    @SuppressWarnings("unchecked")
    public void renderFull(Map<String, Object> report) {
        renderHeader(report);

        List<Map<String, Object>> modules = getList(report, "modules");
        if (modules.isEmpty()) {
            output.accept("No modules recorded.");
            return;
        }

        for (Map<String, Object> module : modules) {
            String artifactId = getString(module, "artifactId");
            String status = getString(module, "status");
            String duration = getString(module, "duration");

            MessageBuilder mb = messageBuilderFactory.builder();
            mb.strong("Module: " + artifactId);
            mb.a(" (").a(duration != null ? formatDuration(duration) : "?").a(") ");
            if ("SUCCESS".equals(status)) {
                mb.success(status);
            } else if ("FAILURE".equals(status)) {
                mb.failure(status);
            } else {
                mb.warning(status);
            }
            output.accept(mb.toString());

            List<Map<String, Object>> mojos = getList(module, "mojos");
            for (Map<String, Object> mojo : mojos) {
                String goal = getString(mojo, "goal");
                String mojoArtifactId = getString(mojo, "artifactId");
                String mojoDuration = getString(mojo, "duration");
                String mojoStatus = getString(mojo, "status");
                String executionId = getString(mojo, "executionId");

                StringBuilder line = new StringBuilder("  ");
                String prefix = mojoArtifactId != null
                        ? mojoArtifactId.replace("maven-", "").replace("-plugin", "")
                        : "";
                line.append(prefix);
                if (goal != null) {
                    line.append(":").append(goal);
                }
                if (executionId != null && !executionId.isEmpty()) {
                    line.append(" (").append(executionId).append(")");
                }

                // Pad with dots
                int padTo = 50;
                while (line.length() < padTo) {
                    line.append('.');
                }
                line.append(' ');

                MessageBuilder mojoMb = messageBuilderFactory.builder();
                mojoMb.a(line);
                mojoMb.a(mojoDuration != null ? formatDuration(mojoDuration) : "?");
                if ("FAILURE".equals(mojoStatus)) {
                    mojoMb.a(" ").failure("FAILED");
                }
                output.accept(mojoMb.toString());
            }
            output.accept("");
        }
    }

    /**
     * List all available build report files in the given directory.
     */
    public void listReports(Path buildReportsDir) throws IOException {
        if (!Files.isDirectory(buildReportsDir)) {
            output.accept("No build reports directory found at: " + buildReportsDir);
            return;
        }

        List<Path> reports = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildReportsDir, "build-report-*.json")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)
                        && !entry.getFileName().toString().equals("build-report-latest.json")) {
                    reports.add(entry);
                }
            }
        }

        if (reports.isEmpty()) {
            output.accept("No build reports found in: " + buildReportsDir);
            return;
        }

        reports.sort(Comparator.comparing(Path::getFileName).reversed());

        output.accept(messageBuilderFactory
                .builder()
                .strong("Available build reports:")
                .toString());
        output.accept("");

        Path latestLink = buildReportsDir.resolve("build-report-latest.json");
        Path latestTarget = null;
        if (Files.isSymbolicLink(latestLink)) {
            try {
                latestTarget = Files.readSymbolicLink(latestLink).getFileName();
            } catch (IOException e) {
                // ignore
            }
        }

        for (Path report : reports) {
            String name = report.getFileName().toString();
            StringBuilder line = new StringBuilder("  ");
            line.append(name);
            if (latestTarget != null && name.equals(latestTarget.toString())) {
                line.append("  <- latest");
            }
            output.accept(line.toString());
        }
    }

    // ---- Log event rendering ----

    /**
     * Render a list of log events (from filter results) as a log view.
     * Each event shows the level, context (module/mojo), and message.
     */
    public void renderLogEvents(List<Map<String, Object>> events) {
        if (events.isEmpty()) {
            output.accept(messageBuilderFactory
                    .builder()
                    .warning("No matching log events found.")
                    .toString());
            return;
        }

        output.accept(messageBuilderFactory
                .builder()
                .strong(events.size() + " matching log event" + (events.size() > 1 ? "s" : "") + ":")
                .toString());
        output.accept("");

        for (Map<String, Object> event : events) {
            String level = getString(event, "level");
            String message = getString(event, "message");
            String context = getString(event, "context");
            String loggerName = getString(event, "loggerName");

            MessageBuilder mb = messageBuilderFactory.builder();

            // Level tag with color
            if ("ERROR".equals(level)) {
                mb.failure("[ERROR]");
            } else if ("WARN".equals(level)) {
                mb.warning("[WARN] ");
            } else if ("DEBUG".equals(level)) {
                mb.debug("[DEBUG]");
            } else if ("TRACE".equals(level)) {
                mb.a("[TRACE]");
            } else {
                mb.info("[INFO] ");
            }

            // Context (module:goal or just module)
            if (context != null && !"build".equals(context)) {
                mb.a(" ");
                mb.strong(context);
            }

            // Logger name (shortened)
            if (loggerName != null) {
                mb.a(" ");
                mb.a(shortenLoggerName(loggerName));
            }

            // Message
            if (message != null) {
                mb.a(" - ").a(message);
            }

            output.accept(mb.toString());
        }
    }

    /**
     * Shorten a fully-qualified logger name to its simple class name.
     */
    private static String shortenLoggerName(String loggerName) {
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }

    // ---- Problem rendering ----

    /**
     * Compact problem rendering for the default summary view.
     * Shows severity, message, source, and suggestion on two lines.
     */
    private void renderProblemCompact(Map<String, Object> problem) {
        String severity = getString(problem, "severity");
        String message = getString(problem, "message");
        String source = getString(problem, "source");
        String suggestion = getString(problem, "suggestion");

        MessageBuilder mb = messageBuilderFactory.builder();
        if ("ERROR".equals(severity)) {
            mb.failure("  [ERROR] ");
        } else if ("WARNING".equals(severity)) {
            mb.warning("  [WARN]  ");
        } else {
            mb.a("  [INFO]  ");
        }
        mb.a(message);
        if (source != null && !source.isEmpty()) {
            mb.a("  ").a(messageBuilderFactory.builder().strong(source).toString());
        }
        output.accept(mb.toString());

        if (suggestion != null && !suggestion.isEmpty()) {
            output.accept("          suggestion: " + suggestion);
        }
    }

    /**
     * Detailed problem rendering for the {@code --diagnostics} view.
     * Shows all available fields: key, severity, message, source, location,
     * suggestion, and documentation URL.
     */
    private void renderProblemDetailed(Map<String, Object> problem) {
        String severity = getString(problem, "severity");
        String message = getString(problem, "message");
        String key = getString(problem, "key");
        String source = getString(problem, "source");
        String suggestion = getString(problem, "suggestion");
        String docUrl = getString(problem, "documentationUrl");

        // Severity label + message
        MessageBuilder mb = messageBuilderFactory.builder();
        if ("ERROR".equals(severity)) {
            mb.failure("  [ERROR] ");
        } else if ("WARNING".equals(severity)) {
            mb.warning("  [WARN]  ");
        } else {
            mb.a("  [INFO]  ");
        }
        mb.a(message);
        output.accept(mb.toString());

        // Key (diagnostic identifier for suppression)
        if (key != null && !key.isEmpty()) {
            output.accept("           key: " + key);
        }

        // Source + location
        if (source != null && !source.isEmpty()) {
            StringBuilder loc = new StringBuilder("           source: ");
            loc.append(source);
            Number line = getNumber(problem, "line");
            if (line != null && line.intValue() > 0) {
                loc.append(":").append(line.intValue());
                Number column = getNumber(problem, "column");
                if (column != null && column.intValue() > 0) {
                    loc.append(":").append(column.intValue());
                }
            }
            output.accept(loc.toString());
        }

        // Suggestion
        if (suggestion != null && !suggestion.isEmpty()) {
            MessageBuilder sugMb = messageBuilderFactory.builder();
            sugMb.a("           suggestion: ").success(suggestion);
            output.accept(sugMb.toString());
        }

        // Documentation URL
        if (docUrl != null && !docUrl.isEmpty()) {
            output.accept("           docs: " + docUrl);
        }

        output.accept("");
    }

    // ---- Internal helpers ----

    private void renderHeader(Map<String, Object> report) {
        String mavenVersion = getString(report, "mavenVersion");
        String startTime = getString(report, "startTime");

        MessageBuilder header = messageBuilderFactory.builder();
        header.strong("Build Report");
        if (mavenVersion != null) {
            header.a(" — Maven ").a(mavenVersion);
        }
        if (startTime != null) {
            header.a(" — ").a(startTime);
        }
        output.accept(header.toString());

        // Result line
        String status = getString(report, "status");
        MessageBuilder result = messageBuilderFactory.builder();
        if ("FAILURE".equals(status)) {
            result.failure("BUILD FAILURE");
        } else {
            result.success("BUILD SUCCESS");
        }
        output.accept(result.toString());
        output.accept("");
    }

    private void renderModuleLine(Map<String, Object> module) {
        String artifactId = getString(module, "artifactId");
        String status = getString(module, "status");
        String duration = getString(module, "duration");

        StringBuilder buffer = new StringBuilder(128);

        // Status marker
        buffer.append(' ');

        buffer.append(artifactId);
        buffer.append(' ');

        // Pad with dots
        int maxLen = 60;
        if (buffer.length() <= maxLen) {
            while (buffer.length() < maxLen) {
                buffer.append('.');
            }
            buffer.append(' ');
        }

        MessageBuilder mb = messageBuilderFactory.builder();
        mb.a(buffer);
        switch (status) {
            case "SUCCESS":
                mb.success(status);
                break;
            case "FAILURE":
                mb.failure(status);
                break;
            default:
                mb.warning(status);
                break;
        }

        // Duration
        if (duration != null) {
            mb.a(" [").a(formatDuration(duration)).a("]");
        }

        output.accept(mb.toString());
    }

    /**
     * Format an ISO-8601 duration string (e.g. "PT2.1S") into a human-readable form.
     */
    static String formatDuration(String isoDuration) {
        try {
            Duration d = Duration.parse(isoDuration);
            long totalSeconds = d.getSeconds();
            int millis = d.getNano() / 1_000_000;

            if (totalSeconds >= 60) {
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                return String.format("%d:%02d min", minutes, seconds);
            } else {
                return String.format("%d.%03d s", totalSeconds, millis);
            }
        } catch (Exception e) {
            return isoDuration; // fallback to raw string
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return List.of();
    }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static Number getNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return (Number) value;
        }
        return null;
    }
}
