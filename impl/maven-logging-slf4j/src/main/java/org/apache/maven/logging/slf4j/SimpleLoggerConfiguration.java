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
package org.apache.maven.logging.slf4j;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;

import org.apache.maven.api.Constants;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.apache.maven.logging.slf4j.OutputChoice.OutputChoiceType;
import org.slf4j.helpers.Reporter;

/**
 * Holds SLF4J formatting configuration for {@link MavenBaseLogger}.
 * Level resolution is delegated to {@link DefaultLoggerManager}.
 */
public class SimpleLoggerConfiguration {

    static final int DEFAULT_LOG_LEVEL_DEFAULT = MavenBaseLogger.LOG_LEVEL_INFO;
    int defaultLogLevel = DEFAULT_LOG_LEVEL_DEFAULT;

    private static final boolean SHOW_DATE_TIME_DEFAULT = false;
    boolean showDateTime = SHOW_DATE_TIME_DEFAULT;

    private static final String DATE_TIME_FORMAT_STR_DEFAULT = null;
    DateTimeFormatter dateFormatter = null;

    private static final boolean SHOW_THREAD_NAME_DEFAULT = true;
    boolean showThreadName = SHOW_THREAD_NAME_DEFAULT;

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

    void init() {
        dateFormatter = null;

        DefaultLoggerManager mgr = getLogManager();

        defaultLogLevel = mgr != null ? mgr.getDefaultLogLevel() : DEFAULT_LOG_LEVEL_DEFAULT;

        String dateTimeFormatStr;

        showLogName = getBooleanProperty(mgr, Constants.MAVEN_LOGGER_SHOW_LOG_NAME, SHOW_LOG_NAME_DEFAULT);
        showShortLogName =
                getBooleanProperty(mgr, Constants.MAVEN_LOGGER_SHOW_SHORT_LOG_NAME, SHOW_SHORT_LOG_NAME_DEFAULT);
        showDateTime = getBooleanProperty(mgr, Constants.MAVEN_LOGGER_SHOW_DATE_TIME, SHOW_DATE_TIME_DEFAULT);
        showThreadName = getBooleanProperty(mgr, Constants.MAVEN_LOGGER_SHOW_THREAD_NAME, SHOW_THREAD_NAME_DEFAULT);
        showThreadId = getBooleanProperty(mgr, Constants.MAVEN_LOGGER_SHOW_THREAD_ID, SHOW_THREAD_ID_DEFAULT);
        dateTimeFormatStr =
                getStringProperty(mgr, Constants.MAVEN_LOGGER_DATE_TIME_FORMAT, DATE_TIME_FORMAT_STR_DEFAULT);
        levelInBrackets = getBooleanProperty(mgr, Constants.MAVEN_LOGGER_LEVEL_IN_BRACKETS, LEVEL_IN_BRACKETS_DEFAULT);
        warnLevelString = getStringProperty(mgr, Constants.MAVEN_LOGGER_WARN_LEVEL, WARN_LEVELS_STRING_DEFAULT);

        logFile = getStringProperty(mgr, Constants.MAVEN_LOGGER_LOG_FILE, logFile);

        cacheOutputStream =
                getBooleanProperty(mgr, Constants.MAVEN_LOGGER_CACHE_OUTPUT_STREAM, CACHE_OUTPUT_STREAM_DEFAULT);
        outputChoice = computeOutputChoice(logFile, cacheOutputStream);

        if (dateTimeFormatStr != null) {
            try {
                dateFormatter = DateTimeFormatter.ofPattern(dateTimeFormatStr);
            } catch (IllegalArgumentException e) {
                Reporter.error("Bad date format; will output relative time", e);
            }
        }
    }

    private static DefaultLoggerManager getLogManager() {
        LoggerManager mgr = LoggerManager.get();
        return mgr instanceof DefaultLoggerManager dlm ? dlm : null;
    }

    private static String getStringProperty(DefaultLoggerManager mgr, String name, String defaultValue) {
        if (mgr != null) {
            return mgr.getStringProperty(name, defaultValue);
        }
        String prop = System.getProperty(name);
        return prop != null ? prop : defaultValue;
    }

    private static boolean getBooleanProperty(DefaultLoggerManager mgr, String name, boolean defaultValue) {
        String prop = getStringProperty(mgr, name, null);
        return prop != null ? "true".equalsIgnoreCase(prop) : defaultValue;
    }

    static int stringToLevel(String levelStr) {
        return DefaultLoggerManager.stringToLevel(levelStr);
    }

    private static OutputChoice computeOutputChoice(String logFile, boolean cacheOutputStream) {
        if ("System.err".equalsIgnoreCase(logFile)) {
            return new OutputChoice(cacheOutputStream ? OutputChoiceType.CACHED_SYS_ERR : OutputChoiceType.SYS_ERR);
        } else if ("System.out".equalsIgnoreCase(logFile)) {
            return new OutputChoice(cacheOutputStream ? OutputChoiceType.CACHED_SYS_OUT : OutputChoiceType.SYS_OUT);
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                PrintStream printStream = new PrintStream(fos, true);
                return new OutputChoice(printStream);
            } catch (FileNotFoundException e) {
                Reporter.error("Could not open [" + logFile + "]. Defaulting to System.err", e);
                return new OutputChoice(OutputChoiceType.SYS_ERR);
            }
        }
    }
}
