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

import java.util.function.Supplier;

/**
 * No-op {@link Logger} returned by {@link NopLoggerManager}.
 * All methods are no-ops; all levels are disabled.
 */
final class NopLogger implements Logger {

    private static final LogEventBuilder NOP_BUILDER = new LogEventBuilder() {
        @Override
        public LogEventBuilder withKeyValue(String key, String value) {
            return this;
        }

        @Override
        public LogEventBuilder withKeyValue(String key, long value) {
            return this;
        }

        @Override
        public LogEventBuilder withKeyValue(String key, boolean value) {
            return this;
        }

        @Override
        public LogEventBuilder withThrowable(Throwable t) {
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
        public void log(String message) {}

        @Override
        public void log(String format, Object arg) {}

        @Override
        public void log(String format, Object arg1, Object arg2) {}

        @Override
        public void log(String format, Object... args) {}

        @Override
        public void log(Supplier<String> messageSupplier) {}
    };

    private final String name;

    NopLogger(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        return false;
    }

    @Override
    public LogEventBuilder atTrace() {
        return NOP_BUILDER;
    }

    @Override
    public LogEventBuilder atDebug() {
        return NOP_BUILDER;
    }

    @Override
    public LogEventBuilder atInfo() {
        return NOP_BUILDER;
    }

    @Override
    public LogEventBuilder atWarn() {
        return NOP_BUILDER;
    }

    @Override
    public LogEventBuilder atError() {
        return NOP_BUILDER;
    }

    @Override
    public LogEventBuilder at(LogLevel level) {
        return NOP_BUILDER;
    }

    @Override
    public Logger child(String childName) {
        return new NopLogger(name + "." + childName);
    }
}
