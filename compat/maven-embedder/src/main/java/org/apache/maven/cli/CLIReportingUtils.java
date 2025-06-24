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
package org.apache.maven.cli;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.apache.maven.jline.MessageUtils;
import org.apache.maven.utils.Os;
import org.slf4j.Logger;

/**
 * Utility class used to report errors, statistics, application version info, etc.
 *
 */
@Deprecated
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
        String timestamp = reduce(buildProperties.getProperty("timestamp"));
        String version = reduce(buildProperties.getProperty(BUILD_VERSION_PROPERTY));
        String rev = reduce(buildProperties.getProperty("buildNumber"));
        String distributionName = reduce(buildProperties.getProperty("distributionName"));

        String msg = distributionName + " ";
        msg += (version != null ? version : "<version unknown>");
        if (rev != null || timestamp != null) {
            msg += " (";
            msg += (rev != null ? rev : "");
            if (timestamp != null && !timestamp.isEmpty()) {
                String ts = formatTimestamp(Long.parseLong(timestamp));
                msg += (rev != null ? "; " : "") + ts;
            }
            msg += ")";
        }
        return msg;
    }

    private static String reduce(String s) {
        return (s != null ? (s.startsWith("${") && s.endsWith("}") ? null : s) : null);
    }

    public static Properties getBuildProperties() {
        Properties properties = new Properties();

        try (InputStream resourceAsStream =
                MavenCli.class.getResourceAsStream("/org/apache/maven/messages/build.properties")) {

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

    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        return sdf.format(new Date(timestamp));
    }

    public static String formatDuration(long duration) {
        // CHECKSTYLE_OFF: MagicNumber
        long ms = duration % 1000;
        long s = (duration / ONE_SECOND) % 60;
        long m = (duration / ONE_MINUTE) % 60;
        long h = (duration / ONE_HOUR) % 24;
        long d = duration / ONE_DAY;
        // CHECKSTYLE_ON: MagicNumber

        String format;
        if (d > 0) {
            // Length 11+ chars
            format = "%d d %02d:%02d h";
        } else if (h > 0) {
            // Length 7 chars
            format = "%2$02d:%3$02d h";
        } else if (m > 0) {
            // Length 9 chars
            format = "%3$02d:%4$02d min";
        } else {
            // Length 7-8 chars
            format = "%4$d.%5$03d s";
        }

        return String.format(format, d, h, m, s, ms);
    }
}
