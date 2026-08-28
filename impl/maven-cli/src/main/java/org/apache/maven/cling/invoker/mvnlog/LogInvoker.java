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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnlog.LogOptions;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;

/**
 * Invoker for the {@code mvnlog} build log viewer.
 * <p>
 * This is a lightweight invoker that does NOT set up the DI container,
 * Maven settings, or any build infrastructure. It only needs a terminal
 * (for colors and width detection) and the parsed CLI options.
 * <p>
 * Supports two output modes:
 * <ul>
 *   <li>Terminal mode (default): renders formatted build report via {@link BuildReportRenderer}</li>
 *   <li>Web mode ({@code --web}): starts a local HTTP server with an interactive report viewer</li>
 * </ul>
 *
 * @since 4.1.0
 */
public class LogInvoker extends LookupInvoker<LogContext> {

    public static final int OK = 0;
    public static final int ERROR = 1;
    public static final int BAD_INPUT = 2;

    private static final String DEFAULT_REPORT_DIR = "target/build-reports";
    private static final String DEFAULT_REPORT_FILE = "build-report-latest.json";
    private static final int DEFAULT_WEB_PORT = 8080;
    private static final long INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * Pattern to validate report IDs: only allows simple filenames
     * (letters, digits, hyphens, dots) to prevent path traversal attacks.
     */
    private static final Pattern SAFE_REPORT_ID = Pattern.compile("[a-zA-Z0-9._-]+\\.json");

    /**
     * Tracks the time of the last HTTP request for the inactivity shutdown timer.
     * Instance field (not static) so multiple LogInvoker instances don't interfere.
     */
    private volatile long lastRequestTime = System.currentTimeMillis();

