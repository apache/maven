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

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.Constants;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.Logger;
import org.apache.maven.api.logging.LoggerManager;

/**
 * Default {@link LoggerManager} implementation. Owns:
 * <ul>
 *   <li>Per-logger level configuration (loaded from properties files)</li>
 *   <li>The active {@link LogSink}</li>
 *   <li>Severity tracking for {@code --fail-on-severity}</li>
 * </ul>
 * <p>
 * This class has no SLF4J dependency. The SLF4J adapter layer creates
 * an instance and registers it via {@link LoggerManager#set(LoggerManager)}.
 */
public class DefaultLoggerManager implements LoggerManager {

    private static final String CONFIGURATION_FILE = "maven.logger.properties";

    @Deprecated(since = "4.0.0")
    private static final String LEGACY_CONFIGURATION_FILE = "simplelogger.properties";

    private static final String LEGACY_PREFIX = "org.slf4j.simpleLogger.";

    public static final int LOG_LEVEL_TRACE = 0;
    public static final int LOG_LEVEL_DEBUG = 10;
    public static final int LOG_LEVEL_INFO = 20;
    public static final int LOG_LEVEL_WARN = 30;
    public static final int LOG_LEVEL_ERROR = 40;
    public static final int LOG_LEVEL_OFF = 50;

    private volatile LogSink logSink;
    private volatile int defaultLogLevel = LOG_LEVEL_INFO;
    private final AtomicReference<LogLevel> maxReached = new AtomicReference<>(null);
    private final Properties properties = new Properties();
    private final ConcurrentHashMap<String, Integer> levelCache = new ConcurrentHashMap<>();

    public DefaultLoggerManager() {
        this.logSink = new AccumulatingLogSink();
        LoggerManager.set(this);
        loadConfiguration();
    }

    // -- LogManager: loggers --

    @Override
    public Logger getLogger(String name) {
        return new DefaultLogger(name);
    }

    // -- LogManager: sink --

    @Override
    public LogSink getLogSink() {
        return logSink;
    }

    @Override
    public void setLogSink(LogSink sink) {
        LogSink old = this.logSink;
        // Drain BEFORE making the new sink visible to concurrent doLog() callers.
        // If we swapped first, a concurrent logger could write directly to the new sink
        // while drainTo() is still replaying older buffered events — breaking ordering.
        if (old instanceof AccumulatingLogSink acc && sink != null) {
            acc.drainTo(sink);
        }
        this.logSink = sink;
    }

    /**
     * A sink that buffers events until a real sink is installed.
     * Used as the default during early bootstrap.
     */
    private static class AccumulatingLogSink implements LogSink {
        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void accept(LogEvent event) {
            events.add(event);
        }

        void drainTo(LogSink target) {
            for (LogEvent event : events) {
                target.accept(event);
            }
            events.clear();
        }
    }

    // -- LogManager: level checking --

    @Override
    public boolean isLevelEnabled(String loggerName, LogLevel level) {
        int configuredLevel = getLoggerLevel(loggerName);
        return toIntLevel(level) >= configuredLevel;
    }

