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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Minimal {@link LogEventBuilder} used during bootstrap.
 * Creates {@link BootstrapLogEvent} instances and buffers them
 * in the {@link BootstrapLoggerManager}.
 */
final class BootstrapLogEventBuilder implements LogEventBuilder {

    private final BootstrapLoggerManager manager;
    private final String loggerName;
    private final LogLevel level;

    private Map<String, String> kvMap;
    private Throwable throwable;

    BootstrapLogEventBuilder(BootstrapLoggerManager manager, String loggerName, LogLevel level) {
        this.manager = manager;
        this.loggerName = loggerName;
        this.level = level;
    }

    @Override
    public LogEventBuilder withKeyValue(String key, String value) {
        ensureKv();
        kvMap.put(key, value);
        return this;
    }

    @Override
    public LogEventBuilder withKeyValue(String key, long value) {
        return withKeyValue(key, Long.toString(value));
    }

    @Override
    public LogEventBuilder withKeyValue(String key, boolean value) {
        return withKeyValue(key, Boolean.toString(value));
    }

    @Override
    public LogEventBuilder withThrowable(Throwable t) {
        this.throwable = t;
        return this;
    }

    @Override
    public LogEventBuilder withProjectId(String projectId) {
        return this;
    }

    @Override
    public LogEventBuilder withMojoId(String mojoId) {
        return this;
    }

    @Override
    public void log(String message) {
        emit(message);
    }

    @Override
    public void log(String format, Object arg) {
        emit(BootstrapLogEvent.formatMessage(format, new Object[] {arg}));
    }

    @Override
    public void log(String format, Object arg1, Object arg2) {
        emit(BootstrapLogEvent.formatMessage(format, new Object[] {arg1, arg2}));
    }

    @Override
    public void log(String format, Object... args) {
        emit(BootstrapLogEvent.formatMessage(format, args));
    }

    @Override
    public void log(Supplier<String> messageSupplier) {
        emit(messageSupplier.get());
    }

    private void emit(String message) {
        Map<String, String> kv = kvMap != null ? Map.copyOf(kvMap) : Map.of();
        manager.buffer(new BootstrapLogEvent(Instant.now(), level, message, loggerName, throwable, kv));
    }

    private void ensureKv() {
        if (kvMap == null) {
            kvMap = new LinkedHashMap<>(4);
        }
    }
}
