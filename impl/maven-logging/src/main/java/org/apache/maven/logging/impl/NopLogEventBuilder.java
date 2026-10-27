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

import java.util.function.Supplier;

import org.apache.maven.api.logging.LogEventBuilder;

/**
 * A no-operation {@link LogEventBuilder} returned by {@link DefaultLogger} when the
 * target level is disabled. All methods are no-ops and no objects are allocated
 * beyond the singleton reference itself.
 *
 * @since 4.1.0
 */
final class NopLogEventBuilder implements LogEventBuilder {

    static final NopLogEventBuilder INSTANCE = new NopLogEventBuilder();

    private NopLogEventBuilder() {}

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
}
