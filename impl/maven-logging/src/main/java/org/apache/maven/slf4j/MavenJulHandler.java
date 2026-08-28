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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.maven.api.services.MessageBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import static org.apache.maven.jline.MessageUtils.builder;

/**
 * A JUL {@link Handler} that routes {@code java.util.logging} events into
 * Maven's structured logging pipeline, preserving the rich {@link LogRecord}
 * metadata that the standard {@code SLF4JBridgeHandler} silently drops
 * (source class name, source method name, thread ID).
 * <p>
 * When a {@link MavenSimpleLogger.LogSink LogSink} is installed (i.e. during
 * a build), JUL events are sent directly to the sink — bypassing SLF4J
 * entirely.  The JUL metadata is stashed in a thread-local so downstream
 * consumers (e.g. {@code ProjectBuildLogAppender}) can read it when
 * constructing a structured {@code LogEvent}.
 * <p>
 * When no LogSink is installed (e.g. during early bootstrap), the handler
 * falls back to routing through SLF4J for console output.
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
     * Private SLF4J logger cache using {@link ConcurrentMap#putIfAbsent}
     * instead of {@link ConcurrentMap#computeIfAbsent}.  This avoids the
     * {@code ConcurrentHashMap.computeIfAbsent} reentrancy bug
     * ({@code IllegalStateException("Recursive update")}) that occurs
     * when a JUL event fires during SLF4J logger initialization: the
     * handler's {@code publish()} calls {@code LoggerFactory.getLogger()},
     * which internally uses {@code computeIfAbsent}, and if that triggers
     * another JUL event whose logger name hashes to the same bucket,
     * {@code ConcurrentHashMap} throws.  {@code putIfAbsent} is safe
     * against reentrancy — worst case, two threads create the same
     * logger and one is discarded.
     */
    private static final ConcurrentMap<String, org.slf4j.Logger> LOGGER_CACHE = new ConcurrentHashMap<>();

    /**
     * Re-entrancy guard: set to {@code true} while {@link #publish} is routing
     * a JUL event through SLF4J on this thread.  Prevents recursive JUL events
     * (e.g. JLine's {@code StyleResolver} calling {@code java.util.logging.Logger}
     * while inside {@link MavenSimpleLogger#renderLevel} lazy-initialisation,
     * which in turn is triggered by a JUL event during terminal construction)
     * from re-entering {@code publish} and crashing with
     * {@code ConcurrentHashMap.computeIfAbsent IllegalStateException("Recursive update")}.
     */
    private static final ThreadLocal<Boolean> IN_PUBLISH = new ThreadLocal<>();

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
        // Note: we intentionally do NOT set rootLogger.setLevel(Level.ALL)
        // here.  Setting it eagerly floods JUL events during SLF4J bootstrap,
        // triggering ConcurrentHashMap.computeIfAbsent reentrancy in the
        // SLF4J logger factory ("Recursive update").  The JUL root default
        // (INFO) is fine — callers that need FINE/FINEST events (e.g. -X
        // debug mode) should set the JUL root level after SLF4J is fully
        // initialized.
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

        // Re-entrancy guard: drop recursive JUL events that originate from
        // within SLF4J/JLine processing triggered by this very publish() call.
        // Example: MavenSimpleLogger.renderLevel() lazily initialises ANSI
        // colour strings by calling JLine's StyleResolver, which logs DEBUG
        // events via java.util.logging — re-entering publish() on the same
        // thread and crashing ConcurrentHashMap.computeIfAbsent with
        // IllegalStateException("Recursive update").
        if (Boolean.TRUE.equals(IN_PUBLISH.get())) {
            return;
        }

        // Guard against null logger name (allowed by JUL spec)
        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "";
        }

        // Look up the SLF4J logger from our private cache, bypassing
        // LoggerFactory.getLogger() on the hot path to avoid the
        // ConcurrentHashMap.computeIfAbsent reentrancy problem.
        org.slf4j.Logger slf4jLogger = LOGGER_CACHE.get(loggerName);
        if (slf4jLogger == null) {
            // Cold path: create the logger via SLF4J.  Guard against
            // the ConcurrentHashMap.computeIfAbsent reentrancy bug:
            // LoggerFactory.getLogger() uses computeIfAbsent internally,
            // so if a JUL event fires during SLF4J initialization and
            // the logger name hashes to the same bucket, CHM throws
            // IllegalStateException("Recursive update").  We catch it
            // and silently drop the event — it's a bootstrap race, and
            // subsequent events will hit the cache.
            try {
                slf4jLogger = LoggerFactory.getLogger(loggerName);
            } catch (IllegalStateException e) {
                // ConcurrentHashMap reentrancy — drop this event
                return;
            }
            LOGGER_CACHE.putIfAbsent(loggerName, slf4jLogger);
        }
        int slf4jLevel = julLevelToSlf4j(record.getLevel());

        // Quick exit if this level is not enabled
        if (!isLevelEnabled(slf4jLogger, slf4jLevel)) {
            return;
        }

        String message = formatMessage(record);
        Throwable throwable = record.getThrown();

        // Set the re-entrancy guard before routing through SLF4J.  This prevents
        // recursive JUL events fired during SLF4J/JLine processing (e.g. from
        // MavenSimpleLogger.renderLevel() -> StyleResolver) from re-entering publish()
        // on the same thread.  The primary fix is in MavenStyleResolver.resolve() which
        // uses get+putIfAbsent instead of computeIfAbsent; this guard is defense-in-depth.
        IN_PUBLISH.set(Boolean.TRUE);
        // If a LogSink is installed, bypass SLF4J entirely: call the sink
        // directly with the JUL metadata so no information is lost in transit.
        MavenSimpleLogger.LogSink sink = MavenSimpleLogger.getLogSink();
        METADATA.set(
                new JulMetadata(record.getSourceClassName(), record.getSourceMethodName(), record.getLongThreadID()));
        try {
            if (sink != null) {
                String formatted = formatForConsole(slf4jLevel, message);
                sink.accept(slf4jLevel, loggerName, message, formatted, throwable);
            } else {
                // No LogSink — fall through to SLF4J for console output
                logToSlf4j(slf4jLogger, slf4jLevel, message, throwable);
            }
        } finally {
            IN_PUBLISH.remove();
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

    /**
     * Formats a JUL message for console output, matching the {@code [LEVEL] message}
     * style used by MavenSimpleLogger with ANSI coloring when available.
     */
    private static String formatForConsole(int level, String message) {
        MessageBuilder mb = builder();
        String levelStr =
                switch (level) {
                    case LocationAwareLogger.TRACE_INT -> mb.trace("TRACE").build();
                    case LocationAwareLogger.DEBUG_INT -> mb.debug("DEBUG").build();
                    case LocationAwareLogger.INFO_INT -> mb.info("INFO").build();
                    case LocationAwareLogger.WARN_INT -> mb.warning("WARNING").build();
                    default -> mb.error("ERROR").build();
                };
        return "[" + levelStr + "] " + message;
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
