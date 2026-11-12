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
package org.apache.maven.logging.slf4j;

import java.util.Arrays;
import java.util.Map;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLogEvent;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Thin SLF4J logger that dispatches events to the Maven unified logging API
 * via {@link LoggerManager}. Level checks are delegated to
 * {@link DefaultLoggerManager}; formatting is deferred to the {@link LogSink}.
 *
 * @since 4.2.0
 */
public class MavenSimpleLogger extends LegacyAbstractLogger {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance();
    final String pluginId;
    volatile int currentLogLevel = LocationAwareLogger.INFO_INT;

    MavenSimpleLogger(String name) {
        this(name, null);
    }

    MavenSimpleLogger(String name, String pluginId) {
        this.name = name;
        this.pluginId = pluginId;
        LoggerManager mgr = LoggerManager.get();
        if (mgr instanceof DefaultLoggerManager dlm) {
            this.currentLogLevel = dlm.getLoggerLevel(name);
        }
    }

    @Override
    protected void handleNormalizedLoggingCall(
            Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        int intLevel = level.toInt();
        if (!isLevelEnabled(intLevel)) {
            return;
        }

        String mavenTemplate = Slf4jTemplateConverter.convert(messagePattern);
        Object[] cleanArgs = throwable != null
                        && arguments != null
                        && arguments.length > 0
                        && arguments[arguments.length - 1] == throwable
                ? Arrays.copyOf(arguments, arguments.length - 1)
                : arguments;

        LoggerManager mgr = LoggerManager.get();
        LogSink sink = mgr.getLogSink();
        if (sink != null) {
            LogLevel logLevel = toLogLevel(intLevel);
            DefaultLoggerManager dlm = mgr instanceof DefaultLoggerManager d ? d : null;

            String sourceClass = null;
            String sourceMethod = null;
            if (sink.needsMetadata()) {
                StackWalker.StackFrame caller = STACK_WALKER.walk(
                        frames -> frames.dropWhile(f -> f.getClassName().startsWith("org.slf4j.")
                                        || f.getClassName().startsWith("org.apache.maven.logging.slf4j."))
                                .findFirst()
                                .orElse(null));
                if (caller != null) {
                    sourceClass = caller.getClassName();
                    sourceMethod = caller.getMethodName();
                }
            }

            sink.accept(new DefaultLogEvent(
                    MonotonicClock.now(),
                    logLevel,
                    null,
                    name,
                    throwable,
                    sourceClass,
                    sourceMethod,
                    Thread.currentThread().getId(),
                    dlm != null ? dlm.nextSequenceNumber() : -1,
                    Thread.currentThread().getName(),
                    null,
                    null,
                    pluginId,
                    Map.of(),
                    mavenTemplate,
                    cleanArgs != null ? Arrays.asList(cleanArgs) : null));
            if (dlm != null) {
                dlm.record(logLevel);
            }
        }
    }

    public void configure(int defaultLogLevel) {
        LoggerManager mgr = LoggerManager.get();
        if (mgr instanceof DefaultLoggerManager dlm) {
            this.currentLogLevel = dlm.getLoggerLevel(name);
        } else {
            this.currentLogLevel = defaultLogLevel;
        }
    }

    public void setLogLevel(int logLevel) {
        this.currentLogLevel = logLevel;
    }

    boolean isLevelEnabled(int logLevel) {
        return logLevel >= currentLogLevel;
    }

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(LocationAwareLogger.TRACE_INT);
    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(LocationAwareLogger.DEBUG_INT);
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(LocationAwareLogger.INFO_INT);
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(LocationAwareLogger.WARN_INT);
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(LocationAwareLogger.ERROR_INT);
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    static LogLevel toLogLevel(int level) {
        return switch (level) {
            case LocationAwareLogger.TRACE_INT -> LogLevel.TRACE;
            case LocationAwareLogger.DEBUG_INT -> LogLevel.DEBUG;
            case LocationAwareLogger.INFO_INT -> LogLevel.INFO;
            case LocationAwareLogger.WARN_INT -> LogLevel.WARN;
            default -> LogLevel.ERROR;
        };
    }
}
