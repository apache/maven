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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

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
 *
 * @since 4.1.0
 */
public class LogInvoker extends LookupInvoker<LogContext> {

    public static final int OK = 0;
    public static final int ERROR = 1;
    public static final int BAD_INPUT = 2;

    private static final String DEFAULT_REPORT_DIR = "target/build-reports";
    private static final String DEFAULT_REPORT_FILE = "build-report-latest.json";

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

        // Handle --json: output the raw JSON and exit
        if (options != null && options.json().orElse(false)) {
            output.accept(json);
            return OK;
        }

        Map<String, Object> report;
        try {
            report = SimpleJsonReader.parse(json);
        } catch (IllegalArgumentException e) {
            context.logger.error("Failed to parse report file: " + e.getMessage());
            return ERROR;
        }

        // Render based on flags
        if (options != null && options.full().orElse(false)) {
            renderer.renderFull(report);
        } else if (options != null && options.failures().orElse(false)) {
            renderer.renderFailures(report);
        } else if (options != null && options.diagnostics().orElse(false)) {
            renderer.renderDiagnostics(report);
        } else {
            renderer.renderSummary(report);
        }

        return OK;
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
}
