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
package org.apache.maven.logging.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.api.logging.LogEventBuilder;
import org.apache.maven.api.logging.LogLevel;

/**
 * Active {@link LogEventBuilder} returned by {@link DefaultLogger} when the target level is enabled.
 * <p>
 * Collects key-value pairs, throwable, and override context fields, then dispatches
 * the event via {@link DefaultLogger#doLog} on the terminal {@code log()} call.
 * Message formatting is deferred to the {@link DefaultLogEvent} — the raw template
 * and arguments are passed through so that sinks can access structured data.
 *
 * @since 4.1.0
 */
final class DefaultLogEventBuilder implements LogEventBuilder {

    private final DefaultLogger log;
    private final LogLevel level;

    private Map<String, String> kvMap;
    private Throwable throwable;

    DefaultLogEventBuilder(DefaultLogger log, LogLevel level) {
        this.log = log;
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
        log.doLog(level, message, null, null, kvMap, throwable);
    }

    @Override
    public void log(String format, Object arg) {
        log.doLog(level, null, format, new Object[] {arg}, kvMap, throwable);
    }

    @Override
    public void log(String format, Object arg1, Object arg2) {
        log.doLog(level, null, format, new Object[] {arg1, arg2}, kvMap, throwable);
    }

    @Override
    public void log(String format, Object... args) {
        log.doLog(level, null, format, args, kvMap, throwable);
    }

    @Override
    public void log(Supplier<String> messageSupplier) {
        log.doLog(level, messageSupplier.get(), null, null, kvMap, throwable);
    }

    private void ensureKv() {
        if (kvMap == null) {
            kvMap = new LinkedHashMap<>(4);
        }
    }
}
