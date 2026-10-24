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

import java.time.Instant;
import java.util.Map;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;

/**
 * Immutable implementation of {@link LogEvent} with lazy message formatting.
 * <p>
 * When constructed with a {@code messageTemplate} and {@code messageArgs},
 * the formatted {@link #message()} is computed lazily on first access.
 * When constructed with a pre-formatted message, it is returned as-is.
 *
 * @since 4.1.0
 */
public final class DefaultLogEvent implements LogEvent {

    private static final String PLACEHOLDER = "{}";

    private final Instant timestamp;
    private final LogLevel level;
    private final String message;
    private final String loggerName;
    private final String stackTrace;
    private final Throwable throwable;
    private final String sourceClassName;
    private final String sourceMethodName;
    private final long threadId;
    private final long sequenceNumber;
    private final String threadName;
    private final String projectId;
    private final String mojoId;
    private final Map<String, String> keyValues;
    private final String messageTemplate;
    private final Object[] messageArgs;

    private volatile String formattedMessage;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultLogEvent(
            Instant timestamp,
            LogLevel level,
            String message,
            String loggerName,
            String stackTrace,
            Throwable throwable,
            String sourceClassName,
            String sourceMethodName,
            long threadId,
            long sequenceNumber,
            String threadName,
            String projectId,
            String mojoId,
            Map<String, String> keyValues,
            String messageTemplate,
            Object[] messageArgs) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.stackTrace = stackTrace;
        this.throwable = throwable;
        this.sourceClassName = sourceClassName;
        this.sourceMethodName = sourceMethodName;
        this.threadId = threadId;
        this.sequenceNumber = sequenceNumber;
        this.threadName = threadName;
        this.projectId = projectId;
        this.mojoId = mojoId;
        this.keyValues = keyValues != null ? keyValues : Map.of();
        this.messageTemplate = messageTemplate;
        this.messageArgs = messageArgs;
    }

    /**
     * Convenience constructor for events without structured data.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultLogEvent(
            Instant timestamp,
            LogLevel level,
            String message,
            String loggerName,
            String stackTrace,
            Throwable throwable,
            String sourceClassName,
            String sourceMethodName,
            long threadId,
            long sequenceNumber,
            String threadName,
            String projectId,
            String mojoId) {
        this(
                timestamp,
                level,
                message,
                loggerName,
                stackTrace,
                throwable,
                sourceClassName,
                sourceMethodName,
                threadId,
                sequenceNumber,
                threadName,
                projectId,
                mojoId,
                Map.of(),
                null,
                null);
    }

    /**
     * Convenience constructor for SLF4J events (no source metadata).
     */
    public DefaultLogEvent(Instant timestamp, LogLevel level, String message, String loggerName, String stackTrace) {
        this(timestamp, level, message, loggerName, stackTrace, null, null, null, -1, -1, null, null, null);
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public LogLevel level() {
        return level;
    }

    @Override
    public String message() {
        if (message != null) {
            return message;
        }
        String m = formattedMessage;
        if (m == null) {
            m = formatMessage(messageTemplate, messageArgs);
            formattedMessage = m;
        }
        return m;
    }

    @Override
    public String loggerName() {
        return loggerName;
    }

    @Override
    public String stackTrace() {
        return stackTrace;
    }

    @Override
    public Throwable throwable() {
        return throwable;
    }

    @Override
    public String sourceClassName() {
        return sourceClassName;
    }

    @Override
    public String sourceMethodName() {
        return sourceMethodName;
    }

    @Override
    public long threadId() {
        return threadId;
    }

    @Override
    public long sequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String threadName() {
        return threadName;
    }

    @Override
    public String projectId() {
        return projectId;
    }

    @Override
    public String mojoId() {
        return mojoId;
    }

    @Override
    public Map<String, String> keyValues() {
        return keyValues;
    }

    @Override
    public String messageTemplate() {
        return messageTemplate;
    }

    @Override
    public Object[] messageArgs() {
        return messageArgs;
    }

    static String formatMessage(String template, Object[] args) {
        if (template == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder();
        int argIdx = 0;
        int start = 0;
        int pos;
        while ((pos = template.indexOf(PLACEHOLDER, start)) != -1 && argIdx < args.length) {
            sb.append(template, start, pos);
            sb.append(args[argIdx++]);
            start = pos + 2;
        }
        sb.append(template, start, template.length());
        return sb.toString();
    }
}
