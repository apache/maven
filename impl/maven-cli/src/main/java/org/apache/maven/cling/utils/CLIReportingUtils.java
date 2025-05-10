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
package org.apache.maven.cling.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Properties;

import org.apache.maven.impl.util.Os;
import org.apache.maven.jline.MessageUtils;
import org.slf4j.Logger;

/**
 * Utility class used to report errors, statistics, application version info, etc.
 *
 */
public final class CLIReportingUtils {
    // CHECKSTYLE_OFF: MagicNumber
    public static final long MB = 1024 * 1024;

    private static final long ONE_SECOND = 1000L;

    private static final long ONE_MINUTE = 60 * ONE_SECOND;

    private static final long ONE_HOUR = 60 * ONE_MINUTE;

    private static final long ONE_DAY = 24 * ONE_HOUR;
    // CHECKSTYLE_ON: MagicNumber

    public static final String BUILD_VERSION_PROPERTY = "version";

    public static String showVersion() {
        return showVersion(null, null);
    }

    public static String showVersion(String commandLine, String terminal) {
        final String ls = System.lineSeparator();
        Properties properties = getBuildProperties();
        StringBuilder version = new StringBuilder(256);
        version.append(MessageUtils.builder().strong(createMavenVersionString(properties)))
                .append(ls);
        version.append(reduce(properties.getProperty("distributionShortName") + " home: "
                        + System.getProperty("maven.home", "<unknown Maven " + "home>")))
                .append(ls);
        version.append("Java version: ")
                .append(System.getProperty("java.version", "<unknown Java version>"))
                .append(", vendor: ")
                .append(System.getProperty("java.vendor", "<unknown vendor>"))
                .append(", runtime: ")
                .append(System.getProperty("java.home", "<unknown runtime>"))
                .append(ls);
        version.append("Default locale: ")
                .append(Locale.getDefault())
                .append(", platform encoding: ")
                .append(System.getProperty("file.encoding", "<unknown encoding>"))
                .append(ls);
        version.append("OS name: \"")
                .append(Os.OS_NAME)
                .append("\", version: \"")
                .append(Os.OS_VERSION)
                .append("\", arch: \"")
                .append(Os.OS_ARCH)
                .append("\", family: \"")
                .append(Os.OS_FAMILY)
                .append('\"');
        // Add process information using modern Java API
        if (commandLine != null) {
            version.append(ls).append("Command line: ").append(commandLine);
        }
        if (terminal != null) {
            version.append(ls).append("Terminal: ").append(terminal);
        }
        return version.toString();
    }

    public static String showVersionMinimal() {
        Properties properties = getBuildProperties();
        String version = reduce(properties.getProperty(BUILD_VERSION_PROPERTY));
        return (version != null ? version : "<version unknown>");
    }

    /**
     * Create a human-readable string containing the Maven version, buildnumber, and time of build
     *
     * @param buildProperties The build properties
     * @return Readable build info
     */
    public static String createMavenVersionString(Properties buildProperties) {
        String version = reduce(buildProperties.getProperty(BUILD_VERSION_PROPERTY));
        String rev = reduce(buildProperties.getProperty("buildNumber"));
        String distributionName = reduce(buildProperties.getProperty("distributionName"));

        return distributionName + " "
                + (version != null ? version : "<version unknown>")
                + (rev != null ? " (" + rev + ")" : "");
    }

    private static String reduce(String s) {
        return (s != null ? (s.startsWith("${") && s.endsWith("}") ? null : s) : null);
    }

    public static Properties getBuildProperties() {
        Properties properties = new Properties();

        try (InputStream resourceAsStream =
                CLIReportingUtils.class.getResourceAsStream("/org/apache/maven/messages/build.properties")) {

            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
            }
        } catch (IOException e) {
            System.err.println("Unable determine version from JAR file: " + e.getMessage());
        }

        return properties;
    }

    public static void showError(Logger logger, String message, Throwable e, boolean showStackTrace) {
        if (logger == null) {
            System.err.println(message);
            if (showStackTrace && e != null) {
                e.printStackTrace(System.err);
            }
            return;
        }
        if (showStackTrace) {
            logger.error(message, e);
        } else {
            logger.error(message);

            if (e != null) {
                logger.error(e.getMessage());

                for (Throwable cause = e.getCause();
                        cause != null && cause != cause.getCause();
                        cause = cause.getCause()) {
                    logger.error("Caused by: {}", cause.getMessage());
                }
            }
        }
    }

    public static String formatTimestamp(TemporalAccessor instant) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant);
    }

    public static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        if (days > 0) {
            return "%d d %02d:%02d h".formatted(days, hours, minutes);
        } else if (hours > 0) {
            return "%02d:%02d h".formatted(hours, minutes);
        } else if (minutes > 0) {
            return "%02d:%02d min".formatted(minutes, seconds);
        } else {
            return "%d.%03d s".formatted(seconds, millis);
        }
    }
}
