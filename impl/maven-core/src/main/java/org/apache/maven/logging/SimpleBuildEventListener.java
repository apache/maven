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

import java.util.function.Consumer;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.execution.ExecutionEvent;
import org.eclipse.aether.transfer.TransferEvent;

public class SimpleBuildEventListener implements BuildEventListener {

    final Consumer<String> output;

    public SimpleBuildEventListener(Consumer<String> output) {
        this.output = output;
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {}

    @Override
    public void projectStarted(String projectId) {}

    @Override
    public void projectLogMessage(String projectId, LogEvent event) {
        log("[" + renderLevel(event.level()) + "] " + event.message());
        String stackTrace = event.stackTrace();
        if (stackTrace != null) {
            log(stackTrace);
        }
    }

    /**
     * Renders the log level for console output.
     * Maven traditionally uses {@code WARNING} rather than SLF4J's {@code WARN}.
     */
    protected String renderLevel(LogLevel level) {
        return level == LogLevel.WARN ? "WARNING" : level.name();
    }

    @Override
    public void projectFinished(String projectId) {}

    @Override
    public void executionFailure(String projectId, boolean halted, String exception) {}

    @Override
    public void mojoStarted(ExecutionEvent event) {}

    @Override
    public void finish(int exitCode) throws Exception {}

    @Override
    public void fail(Throwable t) throws Exception {}

    @Override
    public void log(String msg) {
        output.accept(msg);
    }

    @Override
    public void transfer(String projectId, TransferEvent e) {}
}
