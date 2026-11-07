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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Bootstrap {@link LoggerManager} that buffers log events until the real
 * implementation is installed via {@link LoggerManager#set(LoggerManager)}.
 * <p>
 * All levels are enabled so that early log messages are never lost.
 * After the real manager is installed, any cached {@link BufferingLogger}
 * instances dynamically resolve the current manager for level checks
 * and sink dispatch, so they continue to work correctly.
 */
final class BufferingLoggerManager implements LoggerManager {

    private final ConcurrentLinkedQueue<LogEvent> buffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private volatile LogSink customSink;

    @Override
    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, BufferingLogger::new);
    }

    @Override
    public LogSink getLogSink() {
        LogSink s = customSink;
        return s != null ? s : buffer::add;
    }

    @Override
    public void setLogSink(LogSink sink) {
        this.customSink = sink;
    }

    @Override
    public boolean isLevelEnabled(String loggerName, LogLevel level) {
        return true;
    }

    @Override
    public void setRootLoggerLevel(LogLevel level) {}

    @Override
    public void reconfigure() {}

    @Override
    public boolean hasReached(LogLevel level) {
        return false;
    }

    public void drainTo(LogSink sink) {
        LogEvent event;
        while ((event = buffer.poll()) != null) {
            sink.accept(event);
        }
    }

    long nextSequence() {
        return sequence.getAndIncrement();
    }

    /**
     * Logger that dynamically resolves the current {@link LoggerManager}
     * for level checks and sink dispatch. During the buffering phase,
     * all levels are enabled and events go to the accumulating queue.
     * After the real manager is installed, events go directly to the
     * real sink.
     */
    @SuppressWarnings("deprecation")
    static final class BufferingLogger implements Logger {
        private final String name;

        BufferingLogger(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isEnabled(LogLevel level) {
            return LoggerManager.get().isLevelEnabled(name, level);
        }

        @Override
        public LogEventBuilder atTrace() {
            return at(LogLevel.TRACE);
        }

        @Override
        public LogEventBuilder atDebug() {
            return at(LogLevel.DEBUG);
        }

        @Override
        public LogEventBuilder atInfo() {
            return at(LogLevel.INFO);
        }

        @Override
        public LogEventBuilder atWarn() {
            return at(LogLevel.WARN);
        }

        @Override
        public LogEventBuilder atError() {
            return at(LogLevel.ERROR);
        }

        @Override
        public LogEventBuilder at(LogLevel level) {
            if (!LoggerManager.get().isLevelEnabled(name, level)) {
                return BufferingLogEventBuilder.NOP;
            }
            return new BufferingLogEventBuilder(name, level);
        }

        @Override
        public Logger child(String childName) {
            return new BufferingLogger(name + "." + childName);
        }
    }

    /**
     * Event builder used during the buffering phase. Creates simple
     * {@link SimpleLogEvent} records and dispatches them to the current
     * manager's {@link LogSink}.
     */
    @SuppressWarnings("deprecation")
    static class BufferingLogEventBuilder implements LogEventBuilder {

        static final BufferingLogEventBuilder NOP = new BufferingLogEventBuilder(null, null) {
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

        private final String loggerName;
        private final LogLevel level;
        private Throwable throwable;

        BufferingLogEventBuilder(String loggerName, LogLevel level) {
            this.loggerName = loggerName;
            this.level = level;
        }

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
            dispatch(message);
        }

        @Override
        public void log(String format, Object arg) {
            dispatch(format, new Object[] {arg});
        }

        @Override
        public void log(String format, Object arg1, Object arg2) {
            dispatch(format, new Object[] {arg1, arg2});
        }

        @Override
        public void log(String format, Object... args) {
            dispatch(format, args);
        }

        @Override
        public void log(Supplier<String> messageSupplier) {
            dispatch(messageSupplier.get(), null);
        }

        private void dispatch(String message) {
            dispatch(message, null);
        }

        private void dispatch(String template, Object[] args) {
            LoggerManager mgr = LoggerManager.get();
            LogSink sink = mgr.getLogSink();
            if (sink != null) {
                long seq = mgr instanceof BufferingLoggerManager b ? b.nextSequence() : -1;
                String message = formatMessage(template, args);
                sink.accept(new SimpleLogEvent(
                        Instant.now(),
                        level,
                        message,
                        loggerName,
                        throwable,
                        Thread.currentThread().getId(),
                        Thread.currentThread().getName(),
                        seq));
            }
        }

        static String formatMessage(String template, Object[] args) {
            if (template == null) {
                return "";
            }
            if (args == null || args.length == 0) {
                return template;
            }
            // Direct positional substitution of {0}, {1}, ... — deliberately avoids
            // MessageFormat.format() which treats apostrophes as escape characters.
            StringBuilder sb = new StringBuilder(template.length() + 32);
            int len = template.length();
            int i = 0;
            while (i < len) {
                char c = template.charAt(i);
                if (c == '{' && i + 1 < len) {
                    int j = i + 1;
                    int idx = 0;
                    boolean hasDigits = false;
                    while (j < len && template.charAt(j) >= '0' && template.charAt(j) <= '9') {
                        idx = idx * 10 + (template.charAt(j) - '0');
                        j++;
                        hasDigits = true;
                    }
                    if (hasDigits && j < len && template.charAt(j) == '}') {
                        if (idx < args.length) {
                            sb.append(args[idx]);
                        } else {
                            sb.append(template, i, j + 1);
                        }
                        i = j + 1;
                        continue;
                    }
                }
                sb.append(c);
                i++;
            }
            return sb.toString();
        }
    }

    record SimpleLogEvent(
            Instant timestamp,
            LogLevel level,
            String message,
            String loggerName,
            Throwable throwable,
            long threadId,
            String threadName,
            long sequenceNumber)
            implements LogEvent {}
}
