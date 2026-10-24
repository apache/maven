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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.StackWalker.StackFrame;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogEventBuilder;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.Logger;
import org.apache.maven.api.logging.LoggerManager;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation of both {@link Logger} (new fluent API)
 * and the deprecated {@link org.apache.maven.api.plugin.Log} (backward compatibility).
 * <p>
 * Builds fully-structured {@link LogEvent} objects — including {@code projectId},
 * {@code mojoId}, timestamp, and sequence number — and dispatches them to the
 * current {@link LogSink} obtained from {@link LoggerManager#get()}.
 * Level checks and sink lookup both go through the {@code LoggerManager},
 * so the sink can be changed at runtime.
 *
 * @since 4.0.0
 */
@SuppressWarnings("deprecation")
public class DefaultLogger implements Logger, org.apache.maven.api.plugin.Log {

    private static final StackWalker WALKER = StackWalker.getInstance();
    private static final String THIS_CLASS = DefaultLogger.class.getName();
    private static final String BUILDER_CLASS = DefaultLogEventBuilder.class.getName();

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final String name;
    private final String projectId;
    private final String mojoId;

    public DefaultLogger(String name) {
        this(name, null, null);
    }

    public DefaultLogger(String name, String projectId, String mojoId) {
        this.name = requireNonNull(name, "name");
        this.projectId = projectId;
        this.mojoId = mojoId;
    }

    // ---- org.apache.maven.api.logging.Logger ----

    @Override
    public String name() {
        return name;
    }

    private boolean isLevelEnabled(LogLevel level) {
        return LoggerManager.get().isLevelEnabled(name, level);
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        return isLevelEnabled(level);
    }

    @Override
    public LogEventBuilder atTrace() {
        return isLevelEnabled(LogLevel.TRACE)
                ? new DefaultLogEventBuilder(this, LogLevel.TRACE)
                : NopLogEventBuilder.INSTANCE;
    }

    @Override
    public LogEventBuilder atDebug() {
        return isLevelEnabled(LogLevel.DEBUG)
                ? new DefaultLogEventBuilder(this, LogLevel.DEBUG)
                : NopLogEventBuilder.INSTANCE;
    }

    @Override
    public LogEventBuilder atInfo() {
        return isLevelEnabled(LogLevel.INFO)
                ? new DefaultLogEventBuilder(this, LogLevel.INFO)
                : NopLogEventBuilder.INSTANCE;
    }

    @Override
    public LogEventBuilder atWarn() {
        return isLevelEnabled(LogLevel.WARN)
                ? new DefaultLogEventBuilder(this, LogLevel.WARN)
                : NopLogEventBuilder.INSTANCE;
    }

    @Override
    public LogEventBuilder atError() {
        return isLevelEnabled(LogLevel.ERROR)
                ? new DefaultLogEventBuilder(this, LogLevel.ERROR)
                : NopLogEventBuilder.INSTANCE;
    }

    @Override
    public LogEventBuilder at(LogLevel level) {
        return isLevelEnabled(level) ? new DefaultLogEventBuilder(this, level) : NopLogEventBuilder.INSTANCE;
    }

    @Override
    public DefaultLogger child(String childName) {
        requireNonNull(childName, "childName");
        return new DefaultLogger(name + "." + childName, projectId, mojoId);
    }

    // ---- Internal dispatch ----

    void doLog(LogLevel level, String message, Throwable throwable) {
        doLog(level, message, null, null, null, throwable, null, null);
    }

    @SuppressWarnings("deprecation") // Thread.getId()
    void doLog(
            LogLevel level,
            String message,
            String messageTemplate,
            Object[] messageArgs,
            Map<String, String> keyValues,
            Throwable throwable) {
        doLog(level, message, messageTemplate, messageArgs, keyValues, throwable, null, null);
    }

    @SuppressWarnings({"deprecation", "checkstyle:ParameterNumber"}) // Thread.getId()
    void doLog(
            LogLevel level,
            String message,
            String messageTemplate,
            Object[] messageArgs,
            Map<String, String> keyValues,
            Throwable throwable,
            String projectIdOverride,
            String mojoIdOverride) {
        LoggerManager mgr = LoggerManager.get();
        LogSink sink = mgr.getLogSink();
        if (sink == null) {
            return;
        }

        Instant timestamp = MonotonicClock.now();
        long seq = SEQUENCE.getAndIncrement();

        String sourceClass = null;
        String sourceMethod = null;
        long threadId = -1;
        String threadName = null;

        if (sink.needsMetadata()) {
            sourceClass = name;
            sourceMethod = resolveCallerMethod();
            threadId = Thread.currentThread().getId();
            threadName = Thread.currentThread().getName();
        }

        String stackTrace = throwable != null ? formatStackTrace(throwable) : null;

        LogEvent event = new DefaultLogEvent(
                timestamp,
                level,
                message,
                name,
                stackTrace,
                throwable,
                sourceClass,
                sourceMethod,
                threadId,
                seq,
                threadName,
                projectIdOverride != null ? projectIdOverride : projectId,
                mojoIdOverride != null ? mojoIdOverride : mojoId,
                keyValues,
                messageTemplate,
                messageArgs);

        sink.accept(event);

        if (mgr instanceof DefaultLoggerManager dlm) {
            dlm.record(level);
        }
    }

    private String resolveCallerMethod() {
        return WALKER.walk(frames -> frames.dropWhile(
                        f -> THIS_CLASS.equals(f.getClassName()) || BUILDER_CLASS.equals(f.getClassName()))
                .findFirst()
                .map(StackFrame::getMethodName)
                .orElse(null));
    }

    private static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // ---- Deprecated org.apache.maven.api.plugin.Log (backward compat) ----

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(LogLevel.TRACE);
    }

    @Override
    public void trace(CharSequence content) {
        if (isTraceEnabled()) {
            doLog(LogLevel.TRACE, toString(content), null);
        }
    }

    @Override
    public void trace(CharSequence content, Throwable error) {
        if (isTraceEnabled()) {
            doLog(LogLevel.TRACE, toString(content), error);
        }
    }

    @Override
    public void trace(Throwable error) {
        if (isTraceEnabled()) {
            doLog(LogLevel.TRACE, "", error);
        }
    }

    @Override
    public void trace(Supplier<String> content) {
        if (isTraceEnabled()) {
            doLog(LogLevel.TRACE, content.get(), null);
        }
    }

    @Override
    public void trace(Supplier<String> content, Throwable error) {
        if (isTraceEnabled()) {
            doLog(LogLevel.TRACE, content.get(), error);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(LogLevel.DEBUG);
    }

    @Override
    public void debug(CharSequence content) {
        if (isDebugEnabled()) {
            doLog(LogLevel.DEBUG, toString(content), null);
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (isDebugEnabled()) {
            doLog(LogLevel.DEBUG, toString(content), error);
        }
    }

    @Override
    public void debug(Throwable error) {
        if (isDebugEnabled()) {
            doLog(LogLevel.DEBUG, "", error);
        }
    }

    @Override
    public void debug(Supplier<String> content) {
        if (isDebugEnabled()) {
            doLog(LogLevel.DEBUG, content.get(), null);
        }
    }

    @Override
    public void debug(Supplier<String> content, Throwable error) {
        if (isDebugEnabled()) {
            doLog(LogLevel.DEBUG, content.get(), error);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(LogLevel.INFO);
    }

    @Override
    public void info(CharSequence content) {
        if (isInfoEnabled()) {
            doLog(LogLevel.INFO, toString(content), null);
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (isInfoEnabled()) {
            doLog(LogLevel.INFO, toString(content), error);
        }
    }

    @Override
    public void info(Throwable error) {
        if (isInfoEnabled()) {
            doLog(LogLevel.INFO, "", error);
        }
    }

    @Override
    public void info(Supplier<String> content) {
        if (isInfoEnabled()) {
            doLog(LogLevel.INFO, content.get(), null);
        }
    }

    @Override
    public void info(Supplier<String> content, Throwable error) {
        if (isInfoEnabled()) {
            doLog(LogLevel.INFO, content.get(), error);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(LogLevel.WARN);
    }

    @Override
    public void warn(CharSequence content) {
        if (isWarnEnabled()) {
            doLog(LogLevel.WARN, toString(content), null);
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (isWarnEnabled()) {
            doLog(LogLevel.WARN, toString(content), error);
        }
    }

    @Override
    public void warn(Throwable error) {
        if (isWarnEnabled()) {
            doLog(LogLevel.WARN, "", error);
        }
    }

    @Override
    public void warn(Supplier<String> content) {
        if (isWarnEnabled()) {
            doLog(LogLevel.WARN, content.get(), null);
        }
    }

    @Override
    public void warn(Supplier<String> content, Throwable error) {
        if (isWarnEnabled()) {
            doLog(LogLevel.WARN, content.get(), error);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(LogLevel.ERROR);
    }

    @Override
    public void error(CharSequence content) {
        if (isErrorEnabled()) {
            doLog(LogLevel.ERROR, toString(content), null);
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (isErrorEnabled()) {
            doLog(LogLevel.ERROR, toString(content), error);
        }
    }

    @Override
    public void error(Throwable error) {
        if (isErrorEnabled()) {
            doLog(LogLevel.ERROR, "", error);
        }
    }

    @Override
    public void error(Supplier<String> content) {
        if (isErrorEnabled()) {
            doLog(LogLevel.ERROR, content.get(), null);
        }
    }

    @Override
    public void error(Supplier<String> content, Throwable error) {
        if (isErrorEnabled()) {
            doLog(LogLevel.ERROR, content.get(), error);
        }
    }

    private String toString(CharSequence content) {
        return content != null ? content.toString() : "";
    }
}
