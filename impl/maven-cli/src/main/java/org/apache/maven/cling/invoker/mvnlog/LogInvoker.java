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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnlog.LogOptions;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;

public class LogInvoker extends LookupInvoker<LogContext> {

    private static volatile long lastRequestTime = System.currentTimeMillis();

    public LogInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        super(protoLookup, contextConsumer);
    }

    @Override
    protected LogContext createContext(InvokerRequest invokerRequest) {
        return new LogContext(invokerRequest, (LogOptions) invokerRequest.options().orElse(null));
    }

    @Override
    protected int execute(LogContext context) throws Exception {
        Path reportFile = findReportFile(context);
        if (reportFile == null || !Files.exists(reportFile)) {
            context.logger.error("No build reports found in .mvn/reports/ or ~/.mvn/reports/");
            return 1;
        }

        if (context.options().web().orElse(false)) {
            return startWebServer(context, reportFile);
        } else {
            return renderTerminalOutput(context, reportFile);
        }
    }

    private Path findReportFile(LogContext context) {
        if (context.options().file().isPresent()) {
            return Paths.get(context.options().file().get());
        }

        // Check project .mvn/reports
        Path reportsDir = Paths.get(".mvn", "reports");
        Path report = findLatestReport(reportsDir);
        if (report != null) {
            return report;
        }

        // Check user home .mvn/reports
        Path userReportsDir = Paths.get(System.getProperty("user.home"), ".mvn", "reports");
        report = findLatestReport(userReportsDir);
        if (report != null) {
            return report;
        }

        return null;
    }

    private Path findLatestReport(Path dir) {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(p -> p.getFileName().toString().startsWith("build-report-") && p.getFileName().toString().endsWith(".json"))
                .max(java.util.Comparator.comparing(Path::toString))
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private int renderTerminalOutput(LogContext context, Path reportFile) {
        try {
            String content = Files.readString(reportFile);
            String status = parseJsonStringField(content, "status");
            long startTime = parseJsonLongField(content, "startTime");
            long durationMs = parseJsonLongField(content, "durationMs");
            String mavenVersion = parseJsonStringField(content, "mavenVersion");
            String goals = parseJsonArrayField(content, "goals");

            context.logger.info("========================================================================");
            if ("SUCCESS".equals(status)) {
                context.logger.info("BUILD SUCCESS REPORT: " + reportFile.getFileName());
            } else {
                context.logger.error("BUILD FAILURE REPORT: " + reportFile.getFileName());
            }
            context.logger.info("========================================================================");
            context.logger.info("Maven Version: " + mavenVersion);
            context.logger.info("Start Time:    " + new Date(startTime));
            context.logger.info("Duration:      " + formatDuration(durationMs));
            context.logger.info("Goals:         " + goals);
            context.logger.info("------------------------------------------------------------------------");

            // Simple parsing of modules
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"id\"\\s*:\\s*\"([^\"]+)\",\\s*\"status\"\\s*:\\s*\"([^\"]+)\",\\s*\"startTime\"\\s*:\\s*(\\d+),\\s*\"endTime\"\\s*:\\s*(\\d+),\\s*\"durationMs\"\\s*:\\s*(\\d+)"
            ).matcher(content);

            context.logger.info("Modules built:");
            while (m.find()) {
                String name = m.group(1);
                String modStatus = m.group(2);
                long dur = Long.parseLong(m.group(5));
                String statusStr = "SUCCESS".equals(modStatus) ? "[SUCCESS]" : "[FAILED]";
                context.logger.info(String.format("  %-40s %-10s (%s)", name, statusStr, formatDuration(dur)));
            }
            context.logger.info("========================================================================");

            // Simple count of warnings / errors
            int warnings = 0;
            int errors = 0;
            java.util.regex.Matcher pm = java.util.regex.Pattern.compile("\"severity\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
            while (pm.find()) {
                if ("WARN".equals(pm.group(1))) warnings++;
                if ("ERROR".equals(pm.group(1))) errors++;
            }
            if (warnings > 0 || errors > 0) {
                context.logger.info(String.format("Build Problems: %d warnings, %d errors. Run with --web to explore.", warnings, errors));
                context.logger.info("========================================================================");
            }

            return 0;
        } catch (Exception e) {
            context.logger.error("Failed to parse report file: " + e.getMessage());
            return 1;
        }
    }

    private int startWebServer(LogContext context, Path reportFile) {
        int preferredPort = context.options().port().orElse(8000);
        int actualPort = preferredPort;

        HttpServer server = null;
        // Port selection logic: try preferred port, fall back to random available port if busy
        try {
            server = HttpServer.create(new InetSocketAddress(preferredPort), 0);
        } catch (IOException e) {
            // Find random available port
            try (ServerSocket socket = new ServerSocket(0)) {
                actualPort = socket.getLocalPort();
                server = HttpServer.create(new InetSocketAddress(actualPort), 0);
            } catch (IOException ioException) {
                context.logger.error("Failed to start web server on port " + preferredPort + " and failed to allocate dynamic port: " + ioException.getMessage());
                return 1;
            }
        }

        context.logger.info("Starting local HTTP server serving " + reportFile.getFileName());
        context.logger.info("Open browser: http://localhost:" + actualPort);

        // Serve Frontend
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                lastRequestTime = System.currentTimeMillis();
                if (!"/".equals(exchange.getRequestURI().getPath())) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                try (InputStream is = getClass().getResourceAsStream("/org/apache/maven/cling/invoker/mvnlog/report.html")) {
                    if (is == null) {
                        byte[] err = "report.html not found in classpath".getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(404, err.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(err);
                        }
                        return;
                    }
                    byte[] bytes = is.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            }
        });

        // Serve raw build-report.json content
        server.createContext("/api/report", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                lastRequestTime = System.currentTimeMillis();
                Path fileToServe = reportFile;
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("id=")) {
                    String reqId = query.substring(query.indexOf("id=") + 3);
                    if (reqId.contains("&")) {
                        reqId = reqId.substring(0, reqId.indexOf("&"));
                    }
                    Path p1 = Paths.get(".mvn", "reports", reqId);
                    Path p2 = Paths.get(System.getProperty("user.home"), ".mvn", "reports", reqId);
                    if (Files.exists(p1)) {
                        fileToServe = p1;
                    } else if (Files.exists(p2)) {
                        fileToServe = p2;
                    }
                }

                if (!Files.exists(fileToServe)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                byte[] bytes = Files.readAllBytes(fileToServe);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        // Serve lists of available reports
        server.createContext("/api/reports", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                lastRequestTime = System.currentTimeMillis();
                byte[] bytes = listReportsJson().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        // Auto-open browser
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(URI.create("http://localhost:" + actualPort));
            }
        } catch (Exception e) {
            // Ignore if headless/desktop not supported
        }

        // Self-terminating after 30 minutes of inactivity
        ScheduledExecutorService shutdownExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mvnlog-shutdown-monitor");
            t.setDaemon(true);
            return t;
        });

        HttpServer finalServer = server;
        shutdownExecutor.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastRequestTime > 30 * 60 * 1000) {
                context.logger.info("Terminating server due to 30 minutes of inactivity.");
                finalServer.stop(0);
                shutdownExecutor.shutdown();
                System.exit(0);
            }
        }, 1, 1, TimeUnit.MINUTES);

        // Keep server thread running
        try {
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            server.stop(0);
            shutdownExecutor.shutdown();
        }

        return 0;
    }

    private String listReportsJson() {
        List<Map<String, Object>> reportsList = new ArrayList<>();
        scanReportsDirectory(Paths.get(".mvn", "reports"), reportsList);
        scanReportsDirectory(Paths.get(System.getProperty("user.home"), ".mvn", "reports"), reportsList);

        // Sort by startTime descending
        reportsList.sort((a, b) -> Long.compare((Long) b.get("startTime"), (Long) a.get("startTime")));

        // Build a manual JSON string to avoid dependencies
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < reportsList.size(); i++) {
            Map<String, Object> r = reportsList.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escapeJson((String) r.get("id"))).append("\",\n");
            sb.append("    \"status\": \"").append(r.get("status")).append("\",\n");
            sb.append("    \"startTime\": ").append(r.get("startTime")).append(",\n");
            sb.append("    \"durationMs\": ").append(r.get("durationMs")).append(",\n");
            sb.append("    \"goals\": ").append(r.get("goals")).append("\n");
            sb.append("  }").append(i < reportsList.size() - 1 ? "," : "").append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private void scanReportsDirectory(Path dir, List<Map<String, Object>> reportsList) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream
                .filter(p -> p.getFileName().toString().startsWith("build-report-") && p.getFileName().toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        String status = parseJsonStringField(content, "status");
                        long startTime = parseJsonLongField(content, "startTime");
                        long durationMs = parseJsonLongField(content, "durationMs");
                        String goals = parseJsonArrayField(content, "goals");

                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", p.getFileName().toString());
                        map.put("status", status != null ? status : "unknown");
                        map.put("startTime", startTime);
                        map.put("durationMs", durationMs);
                        map.put("goals", goals != null ? goals : "[]");
                        reportsList.add(map);
                    } catch (Exception e) {
                        // Skip unreadable files
                    }
                });
        } catch (IOException e) {
            // Ignore
        }
    }

    private String parseJsonStringField(String content, String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private long parseJsonLongField(String content, String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(content);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private String parseJsonArrayField(String content, String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(content);
        return m.find() ? "[" + m.group(1) + "]" : "[]";
    }

    private static String formatDuration(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        s %= 60;
        if (m > 0) {
            return m + "m " + s + "s";
        }
        return s + "s";
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
}
