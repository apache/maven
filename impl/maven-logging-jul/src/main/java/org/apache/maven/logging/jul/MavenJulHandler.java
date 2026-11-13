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

import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
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
 * @since 4.2.0
 * @see #install()
 */
public class MavenJulHandler extends Handler {

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
     * System property that controls whether existing JUL root-logger handlers are
     * removed before {@link MavenJulHandler} is added.
     * <p>
     * When set to {@code false}, Maven's handler is added alongside any handlers
     * already installed on the root logger.  This is useful for embedders (IDE
     * plug-ins, build tools running Maven in-process) that have already configured
     * their own JUL handler chain and do not want it replaced for the duration of
     * the Maven invocation.
     * <p>
     * The default value is {@code true}, which preserves the classic Maven
     * behaviour of removing the JDK's default {@code ConsoleHandler} to avoid
     * duplicate log output on the console.
     *
     * @since 4.2.0
     */
    public static final String REPLACE_HANDLERS_PROPERTY = "maven.logging.jul.replaceHandlers";

    /**
     * Installs this handler on the JUL root logger.
     * <p>
     * By default, any previously installed handlers are removed first so that
     * Maven's structured pipeline is the sole destination for JUL events (and the
     * JDK's default {@code ConsoleHandler} does not produce duplicate output).
     * Set the system property {@value #REPLACE_HANDLERS_PROPERTY} to {@code false}
     * to add this handler alongside existing ones instead.
     */
    public static void install() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        if (Boolean.parseBoolean(System.getProperty(REPLACE_HANDLERS_PROPERTY, "true"))) {
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
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

            String messageTemplate = resolveTemplate(record);
            Object[] messageArgs = record.getParameters();
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

            String pluginId = resolvePluginId(mgr);

            LogEvent event = new DefaultLogEvent(
                    MonotonicClock.now(),
                    level,
                    null,
                    loggerName,
                    throwable,
                    sourceClass,
                    sourceMethod,
                    threadId,
                    mgr instanceof DefaultLoggerManager dlm ? dlm.nextSequenceNumber() : -1,
                    threadName,
                    null,
                    null,
                    pluginId,
                    null,
                    messageTemplate,
                    messageArgs != null ? Arrays.asList(messageArgs) : null);

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

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static String resolvePluginId(LoggerManager mgr) {
        // Walk the call stack to find the first frame whose classloader is a registered
        // plugin classloader — matching the attribution strategy used by MavenLoggerFactory
        // for SLF4J loggers so both JUL and SLF4J events are attributed consistently.
        return STACK_WALKER.walk(
                frames -> frames.map(f -> mgr.getPluginId(f.getDeclaringClass().getClassLoader()))
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

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

    private static String resolveTemplate(LogRecord record) {
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
        return message;
    }
}
