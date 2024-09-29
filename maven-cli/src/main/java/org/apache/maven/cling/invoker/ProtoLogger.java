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
package org.apache.maven.cling.invoker;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.maven.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * Proto {@link Logger}. Uses provided {@link PrintStream}s or {@link System} ones as fallback.
 * Supports only two levels: ERROR and WARNING, that is emitted to STDERR and STDOUT.
 */
public class ProtoLogger implements Logger {
    /**
     * The only supported logging levels.
     */
    private enum Level {
        WARNING,
        ERROR
    }

    private final PrintStream out;
    private final PrintStream err;

    public ProtoLogger() {
        this(null, null);
    }

    public ProtoLogger(@Nullable OutputStream out, @Nullable OutputStream err) {
        this.out = toPsOrDef(out, System.out);
        this.err = toPsOrDef(err, System.err);
    }

    private PrintStream toPsOrDef(OutputStream outputStream, PrintStream def) {
        if (outputStream == null) {
            return def;
        }
        if (outputStream instanceof PrintStream ps) {
            return ps;
        }
        return new PrintStream(outputStream);
    }

    //
    // These are the only methods we need in our primordial logger
    //

    private void doHandle(Level level, String message, Object... params) {
        PrintStream ps = level == Level.ERROR ? err : out;
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, params);
        ps.print(level.name());
        ps.println(" ");
        ps.println(tuple.getMessage());
        if (tuple.getThrowable() != null) {
            tuple.getThrowable().printStackTrace(ps);
        }
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public void error(String msg) {
        doHandle(Level.ERROR, msg);
    }

    public void error(String format, Object arg) {
        doHandle(Level.ERROR, format, arg);
    }

    public void error(String format, Object... arguments) {
        doHandle(Level.ERROR, format, arguments);
    }

    public void error(String format, Object arg1, Object arg2) {
        doHandle(Level.ERROR, format, arg1, arg2);
    }

    public void error(String msg, Throwable t) {
        doHandle(Level.ERROR, msg, t);
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public void warn(String msg) {
        doHandle(Level.WARNING, msg);
    }

    public void warn(String format, Object arg) {
        doHandle(Level.WARNING, format, arg);
    }

    public void warn(String format, Object... arguments) {
        doHandle(Level.WARNING, format, arguments);
    }

    public void warn(String format, Object arg1, Object arg2) {
        doHandle(Level.WARNING, format, arg1, arg2);
    }

    public void warn(String msg, Throwable t) {
        doHandle(Level.WARNING, msg, t);
    }

    //
    // Don't need any of this
    //
    public String getName() {
        return null;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public void trace(String msg) {}

    public void trace(String format, Object arg) {}

    public void trace(String format, Object arg1, Object arg2) {}

    public void trace(String format, Object... arguments) {}

    public void trace(String msg, Throwable t) {}

    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    public void trace(Marker marker, String msg) {}

    public void trace(Marker marker, String format, Object arg) {}

    public void trace(Marker marker, String format, Object arg1, Object arg2) {}

    public void trace(Marker marker, String format, Object... argArray) {}

    public void trace(Marker marker, String msg, Throwable t) {}

    public boolean isDebugEnabled() {
        return false;
    }

    public void debug(String msg) {}

    public void debug(String format, Object arg) {}

    public void debug(String format, Object arg1, Object arg2) {}

    public void debug(String format, Object... arguments) {}

    public void debug(String msg, Throwable t) {}

    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    public void debug(Marker marker, String msg) {}

    public void debug(Marker marker, String format, Object arg) {}

    public void debug(Marker marker, String format, Object arg1, Object arg2) {}

    public void debug(Marker marker, String format, Object... arguments) {}

    public void debug(Marker marker, String msg, Throwable t) {}

    public boolean isInfoEnabled() {
        return false;
    }

    public void info(String msg) {}

    public void info(String format, Object arg) {}

    public void info(String format, Object arg1, Object arg2) {}

    public void info(String format, Object... arguments) {}

    public void info(String msg, Throwable t) {}

    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    public void info(Marker marker, String msg) {}

    public void info(Marker marker, String format, Object arg) {}

    public void info(Marker marker, String format, Object arg1, Object arg2) {}

    public void info(Marker marker, String format, Object... arguments) {}

    public void info(Marker marker, String msg, Throwable t) {}

    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    public void warn(Marker marker, String msg) {}

    public void warn(Marker marker, String format, Object arg) {}

    public void warn(Marker marker, String format, Object arg1, Object arg2) {}

    public void warn(Marker marker, String format, Object... arguments) {}

    public void warn(Marker marker, String msg, Throwable t) {}

    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    public void error(Marker marker, String msg) {}

    public void error(Marker marker, String format, Object arg) {}

    public void error(Marker marker, String format, Object arg1, Object arg2) {}

    public void error(Marker marker, String format, Object... arguments) {}

    public void error(Marker marker, String msg, Throwable t) {}
}
