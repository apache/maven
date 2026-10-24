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
package org.apache.maven.api.logging;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Minimal {@link LoggerManager} active during early bootstrap, before
 * the real implementation (from {@code maven-logging}) is available.
 * <p>
 * Accepts all log levels and buffers events in a lock-free queue.
 * When the real manager is discovered (via {@link java.util.ServiceLoader}
 * or {@link LoggerManager#set(LoggerManager)}), buffered events are
 * drained into the real manager's sink in FIFO order, and any pending
 * configuration (root log level) is forwarded.
 */
final class BootstrapLoggerManager implements LoggerManager {

    private final ConcurrentLinkedQueue<LogEvent> events = new ConcurrentLinkedQueue<>();
    private volatile LogLevel pendingRootLevel;

    @Override
    public Logger getLogger(String name) {
        return new BootstrapLogger(this, name);
    }

    @Override
    public LogSink getLogSink() {
        return null;
    }

    @Override
    public void setLogSink(LogSink sink) {
        // ignored during bootstrap
    }

    @Override
    public boolean isLevelEnabled(String loggerName, LogLevel level) {
        return true;
    }

    @Override
    public void setRootLoggerLevel(LogLevel level) {
        this.pendingRootLevel = level;
    }

    @Override
    public void reconfigure() {
        // no-op during bootstrap
    }

    @Override
    public boolean hasReached(LogLevel level) {
        return false;
    }

    void buffer(LogEvent event) {
        events.add(event);
    }

    void drainTo(LoggerManager target) {
        if (pendingRootLevel != null) {
            target.setRootLoggerLevel(pendingRootLevel);
        }
        LogSink sink = target.getLogSink();
        if (sink != null) {
            LogEvent event;
            while ((event = events.poll()) != null) {
                sink.accept(event);
            }
        }
    }
}
