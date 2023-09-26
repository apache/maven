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
package org.slf4j.impl;

import java.io.PrintStream;

import static org.apache.maven.cli.jansi.MessageUtils.builder;

/**
 * Logger for Maven, that support colorization of levels and stacktraces. This class implements 2 methods introduced in
 * slf4j-simple provider local copy.
 *
 * @since 3.5.0
 */
public class MavenSimpleLogger extends SimpleLogger {

    private static final String TRACE_RENDERED_LEVEL = builder().trace("TRACE").build();
    private static final String DEBUG_RENDERED_LEVEL = builder().debug("DEBUG").build();
    private static final String INFO_RENDERED_LEVEL = builder().info("INFO").build();
    private static final String WARN_RENDERED_LEVEL =
            builder().warning("WARNING").build();
    private static final String ERROR_RENDERED_LEVEL = builder().error("ERROR").build();

    MavenSimpleLogger(String name) {
        super(name);
    }

    @Override
    protected String renderLevel(int level) {
        switch (level) {
            case LOG_LEVEL_TRACE:
                return TRACE_RENDERED_LEVEL;
            case LOG_LEVEL_DEBUG:
                return DEBUG_RENDERED_LEVEL;
            case LOG_LEVEL_INFO:
                return INFO_RENDERED_LEVEL;
            case LOG_LEVEL_WARN:
                return WARN_RENDERED_LEVEL;
            case LOG_LEVEL_ERROR:
            default:
                return ERROR_RENDERED_LEVEL;
        }
    }

    @Override
    protected void writeThrowable(Throwable t, PrintStream stream) {
        if (t == null) {
            return;
        }
        stream.print(builder().failure(t.getClass().getName()));
        if (t.getMessage() != null) {
            stream.print(": ");
            stream.print(builder().failure(t.getMessage()));
        }
        stream.println();

        printStackTrace(t, stream, "");
    }

    private void printStackTrace(Throwable t, PrintStream stream, String prefix) {
        for (StackTraceElement e : t.getStackTrace()) {
            stream.print(prefix);
            stream.print("    ");
            stream.print(builder().strong("at"));
            stream.print(" ");
            stream.print(e.getClassName());
            stream.print(".");
            stream.print(e.getMethodName());
            stream.print(" (");
            stream.print(builder().strong(getLocation(e)));
            stream.print(")");
            stream.println();
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
        stream.print(prefix);
        stream.print(builder().strong(caption));
        stream.print(": ");
        stream.print(t.getClass().getName());
        if (t.getMessage() != null) {
            stream.print(": ");
            stream.print(builder().failure(t.getMessage()));
        }
        stream.println();

        printStackTrace(t, stream, prefix);
    }

    protected String getLocation(final StackTraceElement e) {
        assert e != null;

        if (e.isNativeMethod()) {
            return "Native Method";
        } else if (e.getFileName() == null) {
            return "Unknown Source";
        } else if (e.getLineNumber() >= 0) {
            return String.format("%s:%s", e.getFileName(), e.getLineNumber());
        } else {
            return e.getFileName();
        }
    }
}
