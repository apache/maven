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

    static Consumer<String> logSink;

    public static final String DEFAULT_LOG_LEVEL_KEY = "org.slf4j.simpleLogger.defaultLogLevel";

    public static void setLogSink(Consumer<String> logSink) {
        MavenSimpleLogger.logSink = logSink;
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

    protected void write(StringBuilder buf, Throwable t) {
        Consumer<String> sink = logSink;
        if (sink != null) {
            sink.accept(buf.toString());
            if (t != null) {
                writeThrowable(t, sink);
            }
        } else {
            super.write(buf, t);
        }
    }

    @Override
    protected void writeThrowable(Throwable t, PrintStream stream) {
        writeThrowable(t, stream::println);
    }

    protected void writeThrowable(Throwable t, Consumer<String> stream) {
        if (t == null) {
            return;
        }
        MessageBuilder builder = builder().failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.accept(builder.toString());

        printStackTrace(t, stream, "");
    }

    protected void printStackTrace(Throwable t, Consumer<String> stream, String prefix) {
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
        for (Throwable se : t.getSuppressed()) {
            writeThrowable(se, stream, "Suppressed", prefix + "    ");
        }
        Throwable cause = t.getCause();
        if (cause != null && t != cause) {
            writeThrowable(cause, stream, "Caused by", prefix);
        }
    }

    protected void writeThrowable(Throwable t, Consumer<String> stream, String caption, String prefix) {
        MessageBuilder builder =
                builder().a(prefix).strong(caption).a(": ").a(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.accept(builder.toString());

        printStackTrace(t, stream, prefix);
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
