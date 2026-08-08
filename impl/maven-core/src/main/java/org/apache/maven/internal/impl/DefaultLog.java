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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class DefaultLog implements Log {

    /**
     * Metadata captured from Log API calls, mirroring the JUL metadata
     * pattern in {@code MavenJulHandler}.
     *
     * @param sourceClassName  the fully qualified class name of the caller
     * @param sourceMethodName the method that issued the log call
     * @param threadId         the originating thread ID
     */
    public record LogApiMetadata(String sourceClassName, String sourceMethodName, long threadId) {}

    private static final ThreadLocal<LogApiMetadata> LOG_API_METADATA = new ThreadLocal<>();
    private static final StackWalker WALKER = StackWalker.getInstance();
    private static final String THIS_CLASS = DefaultLog.class.getName();

    /**
     * Returns the Log API metadata for the current log event being processed,
     * or {@code null} if the current event did not originate from the Log API.
     * <p>
     * Called from {@code ProjectBuildLogAppender.accept()} to populate
     * {@code LogEvent.sourceClassName()} and {@code LogEvent.sourceMethodName()}.
     *
     * @return the current Log API metadata, or {@code null}
     */
    public static LogApiMetadata getLogApiMetadata() {
        return LOG_API_METADATA.get();
    }

    private final Logger logger;

    public DefaultLog(Logger logger) {
        this.logger = requireNonNull(logger);
    }

    /**
     * Wraps a logging call with Log API metadata: captures the caller's
     * method name via {@link StackWalker}, sets the ThreadLocal, executes
     * the actual SLF4J call, and clears the ThreadLocal.
     * <p>
     * The source class name is taken from the SLF4J logger name (which
     * is the mojo implementation FQCN, set at injection time).  The
     * source method name is resolved by walking the stack past this class
     * to find the first external caller frame.
     */
    private void withMetadata(Runnable logAction) {
        String callerMethodName = WALKER.walk(frames -> frames.dropWhile(f -> THIS_CLASS.equals(f.getClassName()))
                .findFirst()
                .map(StackFrame::getMethodName)
                .orElse(null));
        LOG_API_METADATA.set(new LogApiMetadata(
                logger.getName(), callerMethodName, Thread.currentThread().getId()));
        try {
            logAction.run();
        } finally {
            LOG_API_METADATA.remove();
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(CharSequence content) {
        if (isTraceEnabled()) {
            withMetadata(() -> logger.trace(toString(content)));
        }
    }

    @Override
    public void trace(CharSequence content, Throwable error) {
        if (isTraceEnabled()) {
            withMetadata(() -> logger.trace(toString(content), error));
        }
    }

    @Override
    public void trace(Throwable error) {
        if (isTraceEnabled()) {
            withMetadata(() -> logger.trace("", error));
        }
    }

    @Override
    public void trace(Supplier<String> content) {
        if (isTraceEnabled()) {
            withMetadata(() -> logger.trace(content.get()));
        }
    }

    @Override
    public void trace(Supplier<String> content, Throwable error) {
        if (isTraceEnabled()) {
            withMetadata(() -> logger.trace(content.get(), error));
        }
    }

    @Override
    public void debug(CharSequence content) {
        if (isDebugEnabled()) {
            withMetadata(() -> logger.debug(toString(content)));
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (isDebugEnabled()) {
            withMetadata(() -> logger.debug(toString(content), error));
        }
    }

    @Override
    public void debug(Throwable error) {
        if (isDebugEnabled()) {
            withMetadata(() -> logger.debug("", error));
        }
    }

    @Override
    public void debug(Supplier<String> content) {
        if (isDebugEnabled()) {
            withMetadata(() -> logger.debug(content.get()));
        }
    }

    @Override
    public void debug(Supplier<String> content, Throwable error) {
        if (isDebugEnabled()) {
            withMetadata(() -> logger.debug(content.get(), error));
        }
    }

    @Override
    public void info(CharSequence content) {
        if (isInfoEnabled()) {
            withMetadata(() -> logger.info(toString(content)));
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (isInfoEnabled()) {
            withMetadata(() -> logger.info(toString(content), error));
        }
    }

    @Override
    public void info(Throwable error) {
        if (isInfoEnabled()) {
            withMetadata(() -> logger.info("", error));
        }
    }

    @Override
    public void info(Supplier<String> content) {
        if (isInfoEnabled()) {
            withMetadata(() -> logger.info(content.get()));
        }
    }

    @Override
    public void info(Supplier<String> content, Throwable error) {
        if (isInfoEnabled()) {
            withMetadata(() -> logger.info(content.get(), error));
        }
    }

    @Override
    public void warn(CharSequence content) {
        if (isWarnEnabled()) {
            withMetadata(() -> logger.warn(toString(content)));
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (isWarnEnabled()) {
            withMetadata(() -> logger.warn(toString(content), error));
        }
    }

    @Override
    public void warn(Throwable error) {
        if (isWarnEnabled()) {
            withMetadata(() -> logger.warn("", error));
        }
    }

    @Override
    public void warn(Supplier<String> content) {
        if (isWarnEnabled()) {
            withMetadata(() -> logger.warn(content.get()));
        }
    }

    @Override
    public void warn(Supplier<String> content, Throwable error) {
        if (isWarnEnabled()) {
            withMetadata(() -> logger.warn(content.get(), error));
        }
    }

    @Override
    public void error(CharSequence content) {
        if (isErrorEnabled()) {
            withMetadata(() -> logger.error(toString(content)));
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (isErrorEnabled()) {
            withMetadata(() -> logger.error(toString(content), error));
        }
    }

    @Override
    public void error(Throwable error) {
        if (isErrorEnabled()) {
            withMetadata(() -> logger.error("", error));
        }
    }

    @Override
    public void error(Supplier<String> content) {
        if (isErrorEnabled()) {
            withMetadata(() -> logger.error(content.get()));
        }
    }

    @Override
    public void error(Supplier<String> content, Throwable error) {
        if (isErrorEnabled()) {
            withMetadata(() -> logger.error(content.get(), error));
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
