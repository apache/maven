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

import org.apache.maven.slf4j.MavenSimpleLogger;
import org.slf4j.MDC;

/**
 * Forwards log messages to the client.
 */
public class ProjectBuildLogAppender implements AutoCloseable {

    private static final String KEY_PROJECT_ID = "maven.project.id";
    private static final String KEY_MOJO_ID = "maven.mojo.id";
    private static final ThreadLocal<String> PROJECT_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> MOJO_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> FORKING_PROJECT_ID = new InheritableThreadLocal<>();

    public static String getProjectId() {
        return PROJECT_ID.get();
    }

    public static void setProjectId(String projectId) {
        String forkingProjectId = FORKING_PROJECT_ID.get();
        if (forkingProjectId != null) {
            if (projectId != null) {
                projectId = forkingProjectId + "/" + projectId;
            } else {
                projectId = forkingProjectId;
            }
        }
        if (projectId != null) {
            PROJECT_ID.set(projectId);
            MDC.put(KEY_PROJECT_ID, projectId);
        } else {
            PROJECT_ID.remove();
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    public static String getMojoId() {
        return MOJO_ID.get();
    }

    /**
     * Sets or clears the mojo execution identifier in both the thread-local
     * and the SLF4J MDC.  The value is available to any SLF4J appender via
     * the MDC key {@code maven.mojo.id} and to JUL-bridged messages through
     * the same MDC path.
     * <p>
     * Format: {@code "prefix:goal@executionId"}
     * (e.g. {@code "compiler:compile@default-compile"}).
     *
     * @param mojoId the mojo identifier, or {@code null} to clear
     */
    public static void setMojoId(String mojoId) {
        if (mojoId != null) {
            MOJO_ID.set(mojoId);
            MDC.put(KEY_MOJO_ID, mojoId);
        } else {
            MOJO_ID.remove();
            MDC.remove(KEY_MOJO_ID);
        }
    }

    public static void setForkingProjectId(String forkingProjectId) {
        if (forkingProjectId != null) {
            FORKING_PROJECT_ID.set(forkingProjectId);
        } else {
            FORKING_PROJECT_ID.remove();
        }
    }

    public static void updateMdc() {
        String id = getProjectId();
        if (id != null) {
            MDC.put(KEY_PROJECT_ID, id);
        } else {
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    private final BuildEventListener buildEventListener;

    public ProjectBuildLogAppender(BuildEventListener buildEventListener) {
        this.buildEventListener = buildEventListener;
        MavenSimpleLogger.setLogSink(this::accept);
    }

    protected void accept(String message) {
        String projectId = MDC.get(KEY_PROJECT_ID);
        buildEventListener.projectLogMessage(projectId, message);
    }

    @Override
    public void close() throws Exception {
        MavenSimpleLogger.setLogSink(null);
    }
}
