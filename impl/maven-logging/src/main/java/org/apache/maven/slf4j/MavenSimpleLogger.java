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
import java.util.function.Consumer;

import org.apache.maven.api.services.MessageBuilder;

import static org.apache.maven.jline.MessageUtils.builder;

/**
 * Logger for Maven, that support colorization of levels and stacktraces. This class implements 2 methods introduced in
 * slf4j-simple provider local copy.
 *
 * @since 3.5.0
 */
public class MavenSimpleLogger extends MavenBaseLogger {

    private String traceRenderedLevel;
    private String debugRenderedLevel;
    private String infoRenderedLevel;
    private String warnRenderedLevel;
    private String errorRenderedLevel;

    /**
     * Structured log sink that receives level, logger name, clean message,
     * formatted console output, and throwable for each log event.
     * <p>
     * This replaces the previous {@code Consumer<String>} sink to enable
     * console renderers (e.g. rich mode) to filter by log level and access
     * the clean message independently of ANSI formatting.
     *
     * @since 4.1.0
     */
    @FunctionalInterface
    public interface LogSink {
        void accept(int level, String loggerName, String cleanMessage, String formattedMessage, Throwable throwable);
    }

    static volatile LogSink logSink;

    public static final String DEFAULT_LOG_LEVEL_KEY = "org.slf4j.simpleLogger.defaultLogLevel";

    /**
     * Sets the structured log sink.
     *
     * @param logSink the sink, or {@code null} to remove
     * @since 4.1.0
     */
    public static void setLogSink(LogSink logSink) {
        MavenSimpleLogger.logSink = logSink;
    }

    /**
     * Returns the current log sink, or {@code null} if none is set.
     *
     * @return the current log sink, or {@code null}
     * @since 4.1.0
     */
    public static LogSink getLogSink() {
        return logSink;
    }

    MavenSimpleLogger(String name) {
        super(name);
    }

    @Override
    protected String renderLevel(int level) {
        if (traceRenderedLevel == null) {
            traceRenderedLevel = builder().trace("TRACE").build();
            debugRenderedLevel = builder().debug("DEBUG").build();
            infoRenderedLevel = builder().info("INFO").build();
            warnRenderedLevel = builder().warning("WARNING").build();
            errorRenderedLevel = builder().error("ERROR").build();
        }
        return switch (level) {
            case LOG_LEVEL_TRACE -> traceRenderedLevel;
            case LOG_LEVEL_DEBUG -> debugRenderedLevel;
            case LOG_LEVEL_INFO -> infoRenderedLevel;
            case LOG_LEVEL_WARN -> warnRenderedLevel;
            default -> errorRenderedLevel;
        };
    }

    @Override
    protected void write(int level, String loggerName, String cleanMessage, StringBuilder formattedBuf, Throwable t) {
        LogSink sink = logSink;
        if (sink != null) {
            // Build the full formatted output including throwable rendering,
            // reusing the existing writeThrowable/printStackTrace methods
            // to keep a single rendering path for throwables.
            String formatted = formattedBuf.toString();
            if (t != null) {
                StringBuilder full = new StringBuilder(formatted);
                full.append(System.lineSeparator());
                writeThrowable(t, line -> full.append(line).append(System.lineSeparator()));
                formatted = full.toString();
            }
            sink.accept(level, loggerName, cleanMessage, formatted, t);
        } else {
            super.write(formattedBuf, t);
        }
    }

    @Override
    protected void writeThrowable(Throwable t, PrintStream stream) {
        writeThrowable(t, stream::println);
    }

    private static final int MAX_THROWABLE_DEPTH = 20;

    protected void writeThrowable(Throwable t, Consumer<String> stream) {
        if (t == null) {
            return;
        }
        MessageBuilder builder = builder().failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.accept(builder.toString());

        printStackTrace(t, stream, "", 0);
    }

    protected void printStackTrace(Throwable t, Consumer<String> stream, String prefix) {
        printStackTrace(t, stream, prefix, 0);
    }

    private void printStackTrace(Throwable t, Consumer<String> stream, String prefix, int depth) {
        MessageBuilder builder = builder();
        for (StackTraceElement e : t.getStackTrace()) {
            builder.a(prefix);
            builder.a("    ");
            builder.strong("at");
            builder.a(" ");
            builder.a(e.getClassName());
            builder.a(".");
            builder.a(e.getMethodName());
            builder.a("(");
            builder.strong(getLocation(e));
            builder.a(")");
            stream.accept(builder.toString());
            builder.setLength(0);
        }
        if (depth < MAX_THROWABLE_DEPTH) {
            for (Throwable se : t.getSuppressed()) {
                writeThrowable(se, stream, "Suppressed", prefix + "    ", depth + 1);
            }
            Throwable cause = t.getCause();
            if (cause != null && t != cause) {
                writeThrowable(cause, stream, "Caused by", prefix, depth + 1);
            }
        } else {
            stream.accept(prefix + "    [...cause/suppressed chain truncated at depth " + MAX_THROWABLE_DEPTH + "]");
        }
    }

    protected void writeThrowable(Throwable t, Consumer<String> stream, String caption, String prefix) {
        writeThrowable(t, stream, caption, prefix, 0);
    }

    private void writeThrowable(Throwable t, Consumer<String> stream, String caption, String prefix, int depth) {
        MessageBuilder builder =
                builder().a(prefix).strong(caption).a(": ").a(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.accept(builder.toString());

        printStackTrace(t, stream, prefix, depth);
    }

    protected String getLocation(final StackTraceElement e) {
        assert e != null;

        if (e.isNativeMethod()) {
            return "Native Method";
        } else if (e.getFileName() == null) {
            return "Unknown Source";
        } else if (e.getLineNumber() >= 0) {
            return e.getFileName() + ":" + e.getLineNumber();
        } else {
            return e.getFileName();
        }
    }

    public void configure(int defaultLogLevel) {
        String levelString = recursivelyComputeLevelString();
        if (levelString != null) {
            this.currentLogLevel = SimpleLoggerConfiguration.stringToLevel(levelString);
        } else {
            this.currentLogLevel = defaultLogLevel;
        }
        traceRenderedLevel = builder().trace("TRACE").build();
        debugRenderedLevel = builder().debug("DEBUG").build();
        infoRenderedLevel = builder().info("INFO").build();
        warnRenderedLevel = builder().warning("WARNING").build();
        errorRenderedLevel = builder().error("ERROR").build();
    }

    public void setLogLevel(int logLevel) {
        this.currentLogLevel = logLevel;
    }
}
