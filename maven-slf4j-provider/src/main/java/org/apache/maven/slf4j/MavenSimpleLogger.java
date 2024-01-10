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

import org.apache.maven.api.services.MessageBuilder;
import org.slf4j.simple.SimpleLogger;

import static org.apache.maven.cli.jline.MessageUtils.builder;

/**
 * Logger for Maven, that support colorization of levels and stacktraces. This class implements 2 methods introduced in
 * slf4j-simple provider local copy.
 *
 * @since 3.5.0
 */
public class MavenSimpleLogger extends SimpleLogger {

    private final String traceRenderedLevel = builder().trace("TRACE").build();
    private final String debugRenderedLevel = builder().debug("DEBUG").build();
    private final String infoRenderedLevel = builder().info("INFO").build();
    private final String warnRenderedLevel = builder().warning("WARNING").build();
    private final String errorRenderedLevel = builder().error("ERROR").build();

    MavenSimpleLogger(String name) {
        super(name);
    }

    @Override
    protected String renderLevel(int level) {
        switch (level) {
            case LOG_LEVEL_TRACE:
                return traceRenderedLevel;
            case LOG_LEVEL_DEBUG:
                return debugRenderedLevel;
            case LOG_LEVEL_INFO:
                return infoRenderedLevel;
            case LOG_LEVEL_WARN:
                return warnRenderedLevel;
            case LOG_LEVEL_ERROR:
            default:
                return errorRenderedLevel;
        }
    }

    @Override
    protected void writeThrowable(Throwable t, PrintStream stream) {
        if (t == null) {
            return;
        }
        MessageBuilder builder = builder().failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.println(builder);

        printStackTrace(t, stream, "");
    }

    private void printStackTrace(Throwable t, PrintStream stream, String prefix) {
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
            stream.println(builder);
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

    private void writeThrowable(Throwable t, PrintStream stream, String caption, String prefix) {
        MessageBuilder builder =
                builder().a(prefix).strong(caption).a(": ").a(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.println(builder);

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
}
