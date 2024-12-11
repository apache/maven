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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.apache.maven.slf4j.OutputChoice.OutputChoiceType;
import org.slf4j.helpers.Reporter;

/**
 * This class holds configuration values for {@link MavenBaseLogger}. The
 * values are computed at runtime. See {@link MavenBaseLogger} documentation for
 * more information.
 *
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Scott Sanders
 * @author Rod Waldhoff
 * @author Robert Burrell Donkin
 * @author C&eacute;drik LIME
 *
 * @since 1.7.25
 */
public class SimpleLoggerConfiguration {

    private static final String CONFIGURATION_FILE = "simplelogger.properties";

    static final int DEFAULT_LOG_LEVEL_DEFAULT = MavenBaseLogger.LOG_LEVEL_INFO;
    int defaultLogLevel = DEFAULT_LOG_LEVEL_DEFAULT;

    private static final boolean SHOW_DATE_TIME_DEFAULT = false;
    boolean showDateTime = SHOW_DATE_TIME_DEFAULT;

    private static final String DATE_TIME_FORMAT_STR_DEFAULT = null;
    private static String dateTimeFormatStr = DATE_TIME_FORMAT_STR_DEFAULT;

    DateTimeFormatter dateFormatter = null;

    private static final boolean SHOW_THREAD_NAME_DEFAULT = true;
    boolean showThreadName = SHOW_THREAD_NAME_DEFAULT;

    /**
     * See https://jira.qos.ch/browse/SLF4J-499
     * @since 1.7.33 and 2.0.0-alpha6
     */
    private static final boolean SHOW_THREAD_ID_DEFAULT = false;

    boolean showThreadId = SHOW_THREAD_ID_DEFAULT;

    static final boolean SHOW_LOG_NAME_DEFAULT = true;
    boolean showLogName = SHOW_LOG_NAME_DEFAULT;

    private static final boolean SHOW_SHORT_LOG_NAME_DEFAULT = false;
    boolean showShortLogName = SHOW_SHORT_LOG_NAME_DEFAULT;

    private static final boolean LEVEL_IN_BRACKETS_DEFAULT = false;
    boolean levelInBrackets = LEVEL_IN_BRACKETS_DEFAULT;

    private static final String LOG_FILE_DEFAULT = "System.err";
    private String logFile = LOG_FILE_DEFAULT;
    OutputChoice outputChoice = null;

    private static final boolean CACHE_OUTPUT_STREAM_DEFAULT = false;
    private boolean cacheOutputStream = CACHE_OUTPUT_STREAM_DEFAULT;

    private static final String WARN_LEVELS_STRING_DEFAULT = "WARN";
    String warnLevelString = WARN_LEVELS_STRING_DEFAULT;

    private final Properties properties = new Properties();

    void init() {
        loadProperties();

        String defaultLogLevelString = getStringProperty(MavenBaseLogger.DEFAULT_LOG_LEVEL_KEY, null);
        if (defaultLogLevelString != null) {
            defaultLogLevel = stringToLevel(defaultLogLevelString);
        }

        showLogName =
                getBooleanProperty(MavenBaseLogger.SHOW_LOG_NAME_KEY, SimpleLoggerConfiguration.SHOW_LOG_NAME_DEFAULT);
        showShortLogName = getBooleanProperty(MavenBaseLogger.SHOW_SHORT_LOG_NAME_KEY, SHOW_SHORT_LOG_NAME_DEFAULT);
        showDateTime = getBooleanProperty(MavenBaseLogger.SHOW_DATE_TIME_KEY, SHOW_DATE_TIME_DEFAULT);
        showThreadName = getBooleanProperty(MavenBaseLogger.SHOW_THREAD_NAME_KEY, SHOW_THREAD_NAME_DEFAULT);
        showThreadId = getBooleanProperty(MavenBaseLogger.SHOW_THREAD_ID_KEY, SHOW_THREAD_ID_DEFAULT);
        dateTimeFormatStr = getStringProperty(MavenBaseLogger.DATE_TIME_FORMAT_KEY, DATE_TIME_FORMAT_STR_DEFAULT);
        levelInBrackets = getBooleanProperty(MavenBaseLogger.LEVEL_IN_BRACKETS_KEY, LEVEL_IN_BRACKETS_DEFAULT);
        warnLevelString = getStringProperty(MavenBaseLogger.WARN_LEVEL_STRING_KEY, WARN_LEVELS_STRING_DEFAULT);

        logFile = getStringProperty(MavenBaseLogger.LOG_FILE_KEY, logFile);

        cacheOutputStream =
                getBooleanProperty(MavenBaseLogger.CACHE_OUTPUT_STREAM_STRING_KEY, CACHE_OUTPUT_STREAM_DEFAULT);
        outputChoice = computeOutputChoice(logFile, cacheOutputStream);

        if (dateTimeFormatStr != null) {
            try {
                dateFormatter = DateTimeFormatter.ofPattern(dateTimeFormatStr).withZone(ZoneId.systemDefault());
            } catch (IllegalArgumentException e) {
                Reporter.error("Bad date format in " + CONFIGURATION_FILE + "; will output relative time", e);
            }
        }
    }

    private void loadProperties() {
        // Add props from the resource simplelogger.properties
        ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
        ClassLoader toUseCL = (threadCL != null ? threadCL : ClassLoader.getSystemClassLoader());
        try (InputStream in = toUseCL.getResourceAsStream(CONFIGURATION_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (java.io.IOException e) {
            // ignored
        }
    }

    String getStringProperty(String name, String defaultValue) {
        String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    boolean getBooleanProperty(String name, boolean defaultValue) {
        String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
    }

    String getStringProperty(String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (SecurityException e) {
            // Ignore
        }
        return (prop == null) ? properties.getProperty(name) : prop;
    }

    static int stringToLevel(String levelStr) {
        if ("trace".equalsIgnoreCase(levelStr)) {
            return MavenBaseLogger.LOG_LEVEL_TRACE;
        } else if ("debug".equalsIgnoreCase(levelStr)) {
            return MavenBaseLogger.LOG_LEVEL_DEBUG;
        } else if ("info".equalsIgnoreCase(levelStr)) {
            return MavenBaseLogger.LOG_LEVEL_INFO;
        } else if ("warn".equalsIgnoreCase(levelStr)) {
            return MavenBaseLogger.LOG_LEVEL_WARN;
        } else if ("error".equalsIgnoreCase(levelStr)) {
            return MavenBaseLogger.LOG_LEVEL_ERROR;
        } else if ("off".equalsIgnoreCase(levelStr)) {
            return MavenBaseLogger.LOG_LEVEL_OFF;
        }
        // assume INFO by default
        return MavenBaseLogger.LOG_LEVEL_INFO;
    }

    private static OutputChoice computeOutputChoice(String logFile, boolean cacheOutputStream) {
        if ("System.err".equalsIgnoreCase(logFile)) {
            return new OutputChoice(cacheOutputStream ? OutputChoiceType.CACHED_SYS_ERR : OutputChoiceType.SYS_ERR);
        } else if ("System.out".equalsIgnoreCase(logFile)) {
            return new OutputChoice(cacheOutputStream ? OutputChoiceType.CACHED_SYS_OUT : OutputChoiceType.SYS_OUT);
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(logFile);
                PrintStream printStream = new PrintStream(fos);
                return new OutputChoice(printStream);
            } catch (FileNotFoundException e) {
                Reporter.error("Could not open [" + logFile + "]. Defaulting to System.err", e);
                return new OutputChoice(OutputChoiceType.SYS_ERR);
            }
        }
    }
}
