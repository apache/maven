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

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.logging.ProjectBuildLogAppender;

/**
 * Verifies that the {@link ProjectBuildLogAppender} ThreadLocal context
 * ({@code projectId}, {@code mojoId}) is correctly populated during mojo
 * execution, and that an intercepted {@link LogEvent} carries the expected
 * enriched fields after passing through {@link ProjectBuildLogAppender}.
 *
 * <p>The mojo reads the context values directly from
 * {@link ProjectBuildLogAppender#getProjectId()} and
 * {@link ProjectBuildLogAppender#getMojoId()} (the authoritative ThreadLocal
 * source), and also intercepts one event to capture {@code pluginId} and
 * {@code loggerName}. Results are written to
 * {@code target/log-context.properties} for the IT verifier.</p>
 */
@Mojo(name = "check-context", defaultPhase = org.apache.maven.api.Lifecycle.Phase.VALIDATE, projectRequired = true)
public class CheckContextMojo implements org.apache.maven.api.plugin.Mojo {

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    @Override
    public void execute() {
        // Read projectId/mojoId directly from ProjectBuildLogAppender's ThreadLocals —
        // the authoritative source stamped onto every LogEvent during mojo execution.
        // We do NOT intercept events for these: by the time the custom sink sees a raw
        // event, projectId/mojoId have not yet been enriched (enrichment happens inside
        // ProjectBuildLogAppender.Sink.accept(), which is the previous sink we wrap).
        String projectId = ProjectBuildLogAppender.getProjectId();
        String mojoId = ProjectBuildLogAppender.getMojoId();

        // Intercept one event (via the existing sink) to capture pluginId/loggerName.
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        LoggerManager mgr = LoggerManager.get();
        org.apache.maven.api.logging.LogSink previousSink = mgr.getLogSink();
        mgr.setLogSink(event -> {
            captured.compareAndSet(null, event);
            if (previousSink != null) {
                previousSink.accept(event);
            }
        });

        try {
            // Use SLF4J so the event travels through MavenSimpleLogger and gets
            // enriched by ProjectBuildLogAppender before reaching our wrapper.
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .info("gh-13522: checking logger context fields");
        } finally {
            mgr.setLogSink(previousSink);
        }

        LogEvent event = captured.get();
        String pluginId = event != null ? nvl(event.pluginId()) : "";
        String loggerName = event != null ? nvl(event.loggerName()) : "";

        Properties props = new Properties();
        props.setProperty("projectId", nvl(projectId));
        props.setProperty("mojoId", nvl(mojoId));
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