    @Override
    public void setRootLoggerLevel(LogLevel level) {
        String value =
                switch (level) {
                    case TRACE -> "trace";
                    case DEBUG -> "debug";
                    case INFO -> "info";
                    case WARN -> "warn";
                    case ERROR -> "error";
                };
        System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, value);
        reconfigure();
    }

    @Override
    public void reconfigure() {
        loadConfiguration();
        maxReached.set(null);
    }

    // -- LogManager: severity tracking --

    @Override
    public boolean hasReached(LogLevel level) {
        LogLevel reached = maxReached.get();
        return reached != null && reached.ordinal() >= level.ordinal();
    }

    public void record(LogLevel level) {
        while (true) {
            LogLevel r = maxReached.get();
            if (r == null || level.ordinal() > r.ordinal()) {
                if (!maxReached.compareAndSet(r, level)) {
                    continue;
                }
            }
            break;
        }
    }

    // -- Level resolution --

    /**
     * Returns the effective int log level for a logger name,
     * walking the name hierarchy (e.g. "com.example.Foo" → "com.example" → "com").
     * Results are cached until {@link #reconfigure()} is called.
     */
    public int getLoggerLevel(String loggerName) {
        if (loggerName != null) {
            return levelCache.computeIfAbsent(loggerName, this::computeLoggerLevel);
        }
        return defaultLogLevel;
    }

    private int computeLoggerLevel(String name) {
        String levelString = recursivelyComputeLevelString(name);
        return levelString != null ? stringToLevel(levelString) : defaultLogLevel;
    }

    public int getDefaultLogLevel() {
        return defaultLogLevel;
    }

    private String recursivelyComputeLevelString(String name) {
        String tempName = name;
        String levelString = null;
        int indexOfLastDot = tempName.length();
        while ((levelString == null) && (indexOfLastDot > -1)) {
            tempName = tempName.substring(0, indexOfLastDot);
            levelString = getStringProperty(Constants.MAVEN_LOGGER_LOG_PREFIX + tempName, null);
            indexOfLastDot = tempName.lastIndexOf(".");
        }
        return levelString;
    }

    // -- Configuration loading --

    private void loadConfiguration() {
        levelCache.clear();
        properties.clear();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }

        try (InputStream in = cl.getResourceAsStream(CONFIGURATION_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (java.io.IOException e) {
            // ignored
        }

        try (InputStream in = cl.getResourceAsStream(LEGACY_CONFIGURATION_FILE)) {
            if (in != null) {
                Properties legacyProps = new Properties();
                legacyProps.load(in);
                for (String propName : legacyProps.stringPropertyNames()) {
                    String mavenKey = propName.replace(LEGACY_PREFIX, Constants.MAVEN_LOGGER_PREFIX);
                    if (!properties.containsKey(mavenKey)) {
                        properties.setProperty(mavenKey, legacyProps.getProperty(propName));
                    }
                }
            }
        } catch (java.io.IOException e) {
            // ignored
        }

        String defaultLogLevelString = getStringProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, null);
        if (defaultLogLevelString != null) {
            defaultLogLevel = stringToLevel(defaultLogLevelString);
        } else {
            defaultLogLevel = LOG_LEVEL_INFO;
        }
    }

    // -- Property helpers --

    public String getStringProperty(String name, String defaultValue) {
        String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
    }

    public String getStringProperty(String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
            if (prop == null && name.startsWith(Constants.MAVEN_LOGGER_PREFIX)) {
                String legacyName = name.replace(Constants.MAVEN_LOGGER_PREFIX, LEGACY_PREFIX);
                prop = System.getProperty(legacyName);
            }
        } catch (SecurityException e) {
            // ignored
        }

        if (prop == null) {
            prop = properties.getProperty(name);
            if (prop == null && name.startsWith(Constants.MAVEN_LOGGER_PREFIX)) {
                String legacyName = name.replace(Constants.MAVEN_LOGGER_PREFIX, LEGACY_PREFIX);
                prop = properties.getProperty(legacyName);
            }
        }
        return prop;
    }

    public static int stringToLevel(String levelStr) {
        if ("trace".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_TRACE;
        } else if ("debug".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_DEBUG;
        } else if ("info".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_INFO;
        } else if ("warn".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_WARN;
        } else if ("error".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_ERROR;
        } else if ("off".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_OFF;
        }
        return LOG_LEVEL_INFO;
    }

    public static int toIntLevel(LogLevel level) {
        return switch (level) {
            case TRACE -> LOG_LEVEL_TRACE;
            case DEBUG -> LOG_LEVEL_DEBUG;
            case INFO -> LOG_LEVEL_INFO;
            case WARN -> LOG_LEVEL_WARN;
            case ERROR -> LOG_LEVEL_ERROR;
        };
    }
}
