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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NormalizedParameters;
import org.slf4j.spi.LocationAwareLogger;

/**
 * <p>
 * Simple implementation of {@link Logger} that sends all enabled log messages,
 * for all defined loggers, to the console ({@code System.err}). The following
 * system properties are supported to configure the behavior of this logger:
 *
 *
 * <ul>
 * <li><code>org.slf4j.simpleLogger.logFile</code> - The output target which can
 * be the <em>path</em> to a file, or the special values "System.out" and
 * "System.err". Default is "System.err".</li>
 *
 * <li><code>org.slf4j.simpleLogger.cacheOutputStream</code> - If the output
 * target is set to "System.out" or "System.err" (see preceding entry), by
 * default, logs will be output to the latest value referenced by
 * <code>System.out/err</code> variables. By setting this parameter to true, the
 * output stream will be cached, i.e. assigned once at initialization time and
 * re-used independently of the current value referenced by
 * <code>System.out/err</code>.</li>
 *
 * <li><code>org.slf4j.simpleLogger.defaultLogLevel</code> - Default log level
 * for all instances of SimpleLogger. Must be one of ("trace", "debug", "info",
 * "warn", "error" or "off"). If not specified, defaults to "info".</li>
 *
 * <li><code>org.slf4j.simpleLogger.log.<em>a.b.c</em></code> - Logging detail
 * level for a SimpleLogger instance named "a.b.c". Right-side value must be one
 * of "trace", "debug", "info", "warn", "error" or "off". When a SimpleLogger
 * named "a.b.c" is initialized, its level is assigned from this property. If
 * unspecified, the level of nearest parent logger will be used, and if none is
 * set, then the value specified by
 * <code>org.slf4j.simpleLogger.defaultLogLevel</code> will be used.</li>
 *
 * <li><code>org.slf4j.simpleLogger.showDateTime</code> - Set to
 * <code>true</code> if you want the current date and time to be included in
 * output messages. Default is <code>false</code></li>
 *
 * <li><code>org.slf4j.simpleLogger.dateTimeFormat</code> - The date and time
 * format to be used in the output messages. The pattern describing the date and
 * time format is defined by <a href=
 * "http://docs.oracle.com/javase/1.5.0/docs/api/java/text/SimpleDateFormat.html">
 * <code>SimpleDateFormat</code></a>. If the format is not specified or is
 * invalid, the number of milliseconds since start up will be output.</li>
 *
 * <li><code>org.slf4j.simpleLogger.showThreadName</code> -Set to
 * <code>true</code> if you want to output the current thread name. Defaults to
 * <code>true</code>.</li>
 *
 * <li>(since version 1.7.33 and 2.0.0-alpha6) <code>org.slf4j.simpleLogger.showThreadId</code> -
 * If you would like to output the current thread id, then set to
 * <code>true</code>. Defaults to <code>false</code>.</li>
 *
 * <li><code>org.slf4j.simpleLogger.showLogName</code> - Set to
 * <code>true</code> if you want the Logger instance name to be included in
 * output messages. Defaults to <code>true</code>.</li>
 *
 * <li><code>org.slf4j.simpleLogger.showShortLogName</code> - Set to
 * <code>true</code> if you want the last component of the name to be included
 * in output messages. Defaults to <code>false</code>.</li>
 *
 * <li><code>org.slf4j.simpleLogger.levelInBrackets</code> - Should the level
 * string be output in brackets? Defaults to <code>false</code>.</li>
 *
 * <li><code>org.slf4j.simpleLogger.warnLevelString</code> - The string value
 * output for the warn level. Defaults to <code>WARN</code>.</li>
 *
 * </ul>
 *
 * <p>
 * In addition to looking for system properties with the names specified above,
 * this implementation also checks for a class loader resource named
 * <code>"simplelogger.properties"</code>, and includes any matching definitions
 * from this resource (if it exists).
 *
 *
 * <p>
 * With no configuration, the default output includes the relative time in
 * milliseconds, thread name, the level, logger name, and the message followed
 * by the line separator for the host. In log4j terms it amounts to the "%r [%t]
 * %level %logger - %m%n" pattern.
 *
 * <p>
 * Sample output follows.
 *
 *
 * <pre>
 * 176 [main] INFO examples.Sort - Populating an array of 2 elements in reverse order.
 * 225 [main] INFO examples.SortAlgo - Entered the sort method.
 * 304 [main] INFO examples.SortAlgo - Dump of integer array:
 * 317 [main] INFO examples.SortAlgo - Element [0] = 0
 * 331 [main] INFO examples.SortAlgo - Element [1] = 1
 * 343 [main] INFO examples.Sort - The next log statement should be an error message.
 * 346 [main] ERROR examples.SortAlgo - Tried to dump an uninitialized array.
 *   at org.log4j.examples.SortAlgo.dump(SortAlgo.java:58)
 *   at org.log4j.examples.Sort.main(Sort.java:64)
 * 467 [main] INFO  examples.Sort - Exiting main method.
 * </pre>
 *
 * <p>
 * This implementation is heavily inspired by
 * <a href="http://commons.apache.org/logging/">Apache Commons Logging</a>'s
 * SimpleLog.
 *
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Scott Sanders
 * @author Rod Waldhoff
 * @author Robert Burrell Donkin
 * @author C&eacute;drik LIME
 */
