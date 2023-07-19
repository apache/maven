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

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.cli.jansi.MessageUtils;

/**
 * Logger for Maven, that support colorization of levels and stacktraces. This class implements 2 methods introduced in
 * slf4j-simple provider local copy.
 *
 * @since 3.5.0
 */
public class MavenSimpleLogger extends SimpleLogger {
    MavenSimpleLogger(String name) {
        super(name);
    }

    @Override
    protected String renderLevel(int level) {
        switch (level) {
            case LOG_LEVEL_TRACE:
                return builder().debug("TRACE").build();
            case LOG_LEVEL_DEBUG:
                return builder().debug("DEBUG").build();
            case LOG_LEVEL_INFO:
                return builder().info("INFO").build();
            case LOG_LEVEL_WARN:
                return builder().warning("WARNING").build();
            case LOG_LEVEL_ERROR:
            default:
                return builder().error("ERROR").build();
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
            stream.print(" " + e.getClassName() + "." + e.getMethodName());
            stream.print(builder().a(" (").strong(getLocation(e)).a(")"));
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
        stream.print(builder().a(prefix).strong(caption).a(": ").a(t.getClass().getName()));
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

    private MessageBuilder builder() {
        return MessageUtils.builder();
    }
}
