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
package org.apache.maven.logging.jul;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLogEvent;
import org.apache.maven.logging.impl.DefaultLoggerManager;

/**
 * A JUL {@link Handler} that routes {@code java.util.logging} events into
 * Maven's structured logging pipeline via {@link LoggerManager}, preserving
 * the rich {@link LogRecord} metadata (source class name, source method name,
 * thread ID) that the standard {@code SLF4JBridgeHandler} silently drops.
 * <p>
 * This handler has no SLF4J dependency — events go directly through
 * {@link LoggerManager#get()} for level checking and sink dispatch.
 *
 * @since 4.1.0
 * @see #install()
 */
public class MavenJulHandler extends Handler {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    /**
     * Re-entrancy guard: set to {@code true} while {@link #publish} is routing
     * a JUL event through the sink on this thread.  Prevents recursive JUL events
     * from re-entering {@code publish} and causing infinite loops.
     */
    private static final ThreadLocal<Boolean> IN_PUBLISH = new ThreadLocal<>();

    static void setInPublishForTest(boolean inPublish) {
        if (inPublish) {
            IN_PUBLISH.set(Boolean.TRUE);
        } else {
            IN_PUBLISH.remove();
        }
    }

    /**
     * Installs this handler on the JUL root logger, removing any
     * previously installed handlers.
     */
    public static void install() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        rootLogger.addHandler(new MavenJulHandler());
    }

    /**
     * Returns {@code true} if a {@code MavenJulHandler} is installed
     * on the JUL root logger.
     */
    public static boolean isInstalled() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof MavenJulHandler) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation") // LogRecord.getThreadID()
    public void publish(LogRecord record) {
        if (record == null || !isLoggable(record)) {
            return;
        }

        if (IN_PUBLISH.get() != null) {
            return;
        }

        LoggerManager mgr = LoggerManager.get();
        if (mgr == null) {
            return;
        }

        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "";
        }

        LogLevel level = julLevelToLogLevel(record.getLevel());
        if (!mgr.isLevelEnabled(loggerName, level)) {
            return;
        }

        IN_PUBLISH.set(Boolean.TRUE);
        try {
            LogSink sink = mgr.getLogSink();
            if (sink == null) {
                return;
            }

            String message = formatMessage(record);
            Throwable throwable = record.getThrown();

            String sourceClass = null;
            String sourceMethod = null;
            long threadId = -1;
            String threadName = null;

            if (sink.needsMetadata()) {
                sourceClass = record.getSourceClassName();
                sourceMethod = record.getSourceMethodName();
                threadId = record.getLongThreadID();
                threadName = Thread.currentThread().getName();
            }

            String stackTrace = throwable != null ? formatStackTrace(throwable) : null;

            LogEvent event = new DefaultLogEvent(
                    MonotonicClock.now(),
                    level,
                    message,
                    loggerName,
                    stackTrace,
                    throwable,
                    sourceClass,
                    sourceMethod,
                    threadId,
                    SEQUENCE.getAndIncrement(),
                    threadName,
                    null,
                    null);

            sink.accept(event);

            if (mgr instanceof DefaultLoggerManager dlm) {
                dlm.record(level);
            }
        } finally {
            IN_PUBLISH.remove();
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}

    static LogLevel julLevelToLogLevel(Level julLevel) {
        int value = julLevel.intValue();
        if (value <= Level.FINEST.intValue()) {
            return LogLevel.TRACE;
        } else if (value <= Level.FINE.intValue()) {
            return LogLevel.DEBUG;
        } else if (value <= Level.INFO.intValue()) {
            return LogLevel.INFO;
        } else if (value <= Level.WARNING.intValue()) {
            return LogLevel.WARN;
        } else {
            return LogLevel.ERROR;
        }
    }

    private static String formatMessage(LogRecord record) {
        String message = record.getMessage();
        if (message == null) {
            return "";
        }

        ResourceBundle bundle = record.getResourceBundle();
        if (bundle != null) {
            try {
                message = bundle.getString(message);
            } catch (MissingResourceException e) {
                // use raw message
            }
        }

        Object[] params = record.getParameters();
        if (params != null && params.length > 0) {
            try {
                message = MessageFormat.format(message, params);
            } catch (IllegalArgumentException e) {
                // use message as-is if formatting fails
            }
        }

        return message;
    }

    private static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
