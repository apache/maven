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
package org.apache.maven.internal.impl;

import java.lang.StackWalker.StackFrame;
import java.util.function.Supplier;

import org.apache.maven.api.plugin.Log;
import org.apache.maven.slf4j.MavenSimpleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import static java.util.Objects.requireNonNull;

public class DefaultLog implements Log {

    private static final StackWalker WALKER = StackWalker.getInstance();
    private static final String THIS_CLASS = DefaultLog.class.getName();

    private final Logger logger;

    public DefaultLog(Logger logger) {
        this.logger = requireNonNull(logger);
    }

    @SuppressWarnings("deprecation") // Thread.getId() — threadId() requires Java 19+
    private void doLog(int level, String message, Throwable throwable) {
        if (logger instanceof MavenSimpleLogger msl) {
            MavenSimpleLogger.SourceMetadata metadata = MavenSimpleLogger.isMetadataEnabled()
                    ? new MavenSimpleLogger.SourceMetadata(
                            logger.getName(),
                            resolveCallerMethod(),
                            Thread.currentThread().getId())
                    : null;
            msl.logDirect(level, message, throwable, metadata);
        } else {
            logToSlf4j(level, message, throwable);
        }
    }

    private String resolveCallerMethod() {
        return WALKER.walk(frames -> frames.dropWhile(f -> THIS_CLASS.equals(f.getClassName()))
                .findFirst()
                .map(StackFrame::getMethodName)
                .orElse(null));
    }

    private void logToSlf4j(int level, String message, Throwable throwable) {
        if (throwable != null) {
            switch (level) {
                case LocationAwareLogger.TRACE_INT -> logger.trace(message, throwable);
                case LocationAwareLogger.DEBUG_INT -> logger.debug(message, throwable);
                case LocationAwareLogger.INFO_INT -> logger.info(message, throwable);
                case LocationAwareLogger.WARN_INT -> logger.warn(message, throwable);
                default -> logger.error(message, throwable);
            }
        } else {
            switch (level) {
                case LocationAwareLogger.TRACE_INT -> logger.trace(message);
                case LocationAwareLogger.DEBUG_INT -> logger.debug(message);
                case LocationAwareLogger.INFO_INT -> logger.info(message);
                case LocationAwareLogger.WARN_INT -> logger.warn(message);
                default -> logger.error(message);
            }
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(CharSequence content) {
        if (isTraceEnabled()) {
            doLog(LocationAwareLogger.TRACE_INT, toString(content), null);
        }
    }

    @Override
    public void trace(CharSequence content, Throwable error) {
        if (isTraceEnabled()) {
            doLog(LocationAwareLogger.TRACE_INT, toString(content), error);
        }
    }

    @Override
    public void trace(Throwable error) {
        if (isTraceEnabled()) {
            doLog(LocationAwareLogger.TRACE_INT, "", error);
        }
    }

    @Override
    public void trace(Supplier<String> content) {
        if (isTraceEnabled()) {
            doLog(LocationAwareLogger.TRACE_INT, content.get(), null);
        }
    }

    @Override
    public void trace(Supplier<String> content, Throwable error) {
        if (isTraceEnabled()) {
            doLog(LocationAwareLogger.TRACE_INT, content.get(), error);
        }
    }

    @Override
    public void debug(CharSequence content) {
        if (isDebugEnabled()) {
            doLog(LocationAwareLogger.DEBUG_INT, toString(content), null);
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (isDebugEnabled()) {
            doLog(LocationAwareLogger.DEBUG_INT, toString(content), error);
        }
    }

    @Override
    public void debug(Throwable error) {
        if (isDebugEnabled()) {
            doLog(LocationAwareLogger.DEBUG_INT, "", error);
        }
    }

    @Override
    public void debug(Supplier<String> content) {
        if (isDebugEnabled()) {
            doLog(LocationAwareLogger.DEBUG_INT, content.get(), null);
        }
    }

    @Override
    public void debug(Supplier<String> content, Throwable error) {
        if (isDebugEnabled()) {
            doLog(LocationAwareLogger.DEBUG_INT, content.get(), error);
        }
    }

    @Override
    public void info(CharSequence content) {
        if (isInfoEnabled()) {
            doLog(LocationAwareLogger.INFO_INT, toString(content), null);
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (isInfoEnabled()) {
            doLog(LocationAwareLogger.INFO_INT, toString(content), error);
        }
    }

    @Override
    public void info(Throwable error) {
        if (isInfoEnabled()) {
            doLog(LocationAwareLogger.INFO_INT, "", error);
        }
    }

    @Override
    public void info(Supplier<String> content) {
        if (isInfoEnabled()) {
            doLog(LocationAwareLogger.INFO_INT, content.get(), null);
        }
    }

    @Override
    public void info(Supplier<String> content, Throwable error) {
        if (isInfoEnabled()) {
            doLog(LocationAwareLogger.INFO_INT, content.get(), error);
        }
    }

    @Override
    public void warn(CharSequence content) {
        if (isWarnEnabled()) {
            doLog(LocationAwareLogger.WARN_INT, toString(content), null);
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (isWarnEnabled()) {
            doLog(LocationAwareLogger.WARN_INT, toString(content), error);
        }
    }

    @Override
    public void warn(Throwable error) {
        if (isWarnEnabled()) {
            doLog(LocationAwareLogger.WARN_INT, "", error);
        }
    }

    @Override
    public void warn(Supplier<String> content) {
        if (isWarnEnabled()) {
            doLog(LocationAwareLogger.WARN_INT, content.get(), null);
        }
    }

    @Override
    public void warn(Supplier<String> content, Throwable error) {
        if (isWarnEnabled()) {
            doLog(LocationAwareLogger.WARN_INT, content.get(), error);
        }
    }

    @Override
    public void error(CharSequence content) {
        if (isErrorEnabled()) {
            doLog(LocationAwareLogger.ERROR_INT, toString(content), null);
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (isErrorEnabled()) {
            doLog(LocationAwareLogger.ERROR_INT, toString(content), error);
        }
    }

    @Override
    public void error(Throwable error) {
        if (isErrorEnabled()) {
            doLog(LocationAwareLogger.ERROR_INT, "", error);
        }
    }

    @Override
    public void error(Supplier<String> content) {
        if (isErrorEnabled()) {
            doLog(LocationAwareLogger.ERROR_INT, content.get(), null);
        }
    }

    @Override
    public void error(Supplier<String> content, Throwable error) {
        if (isErrorEnabled()) {
            doLog(LocationAwareLogger.ERROR_INT, content.get(), error);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public Log child(String name) {
        requireNonNull(name, "name");
        return new DefaultLog(LoggerFactory.getLogger(logger.getName() + "." + name));
    }

    private String toString(CharSequence content) {
        return content != null ? content.toString() : "";
    }
}