public class MavenBaseLogger extends LegacyAbstractLogger {

    private static final long serialVersionUID = -632788891211436180L;

    private static final long START_TIME = System.currentTimeMillis();

    protected static final int LOG_LEVEL_TRACE = LocationAwareLogger.TRACE_INT;
    protected static final int LOG_LEVEL_DEBUG = LocationAwareLogger.DEBUG_INT;
    protected static final int LOG_LEVEL_INFO = LocationAwareLogger.INFO_INT;
    protected static final int LOG_LEVEL_WARN = LocationAwareLogger.WARN_INT;
    protected static final int LOG_LEVEL_ERROR = LocationAwareLogger.ERROR_INT;

    static final char SP = ' ';
    static final String TID_PREFIX = "tid=";

    // The OFF level can only be used in configuration files to disable logging.
    // It has
    // no printing method associated with it in o.s.Logger interface.
    protected static final int LOG_LEVEL_OFF = LOG_LEVEL_ERROR + 10;

    static final SimpleLoggerConfiguration CONFIG_PARAMS = new SimpleLoggerConfiguration();

    private static boolean initialized = false;

    static void lazyInit() {
        if (initialized) {
            return;
        }
        initialized = true;
        init();
    }

    // external software might be invoking this method directly. Do not rename
    // or change its semantics.
    static void init() {
        CONFIG_PARAMS.init();
    }

    /** The current log level */
    protected int currentLogLevel = LOG_LEVEL_INFO;
    /** The short name of this simple log instance */
    private transient String shortLogName = null;

    /**
     * All system properties used by <code>SimpleLogger</code> start with this
     * prefix
     */
    public static final String SYSTEM_PREFIX = "org.slf4j.simpleLogger.";

    public static final String LOG_KEY_PREFIX = MavenBaseLogger.SYSTEM_PREFIX + "log.";

    public static final String CACHE_OUTPUT_STREAM_STRING_KEY = MavenBaseLogger.SYSTEM_PREFIX + "cacheOutputStream";

    public static final String WARN_LEVEL_STRING_KEY = MavenBaseLogger.SYSTEM_PREFIX + "warnLevelString";

    public static final String LEVEL_IN_BRACKETS_KEY = MavenBaseLogger.SYSTEM_PREFIX + "levelInBrackets";

    public static final String LOG_FILE_KEY = MavenBaseLogger.SYSTEM_PREFIX + "logFile";

    public static final String SHOW_SHORT_LOG_NAME_KEY = MavenBaseLogger.SYSTEM_PREFIX + "showShortLogName";