    public LogInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        super(protoLookup, contextConsumer);
    }

    @Override
    protected LogContext createContext(InvokerRequest invokerRequest) {
        return new LogContext(
                invokerRequest, (LogOptions) invokerRequest.options().orElse(null));
    }

    /**
     * Override doInvoke to skip the heavyweight DI container, settings,
     * and repository setup that mvnlog does not need.
     */
    @Override
    protected int doInvoke(LogContext context) throws Exception {
        validate(context);
        pushCoreProperties(context);
        configureLogging(context);
        createTerminal(context);
        activateLogging(context);
        helpOrVersionAndMayExit(context);
        return execute(context);
    }

    @Override
    protected void lookup(LogContext context) throws Exception {
        // No DI container needed for log viewing
    }

    @Override
    protected int execute(LogContext context) throws Exception {
        LogOptions options = context.options();

        // Check for --web mode
        if (options != null && options.web().orElse(false)) {
            return startWebServer(context);
        }

        // Terminal mode below
        Consumer<String> output = line -> {
            if (context.writer != null) {
                context.writer.accept(line);
            } else {
                context.logger.info(line);
            }
        };

        BuildReportRenderer renderer = new BuildReportRenderer(context.invokerRequest.messageBuilderFactory(), output);

        // Handle --list: show available reports
        if (options != null && options.list().orElse(false)) {
            Path reportDir = resolveReportDir(context);
            renderer.listReports(reportDir);
            return OK;
        }

        // Resolve and read the report file
        Path reportFile = resolveReportFile(context);
        if (!Files.isRegularFile(reportFile)) {
            context.logger.error("Build report not found: " + reportFile);
            context.logger.error("Run a Maven build first, then use mvnlog to view the report.");
            return BAD_INPUT;
        }

        String json;
        try {
            json = Files.readString(reportFile);
        } catch (IOException e) {
            context.logger.error("Failed to read report file: " + e.getMessage());
            return ERROR;
        }

        Map<String, Object> report;
        try {
            report = SimpleJsonReader.parse(json);
        } catch (IllegalArgumentException e) {
            context.logger.error("Failed to parse report file: " + e.getMessage());
            return ERROR;
        }

        // Build filter from options
        BuildReportFilter filter = buildFilter(options);

        // Handle --json: output JSON (filtered if applicable) and exit
        if (options != null && options.json().orElse(false)) {
            if (filter.hasFilters()) {
                Map<String, Object> filtered = filter.apply(report);
                output.accept(SimpleJsonWriter.toJson(filtered));
            } else {
                output.accept(json);
            }
            return OK;
        }

        // If log-level filters are active (--level, --grep), render a log view
        // instead of the default summary.
        if (filter.hasLogFilters()) {
            List<Map<String, Object>> events = filter.collectMatchingLogEvents(report);
            renderer.renderLogEvents(events);
            return OK;
        }

        // Apply structural filters (--module, --mojo) to the report
        Map<String, Object> filteredReport = filter.apply(report);

        // Render based on flags
        if (options != null && options.full().orElse(false)) {
            renderer.renderFull(filteredReport);
        } else if (options != null && options.failures().orElse(false)) {
            renderer.renderFailures(filteredReport);
        } else if (options != null && options.diagnostics().orElse(false)) {
            renderer.renderDiagnostics(filteredReport);
        } else {
            renderer.renderSummary(filteredReport);
        }

        return OK;
    }

    // ---- Web server mode ----

    private int startWebServer(LogContext context) {
        Path reportFile = resolveReportFile(context);
        if (!Files.isRegularFile(reportFile)) {
            context.logger.error("Build report not found: " + reportFile);
            context.logger.error("Run a Maven build first, then use mvnlog --web to view the report.");
            return BAD_INPUT;
        }

        Path reportDir = reportFile.getParent();
        int preferredPort = DEFAULT_WEB_PORT;
        LogOptions options = context.options();
        if (options != null) {
            preferredPort = options.port().orElse(DEFAULT_WEB_PORT);
        }

        HttpServer server;
        int actualPort;
        try {
            // Bind to loopback only (not 0.0.0.0) to prevent network exposure
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), preferredPort);
            server = HttpServer.create(addr, 0);
            actualPort = preferredPort;
        } catch (IOException e) {
            // Preferred port busy, try a random available port on loopback
            try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
                actualPort = socket.getLocalPort();
                socket.close();
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), actualPort), 0);
            } catch (IOException ioException) {
                context.logger.error("Failed to start web server: " + ioException.getMessage());
                return ERROR;
            }
        }

        context.logger.info("Starting Maven Build Report viewer on http://localhost:" + actualPort);

        // Serve the HTML frontend
        server.createContext("/", exchange -> {
            lastRequestTime = System.currentTimeMillis();
            if (!"/".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            serveClasspathResource(exchange, "report.html", "text/html; charset=UTF-8");
        });

        // Serve a specific build report by ID (with path traversal protection)
        server.createContext("/api/report", exchange -> {
            lastRequestTime = System.currentTimeMillis();
            Path fileToServe = reportFile;

            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("id=")) {
                String reqId = extractQueryParam(query, "id");
                if (reqId != null) {
                    // Validate: only allow simple filenames (no path separators, no ..)
                    if (!SAFE_REPORT_ID.matcher(reqId).matches()) {
                        sendError(exchange, 400, "Invalid report ID");
                        return;
                    }
                    Path candidate = reportDir.resolve(reqId);
                    // Double-check the resolved path is still within the report directory
                    if (!candidate.normalize().startsWith(reportDir.normalize())) {
                        sendError(exchange, 400, "Invalid report ID");
                        return;
                    }
                    if (Files.isRegularFile(candidate)) {
                        fileToServe = candidate;
                    } else {
                        sendError(exchange, 404, "Report not found");
                        return;
                    }
                }
            }

            if (!Files.isRegularFile(fileToServe)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            byte[] bytes = Files.readAllBytes(fileToServe);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // List available reports
        server.createContext("/api/reports", exchange -> {
            lastRequestTime = System.currentTimeMillis();
            byte[] bytes = listReportsJson(reportDir).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        // Auto-open browser (best-effort, ignore if headless)
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(URI.create("http://localhost:" + actualPort));
            }
        } catch (Exception e) {
            // Ignore — headless or desktop not supported
        }

        // Graceful inactivity shutdown (no System.exit!)
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        ScheduledExecutorService shutdownExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mvnlog-shutdown-monitor");
            t.setDaemon(true);
            return t;
        });

        HttpServer finalServer = server;
        shutdownExecutor.scheduleAtFixedRate(
                () -> {
                    if (System.currentTimeMillis() - lastRequestTime > INACTIVITY_TIMEOUT_MS) {
                        context.logger.info("Terminating server after 30 minutes of inactivity.");
                        finalServer.stop(1);
                        shutdownExecutor.shutdown();
                        shutdownLatch.countDown();
                    }
                },
                1,
                1,
                TimeUnit.MINUTES);

        // Block until inactivity shutdown or interrupt (Ctrl+C)
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.logger.info("Server interrupted, shutting down.");
            server.stop(1);
            shutdownExecutor.shutdown();
        }

        return OK;
    }

    private void serveClasspathResource(HttpExchange exchange, String resource, String contentType) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/org/apache/maven/cling/invoker/mvnlog/" + resource)) {
            if (is == null) {
                sendError(exchange, 404, resource + " not found");
                return;
            }
            byte[] bytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String extractQueryParam(String query, String param) {
        String prefix = param + "=";
        int start = query.indexOf(prefix);
        if (start == -1) {
            return null;
        }
        start += prefix.length();
        int end = query.indexOf('&', start);
        return end == -1 ? query.substring(start) : query.substring(start, end);
    }

    private String listReportsJson(Path reportDir) {
        List<String> entries = new ArrayList<>();
        if (Files.isDirectory(reportDir)) {
            try (Stream<Path> stream = Files.list(reportDir)) {
                stream.filter(p -> {
                            String name = p.getFileName().toString();
                            return name.startsWith("build-report-")
                                    && name.endsWith(".json")
                                    && !name.equals("build-report-latest.json");
                        })
                        .sorted((a, b) -> b.getFileName()
                                .toString()
                                .compareTo(a.getFileName().toString()))
                        .forEach(p -> {
                            try {
                                String content = Files.readString(p);
                                Map<String, Object> report = SimpleJsonReader.parse(content);
                                String id = p.getFileName().toString();
                                // BuildReportCollector uses flat top-level fields
                                String reportStatus = String.valueOf(report.getOrDefault("status", "UNKNOWN"));
                                Object startTime = report.getOrDefault("startTime", "");
                                Object duration = report.getOrDefault("duration", "");
                                Object goals = report.getOrDefault("goals", List.of());
                                entries.add("  {\"id\": \"" + escapeJson(id) + "\", \"status\": \""
                                        + escapeJson(reportStatus) + "\", \"startTime\": \""
                                        + escapeJson(String.valueOf(startTime)) + "\", \"duration\": \""
                                        + escapeJson(String.valueOf(duration)) + "\", \"goals\": "
                                        + SimpleJsonWriter.toJson(goals) + "}");
                            } catch (Exception e) {
                                // Skip unreadable files
                            }
                        });
            } catch (IOException e) {
                // Ignore
            }
        }
        return "[\n" + String.join(",\n", entries) + "\n]";
    }

    // ---- Helpers ----

    private static BuildReportFilter buildFilter(LogOptions options) {
        if (options == null) {
            return new BuildReportFilter(null, null, null, null);
        }
        return new BuildReportFilter(
                options.module().orElse(null),
                options.mojo().orElse(null),
                options.level().orElse(null),
                options.grep().orElse(null));
    }

    private Path resolveReportDir(LogContext context) {
        Path cwd = context.invokerRequest.cwd();
        return cwd.resolve(DEFAULT_REPORT_DIR);
    }

    private Path resolveReportFile(LogContext context) {
        LogOptions options = context.options();

        // Explicit report file path from command line
        if (options != null) {
            String reportFile = options.reportFile().orElse(null);
            if (reportFile != null) {
                Path path = Path.of(reportFile);
                if (path.isAbsolute()) {
                    return path;
                }
                return context.invokerRequest.cwd().resolve(path);
            }
        }

        // Default: target/build-reports/build-report-latest.json
        return resolveReportDir(context).resolve(DEFAULT_REPORT_FILE);
    }

    private static String escapeJson(String string) {
        if (string == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
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
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
