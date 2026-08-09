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
package org.apache.maven.slf4j;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A JUL {@link Handler} that routes {@code java.util.logging} events into
 * Maven's structured logging pipeline, preserving the rich {@link LogRecord}
 * metadata that the standard {@code SLF4JBridgeHandler} silently drops
 * (source class name, source method name, thread ID).
 * <p>
 * All JUL events are routed through SLF4J so that {@link MavenSimpleLogger}
 * produces a consistent {@code formattedMessage} (with timestamp, logger name,
 * and ANSI styling) regardless of the event's origin.  The JUL metadata is
 * stashed in a thread-local <em>before</em> the SLF4J call so that downstream
 * consumers (e.g. {@code ProjectBuildLogAppender}) can read it when
 * constructing a structured {@code LogEvent}.
 * <p>
 * Usage — replace the standard SLF4J bridge in {@code LookupInvoker}:
 * <pre>
 *     MavenJulHandler.install();
 * </pre>
 *
 * @since 4.1.0
 * @see #install()
 * @see #getJulMetadata()
 */
public class MavenJulHandler extends Handler {

    /**
     * JUL metadata captured from a {@link LogRecord} that would otherwise
     * be lost when bridging to SLF4J.
     *
     * @param sourceClassName  the source class, or {@code null}
     * @param sourceMethodName the source method, or {@code null}
     * @param threadId         the originating thread ID
     */
    public record JulMetadata(String sourceClassName, String sourceMethodName, long threadId) {}

    private static final ThreadLocal<JulMetadata> METADATA = new ThreadLocal<>();

    /**
     * Returns the JUL metadata for the current log event being processed,
     * or {@code null} if the current log event did not originate from JUL.
     * <p>
     * This method is intended to be called from within a
     * {@link MavenSimpleLogger.LogSink} callback (e.g. in
     * {@code ProjectBuildLogAppender.accept()}).
     *
     * @return the current JUL metadata, or {@code null}
     */
    public static JulMetadata getJulMetadata() {
        return METADATA.get();
    }

    /**
     * Installs this handler on the JUL root logger, removing any
     * previously installed handlers.  This replaces the standard
     * {@code SLF4JBridgeHandler.install()} call.
     */
    public static void install() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        // Remove all existing handlers (including any SLF4JBridgeHandler)
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        rootLogger.addHandler(new MavenJulHandler());
        // Accept all levels — filtering is done by SLF4J
        rootLogger.setLevel(Level.ALL);
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
    public void publish(LogRecord record) {
        if (record == null) {
            return;
        }

        // Guard against null logger name (allowed by JUL spec)
        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "";
        }
        org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(loggerName);
        int slf4jLevel = julLevelToSlf4j(record.getLevel());

        // Quick exit if this level is not enabled
        if (!isLevelEnabled(slf4jLogger, slf4jLevel)) {
            return;
        }

        String message = formatMessage(record);
        Throwable throwable = record.getThrown();

        // Set the JUL metadata before routing through SLF4J so that
        // downstream consumers (e.g. ProjectBuildLogAppender) can read
        // it when constructing a structured LogEvent.  By always going
        // through SLF4J, the formattedMessage is produced by
        // MavenSimpleLogger (with proper timestamp, logger name, and
        // ANSI styling) regardless of whether the event originated from
        // JUL or SLF4J — fixing the format inconsistency.
        METADATA.set(
                new JulMetadata(record.getSourceClassName(), record.getSourceMethodName(), record.getLongThreadID()));
        try {
            logToSlf4j(slf4jLogger, slf4jLevel, message, throwable);
        } finally {
            METADATA.remove();
        }
    }

    @Override
    public void flush() {
        // nothing to flush
    }

    @Override
    public void close() throws SecurityException {
        // nothing to close
    }

    /**
     * Formats the log message, applying i18n resource bundle lookup and
     * {@link MessageFormat} parameter substitution, matching the behavior
     * of {@code SLF4JBridgeHandler}.
     */
    private static String formatMessage(LogRecord record) {
        String message = record.getMessage();
        if (message == null) {
            return "";
        }

        // Try resource bundle lookup
        ResourceBundle bundle = record.getResourceBundle();
        if (bundle != null) {
            try {
                message = bundle.getString(message);
            } catch (MissingResourceException e) {
                // use raw message
            }
        }

        // Apply MessageFormat parameters
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

    private static int julLevelToSlf4j(Level julLevel) {
        int value = julLevel.intValue();
        if (value <= Level.FINEST.intValue()) {
            return LocationAwareLogger.TRACE_INT;
        } else if (value <= Level.FINE.intValue()) {
            return LocationAwareLogger.DEBUG_INT;
        } else if (value <= Level.INFO.intValue()) {
            return LocationAwareLogger.INFO_INT;
        } else if (value <= Level.WARNING.intValue()) {
            return LocationAwareLogger.WARN_INT;
        } else {
            return LocationAwareLogger.ERROR_INT;
        }
    }

    private static boolean isLevelEnabled(org.slf4j.Logger logger, int level) {
        return switch (level) {
            case LocationAwareLogger.TRACE_INT -> logger.isTraceEnabled();
            case LocationAwareLogger.DEBUG_INT -> logger.isDebugEnabled();
            case LocationAwareLogger.INFO_INT -> logger.isInfoEnabled();
            case LocationAwareLogger.WARN_INT -> logger.isWarnEnabled();
            default -> logger.isErrorEnabled();
        };
    }

    private static void logToSlf4j(org.slf4j.Logger logger, int level, String message, Throwable throwable) {
        switch (level) {
            case LocationAwareLogger.TRACE_INT -> logger.trace(message, throwable);
            case LocationAwareLogger.DEBUG_INT -> logger.debug(message, throwable);
            case LocationAwareLogger.INFO_INT -> logger.info(message, throwable);
            case LocationAwareLogger.WARN_INT -> logger.warn(message, throwable);
            default -> logger.error(message, throwable);
        }
    }
}