    public static final String SHOW_LOG_NAME_KEY = MavenBaseLogger.SYSTEM_PREFIX + "showLogName";

    public static final String SHOW_THREAD_NAME_KEY = MavenBaseLogger.SYSTEM_PREFIX + "showThreadName";

    public static final String SHOW_THREAD_ID_KEY = MavenBaseLogger.SYSTEM_PREFIX + "showThreadId";

    public static final String DATE_TIME_FORMAT_KEY = MavenBaseLogger.SYSTEM_PREFIX + "dateTimeFormat";

    public static final String SHOW_DATE_TIME_KEY = MavenBaseLogger.SYSTEM_PREFIX + "showDateTime";

    public static final String DEFAULT_LOG_LEVEL_KEY = MavenBaseLogger.SYSTEM_PREFIX + "defaultLogLevel";

    /**
     * Protected access allows only {@link MavenLoggerFactory} and also derived classes to instantiate
     * MavenLoggerFactory instances.
     */
    protected MavenBaseLogger(String name) {
        this.name = name;

        String levelString = recursivelyComputeLevelString();
        if (levelString != null) {
            this.currentLogLevel = SimpleLoggerConfiguration.stringToLevel(levelString);
        } else {
            this.currentLogLevel = CONFIG_PARAMS.defaultLogLevel;
        }
    }

    String recursivelyComputeLevelString() {
        String tempName = name;
        String levelString = null;
        int indexOfLastDot = tempName.length();
        while ((levelString == null) && (indexOfLastDot > -1)) {
            tempName = tempName.substring(0, indexOfLastDot);
            levelString = CONFIG_PARAMS.getStringProperty(MavenBaseLogger.LOG_KEY_PREFIX + tempName, null);
            indexOfLastDot = String.valueOf(tempName).lastIndexOf(".");
        }
        return levelString;
    }

    /**
     * To avoid intermingling of log messages and associated stack traces, the two
     * operations are done in a synchronized block.
     *
     * @param buf
     * @param t
     */
    protected void write(StringBuilder buf, Throwable t) {
        PrintStream targetStream = CONFIG_PARAMS.outputChoice.getTargetPrintStream();

        synchronized (CONFIG_PARAMS) {
            targetStream.println(buf.toString());
            writeThrowable(t, targetStream);
            targetStream.flush();
        }
    }

    protected void writeThrowable(Throwable t, PrintStream targetStream) {
        if (t != null) {
            t.printStackTrace(targetStream);
        }
    }

    protected String getFormattedDate() {
        Date now = new Date();
        String dateText;
        synchronized (CONFIG_PARAMS.dateFormatter) {
            dateText = CONFIG_PARAMS.dateFormatter.format(now);
        }
        return dateText;
    }

