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
package org.apache.maven.its.gh13522;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * Verifies that the logger context ({@code projectId}, {@code mojoId}) is correctly
 * populated during mojo execution and that log events carry the expected fields.
 *
 * <p>{@code projectId} and {@code mojoId} are derived from the injected
 * {@link Project} and {@link MojoExecution} objects — the same values that
 * {@code DefaultBuildPluginManager} stamps onto every {@link LogEvent} via
 * the enrichment sink. {@code pluginId} and {@code loggerName} are read from
 * an intercepted event (they are set on raw events before enrichment).
 * Results are written to {@code target/log-context.properties} for the IT verifier.</p>
 */
@Mojo(name = "check-context", defaultPhase = org.apache.maven.api.Lifecycle.Phase.VALIDATE, projectRequired = true)
public class CheckContextMojo implements org.apache.maven.api.plugin.Mojo {

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    @Inject
    private Project project;

    @Inject
    private MojoExecution mojoExecution;

    @Override
    public void execute() {
        // Derive projectId / mojoId from public API objects injected by Maven.
        // These are the same values that DefaultBuildPluginManager stamps onto
        // every LogEvent via the enrichment sink.
        String projectId = project.getArtifactId();
        String mojoId = mojoExecution.descriptor().getFullGoalName()
                + "@" + mojoExecution.executionId().orElse("");

        // Intercept one event to capture pluginId and loggerName.
        // pluginId is baked into the logger at creation time (before enrichment),
        // so it is present even on raw events seen by the custom sink.
        String expectedLoggerName = getClass().getName();
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        LoggerManager mgr = LoggerManager.get();
        LogSink previousSink = mgr.getLogSink();
        mgr.setLogSink(event -> {
            if (expectedLoggerName.equals(event.loggerName())) {
                captured.compareAndSet(null, event);
            }
            if (previousSink != null) {
                previousSink.accept(event);
            }
        });

        try {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .info("gh-13522: checking logger context fields");
        } finally {
            mgr.setLogSink(previousSink);
        }

        LogEvent event = captured.get();
        String pluginId = event != null ? nvl(event.pluginId()) : "";
        String loggerName = event != null ? nvl(event.loggerName()) : "";

        Properties props = new Properties();
        props.setProperty("projectId", projectId);
        props.setProperty("mojoId", mojoId);
        props.setProperty("pluginId", pluginId);
        props.setProperty("loggerName", loggerName);

        Path out = Paths.get(buildDirectory).resolve("log-context.properties");
        try {
            Files.createDirectories(out.getParent());
            try (OutputStream os = Files.newOutputStream(out)) {
                props.store(os, "Captured from gh-13522 IT");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log-context.properties", e);
        }

        org.slf4j.LoggerFactory.getLogger(getClass())
                .info("gh-13522: projectId={}, mojoId={}, pluginId={}", projectId, mojoId, pluginId);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
