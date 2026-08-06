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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.services.BuilderProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class DefaultLog implements Log {

    /**
     * Thread-local flag set by {@link #problem(BuilderProblem)} around the SLF4J call
     * so that {@code BuildReportCollector} can skip auto-promotion for messages that
     * are already reported as structured problems. This avoids double-counting.
     */
    public static final ThreadLocal<Boolean> STRUCTURED_PROBLEM_ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final Logger logger;
    private final Consumer<BuilderProblem> problemSink;

    public DefaultLog(Logger logger) {
        this(logger, p -> {});
    }

    public DefaultLog(Logger logger, Consumer<BuilderProblem> problemSink) {
        this.logger = requireNonNull(logger);
        this.problemSink = requireNonNull(problemSink);
    }

    @Override
    public void debug(CharSequence content) {
        if (isDebugEnabled()) {
            logger.debug(toString(content));
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (isDebugEnabled()) {
            logger.debug(toString(content), error);
        }
    }

    @Override
    public void debug(Throwable error) {
        logger.debug("", error);
    }

    @Override
    public void debug(Supplier<String> content) {
        if (isDebugEnabled()) {
            logger.debug(content.get());
        }
    }

    @Override
    public void debug(Supplier<String> content, Throwable error) {
        if (isDebugEnabled()) {
            logger.debug(content.get(), error);
        }
    }

    @Override
    public void info(CharSequence content) {
        if (isInfoEnabled()) {
            logger.info(toString(content));
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (isInfoEnabled()) {
            logger.info(toString(content), error);
        }
    }

    @Override
    public void info(Throwable error) {
        logger.info("", error);
    }

    @Override
    public void info(Supplier<String> content) {
        if (isInfoEnabled()) {
            logger.info(content.get());
        }
    }

    @Override
    public void info(Supplier<String> content, Throwable error) {
        if (isInfoEnabled()) {
            logger.info(content.get(), error);
        }
    }

    @Override
    public void warn(CharSequence content) {
        if (isWarnEnabled()) {
            logger.warn(toString(content));
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (isWarnEnabled()) {
            logger.warn(toString(content), error);
        }
    }

    @Override
    public void warn(Throwable error) {
        logger.warn("", error);
    }

    @Override
    public void warn(Supplier<String> content) {
        if (isWarnEnabled()) {
            logger.warn(content.get());
        }
    }

    @Override
    public void warn(Supplier<String> content, Throwable error) {
        if (isWarnEnabled()) {
            logger.warn(content.get(), error);
        }
    }

    @Override
    public void error(CharSequence content) {
        if (isErrorEnabled()) {
            logger.error(toString(content));
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (isErrorEnabled()) {
            logger.error(toString(content), error);
        }
    }

    @Override
    public void error(Throwable error) {
        logger.error("", error);
    }

    @Override
    public void error(Supplier<String> content) {
        if (isErrorEnabled()) {
            logger.error(content.get());
        }
    }

    @Override
    public void error(Supplier<String> content, Throwable error) {
        if (isErrorEnabled()) {
            logger.error(content.get(), error);
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
        requireNonNull(name, "child logger name must not be null");
        return new DefaultLog(LoggerFactory.getLogger(logger.getName() + "." + name), problemSink);
    }

    @Override
    public void problem(BuilderProblem problem) {
        requireNonNull(problem, "problem must not be null");
        // Report to the diagnostic collector for dedup and end-of-build summary
        problemSink.accept(problem);
        // Also log the message at the appropriate level for console output.
        // Set the thread-local flag so BuildReportCollector skips auto-promotion
        // (avoiding double-counting as both a structured problem and a synthetic one).
        STRUCTURED_PROBLEM_ACTIVE.set(Boolean.TRUE);
        try {
            String message = problem.getMessage();
            switch (problem.getSeverity()) {
                case FATAL, ERROR -> logger.error(message);
                case WARNING -> logger.warn(message);
                default -> logger.info(message);
            }
        } finally {
            STRUCTURED_PROBLEM_ACTIVE.set(Boolean.FALSE);
        }
    }

    private String toString(CharSequence content) {
        return content != null ? content.toString() : "";
    }
}
