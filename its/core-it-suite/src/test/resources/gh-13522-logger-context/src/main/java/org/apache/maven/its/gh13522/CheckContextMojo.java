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

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.Logger;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * Verifies that a DI-injected {@link Logger} carries the correct
 * {@code projectId}, {@code mojoId}, and {@code pluginId} on each
 * {@link LogEvent} it emits.
 *
 * <p>The mojo wraps the global {@link LoggerManager} sink, captures
 * the first event it emits, extracts the context fields, and writes
 * them to {@code target/log-context.properties} so the IT verifier
 * can assert their values.</p>
 */
@Mojo(name = "check-context", defaultPhase = org.apache.maven.api.Lifecycle.Phase.VALIDATE)
public class CheckContextMojo implements org.apache.maven.api.plugin.Mojo {

    @Inject
    private Logger logger;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    @Override
    public void execute() {
        AtomicReference<LogEvent> captured = new AtomicReference<>();

        // Wrap the existing sink to intercept the next event from this logger
        LoggerManager mgr = LoggerManager.get();
        org.apache.maven.api.logging.LogSink previousSink = mgr.getLogSink();
        mgr.setLogSink(event -> {
            if (logger.name().equals(event.loggerName())) {
                captured.compareAndSet(null, event);
            }
            if (previousSink != null) {
                previousSink.accept(event);
            }
        });

        try {
            logger.info("gh-13522: checking logger context fields");
        } finally {
            mgr.setLogSink(previousSink);
        }

        LogEvent event = captured.get();
        if (event == null) {
            throw new RuntimeException("No LogEvent was captured — logger did not emit to the sink");
        }

        Properties props = new Properties();
        props.setProperty("projectId", nvl(event.projectId()));
        props.setProperty("mojoId", nvl(event.mojoId()));
        props.setProperty("pluginId", nvl(event.pluginId()));
        props.setProperty("loggerName", nvl(event.loggerName()));

        Path out = Paths.get(buildDirectory).resolve("log-context.properties");
        try {
            Files.createDirectories(out.getParent());
            try (OutputStream os = Files.newOutputStream(out)) {
                props.store(os, "Captured from gh-13522 IT");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log-context.properties", e);
        }

        logger.info(
                "gh-13522: projectId={}, mojoId={}, pluginId={}", event.projectId(), event.mojoId(), event.pluginId());
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
