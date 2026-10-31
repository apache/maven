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

import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A JUL {@link Handler} that routes {@code java.util.logging} events into
 * Maven's structured logging pipeline, preserving the rich {@link LogRecord}
 * metadata that the standard {@code SLF4JBridgeHandler} silently drops
 * (source class name, source method name, thread ID).
 * <p>
 * When the underlying SLF4J logger is a {@link MavenSimpleLogger} (the
 * normal case inside Maven), events are delivered via
 * {@link MavenSimpleLogger#logDirect} — metadata flows as method
 * parameters instead of through ThreadLocals.  For non-Maven SLF4J
 * backends the handler falls back to standard {@code logger.info()} etc.
 * <p>
 * Usage — replace the standard SLF4J bridge in {@code LookupInvoker}:
 * <pre>
 *     MavenJulHandler.install();
 * </pre>
 *
 * @since 4.1.0
 * @see #install()
 */
public class MavenJulHandler extends Handler {

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
     * <p>
     * The cache is intentionally unbounded: its size is naturally bounded
     * by the number of distinct JUL logger names observed across all builds
     * in a JVM lifetime, which is small in practice (a few dozen at most).
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
     * Package-private test hook: sets or clears the re-entrancy flag on
     * the current thread without reflection.
     *
     * @param inPublish {@code true} to simulate being inside {@link #publish},
     *                  {@code false} to clear the flag
     */
    static void setInPublishForTest(boolean inPublish) {
        if (inPublish) {
            IN_PUBLISH.set(Boolean.TRUE);
        } else {
            IN_PUBLISH.remove();
        }
    }

    /**
     * Installs this handler on the JUL root logger, removing any
     * previously installed handlers.  This replaces the standard
     * {@code SLF4JBridgeHandler.install()} call.
     */
    public static void install() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
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

        if (!isLoggable(record)) {
            return;
        }

        // Re-entrancy guard: drop recursive JUL events that originate from
        // within SLF4J/JLine processing triggered by this very publish() call.
        if (IN_PUBLISH.get() != null) {
            return;
        }

        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "";
        }

        org.slf4j.Logger slf4jLogger = LOGGER_CACHE.get(loggerName);
        if (slf4jLogger == null) {
            try {
                slf4jLogger = LoggerFactory.getLogger(loggerName);
            } catch (IllegalStateException e) {
                return;
            }
            LOGGER_CACHE.putIfAbsent(loggerName, slf4jLogger);
        }
        int slf4jLevel = julLevelToSlf4j(record.getLevel());

        if (!isLevelEnabled(slf4jLogger, slf4jLevel)) {
            return;
        }

        String message = formatMessage(record);
        Throwable throwable = record.getThrown();

        IN_PUBLISH.set(Boolean.TRUE);
        try {
            if (slf4jLogger instanceof MavenSimpleLogger msl) {
                MavenSimpleLogger.SourceMetadata metadata = MavenSimpleLogger.isMetadataEnabled()
                        ? new MavenSimpleLogger.SourceMetadata(
                                record.getSourceClassName(), record.getSourceMethodName(), record.getLongThreadID())
                        : null;
                msl.logDirect(slf4jLevel, message, throwable, metadata);
            } else {
                logToSlf4j(slf4jLogger, slf4jLevel, message, throwable);
            }
        } finally {
            IN_PUBLISH.remove();
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