    protected String computeShortName() {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    // /**
    // * For formatted messages, first substitute arguments and then log.
    // *
    // * @param level
    // * @param format
    // * @param arg1
    // * @param arg2
    // */
    // private void formatAndLog(int level, String format, Object arg1, Object arg2) {
    // if (!isLevelEnabled(level)) {
    // return;
    // }
    // FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
    // log(level, tp.getMessage(), tp.getThrowable());
    // }

    // /**
    // * For formatted messages, first substitute arguments and then log.
    // *
    // * @param level
    // * @param format
    // * @param arguments
    // * a list of 3 ore more arguments
    // */
    // private void formatAndLog(int level, String format, Object... arguments) {
    // if (!isLevelEnabled(level)) {
    // return;
    // }
    // FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
    // log(level, tp.getMessage(), tp.getThrowable());
    // }

    /**
     * Is the given log level currently enabled?
     *
     * @param logLevel is this level enabled?
     * @return whether the logger is enabled for the given level
     */
    protected boolean isLevelEnabled(int logLevel) {
        // log level are numerically ordered so can use simple numeric
        // comparison
        return (logLevel >= currentLogLevel);
    }

    /** Are {@code trace} messages currently enabled? */
    public boolean isTraceEnabled() {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    /** Are {@code debug} messages currently enabled? */
    public boolean isDebugEnabled() {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    /** Are {@code info} messages currently enabled? */
    public boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    /** Are {@code warn} messages currently enabled? */
    public boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    /** Are {@code error} messages currently enabled? */
    public boolean isErrorEnabled() {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    /**
     * SimpleLogger's implementation of
     * {@link org.slf4j.helpers.AbstractLogger#handleNormalizedLoggingCall(Level, Marker, String, Object[], Throwable) AbstractLogger#handleNormalizedLoggingCall}
     * }
     *
     * @param level the SLF4J level for this event
     * @param marker  The marker to be used for this event, may be null.
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param arguments  the array of arguments to be formatted, may be null
     * @param throwable  The exception whose stack trace should be logged, may be null
     */
    @Override
    protected void handleNormalizedLoggingCall(
            Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {

        List<Marker> markers = null;

        if (marker != null) {
            markers = new ArrayList<>();
            markers.add(marker);
        }

        innerHandleNormalizedLoggingCall(level, markers, messagePattern, arguments, throwable);
    }

    private void innerHandleNormalizedLoggingCall(
            Level level, List<Marker> markers, String messagePattern, Object[] arguments, Throwable t) {

        StringBuilder buf = new StringBuilder(32);

        // Append date-time if so configured
        if (CONFIG_PARAMS.showDateTime) {
            if (CONFIG_PARAMS.dateFormatter != null) {
                buf.append(getFormattedDate());
                buf.append(SP);
            } else {
                buf.append(System.currentTimeMillis() - START_TIME);
                buf.append(SP);
            }
        }

        // Append current thread name if so configured
        if (CONFIG_PARAMS.showThreadName) {
            buf.append('[');
            buf.append(Thread.currentThread().getName());
            buf.append("] ");
        }

        if (CONFIG_PARAMS.showThreadId) {
            buf.append(TID_PREFIX);
            buf.append(Thread.currentThread().getId());
            buf.append(SP);
        }

        if (CONFIG_PARAMS.levelInBrackets) {
            buf.append('[');
        }

        // Append a readable representation of the log level
        String levelStr = renderLevel(level.toInt());
        buf.append(levelStr);
        if (CONFIG_PARAMS.levelInBrackets) {
            buf.append(']');
        }
        buf.append(SP);

        // Append the name of the log instance if so configured
        if (CONFIG_PARAMS.showShortLogName) {
            if (shortLogName == null) {
                shortLogName = computeShortName();
            }
            buf.append(shortLogName).append(" - ");
        } else if (CONFIG_PARAMS.showLogName) {
            buf.append(name).append(" - ");
        }

        if (markers != null) {
            buf.append(SP);
            for (Marker marker : markers) {
                buf.append(marker.getName()).append(SP);
            }
        }

        String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);

        // Append the message
        buf.append(formattedMessage);

        write(buf, t);
    }

    protected String renderLevel(int levelInt) {
        switch (levelInt) {
            case LOG_LEVEL_TRACE:
                return "TRACE";
            case LOG_LEVEL_DEBUG:
                return ("DEBUG");
            case LOG_LEVEL_INFO:
                return "INFO";
            case LOG_LEVEL_WARN:
                return "WARN";
            case LOG_LEVEL_ERROR:
                return "ERROR";
            default:
                throw new IllegalStateException("Unrecognized level [" + levelInt + "]");
        }
    }

    public void log(LoggingEvent event) {
        int levelInt = event.getLevel().toInt();

        if (!isLevelEnabled(levelInt)) {
            return;
        }

        NormalizedParameters np = NormalizedParameters.normalize(event);

        innerHandleNormalizedLoggingCall(
                event.getLevel(), event.getMarkers(), np.getMessage(), np.getArguments(), event.getThrowable());
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }
}
