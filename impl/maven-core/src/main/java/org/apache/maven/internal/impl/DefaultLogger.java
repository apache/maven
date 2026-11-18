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

import java.util.function.Supplier;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.logging.LogEventBuilder;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import static java.util.Objects.requireNonNull;

/**
 * Default {@link Logger} implementation backed by an SLF4J {@link org.slf4j.Logger}.
 */
public class DefaultLogger implements Logger {
    private static final LogEventBuilder NOP_BUILDER = new NopLogEventBuilder();

    private final org.slf4j.Logger logger;

    public DefaultLogger(org.slf4j.Logger logger) {
        this.logger = requireNonNull(logger, "logger cannot be null");
    }

    @Override
    @Nonnull
    public String name() {
        return logger.getName();
    }

    @Override
    public boolean isEnabled(@Nonnull LogLevel level) {
        requireNonNull(level, "level cannot be null");
        return switch (level) {
            case TRACE -> logger.isTraceEnabled();
            case DEBUG -> logger.isDebugEnabled();
            case INFO -> logger.isInfoEnabled();
            case WARN -> logger.isWarnEnabled();
            case ERROR -> logger.isErrorEnabled();
        };
    }

    @Override
    @Nonnull
    public LogEventBuilder atTrace() {
        return at(LogLevel.TRACE);
    }

    @Override
    @Nonnull
    public LogEventBuilder atDebug() {
        return at(LogLevel.DEBUG);
    }

    @Override
    @Nonnull
    public LogEventBuilder atInfo() {
        return at(LogLevel.INFO);
    }

    @Override
    @Nonnull
    public LogEventBuilder atWarn() {
        return at(LogLevel.WARN);
    }

    @Override
    @Nonnull
    public LogEventBuilder atError() {
        return at(LogLevel.ERROR);
    }

    @Override
    @Nonnull
    public LogEventBuilder at(@Nonnull LogLevel level) {
        if (!isEnabled(level)) {
            return NOP_BUILDER;
        }
        return new DefaultLogEventBuilder(logger, level);
    }

    @Override
    @Nonnull
    public Logger child(@Nonnull String name) {
        requireNonNull(name, "name cannot be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("child logger name cannot be empty");
        }
        String parentName = logger.getName();
        String childName = parentName.isEmpty() ? name : parentName + "." + name;
        return new DefaultLogger(LoggerFactory.getLogger(childName));
    }

    private static final class NopLogEventBuilder implements LogEventBuilder {
        @Override
        @Nonnull
        public LogEventBuilder withKeyValue(@Nonnull String key, String value) {
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withKeyValue(@Nonnull String key, long value) {
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withKeyValue(@Nonnull String key, boolean value) {
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withThrowable(Throwable t) {
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withProjectId(String projectId) {
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withMojoId(String mojoId) {
            return this;
        }

        @Override
        public void log(@Nonnull String message) {}

        @Override
        public void log(@Nonnull String format, Object arg) {}

        @Override
        public void log(@Nonnull String format, Object arg1, Object arg2) {}

        @Override
        public void log(@Nonnull String format, Object... args) {}

        @Override
        public void log(@Nonnull Supplier<String> messageSupplier) {}
    }

    private static final class DefaultLogEventBuilder implements LogEventBuilder {
        private final org.slf4j.Logger logger;
        private final LogLevel level;
        private Throwable throwable;
        private LoggingEventBuilder slf4jBuilder;

        DefaultLogEventBuilder(org.slf4j.Logger logger, LogLevel level) {
            this.logger = logger;
            this.level = level;
        }

        private LoggingEventBuilder ensureBuilder() {
            if (slf4jBuilder == null) {
                slf4jBuilder = switch (level) {
                    case TRACE -> logger.atTrace();
                    case DEBUG -> logger.atDebug();
                    case INFO -> logger.atInfo();
                    case WARN -> logger.atWarn();
                    case ERROR -> logger.atError();
                };
                if (throwable != null) {
                    slf4jBuilder.setCause(throwable);
                }
            }
            return slf4jBuilder;
        }

        @Override
        @Nonnull
        public LogEventBuilder withKeyValue(@Nonnull String key, String value) {
            ensureBuilder().addKeyValue(key, value);
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withKeyValue(@Nonnull String key, long value) {
            ensureBuilder().addKeyValue(key, value);
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withKeyValue(@Nonnull String key, boolean value) {
            ensureBuilder().addKeyValue(key, value);
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withThrowable(Throwable t) {
            if (slf4jBuilder != null) {
                slf4jBuilder.setCause(t);
            } else {
                this.throwable = t;
            }
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withProjectId(String projectId) {
            return this;
        }

        @Override
        @Nonnull
        public LogEventBuilder withMojoId(String mojoId) {
            return this;
        }

        @Override
        public void log(@Nonnull String message) {
            if (slf4jBuilder != null) {
                slf4jBuilder.log(message);
                return;
            }
            if (throwable != null) {
                switch (level) {
                    case TRACE -> logger.trace(message, throwable);
                    case DEBUG -> logger.debug(message, throwable);
                    case INFO -> logger.info(message, throwable);
                    case WARN -> logger.warn(message, throwable);
                    case ERROR -> logger.error(message, throwable);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            } else {
                switch (level) {
                    case TRACE -> logger.trace(message);
                    case DEBUG -> logger.debug(message);
                    case INFO -> logger.info(message);
                    case WARN -> logger.warn(message);
                    case ERROR -> logger.error(message);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            }
        }

        @Override
        public void log(@Nonnull String format, Object arg) {
            if (slf4jBuilder != null) {
                slf4jBuilder.log(format, arg);
                return;
            }
            if (throwable != null) {
                switch (level) {
                    case TRACE -> logger.trace(format, arg, throwable);
                    case DEBUG -> logger.debug(format, arg, throwable);
                    case INFO -> logger.info(format, arg, throwable);
                    case WARN -> logger.warn(format, arg, throwable);
                    case ERROR -> logger.error(format, arg, throwable);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            } else {
                switch (level) {
                    case TRACE -> logger.trace(format, arg);
                    case DEBUG -> logger.debug(format, arg);
                    case INFO -> logger.info(format, arg);
                    case WARN -> logger.warn(format, arg);
                    case ERROR -> logger.error(format, arg);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            }
        }

        @Override
        public void log(@Nonnull String format, Object arg1, Object arg2) {
            if (slf4jBuilder != null) {
                slf4jBuilder.log(format, arg1, arg2);
                return;
            }
            if (throwable != null) {
                switch (level) {
                    case TRACE -> logger.trace(format, arg1, arg2, throwable);
                    case DEBUG -> logger.debug(format, arg1, arg2, throwable);
                    case INFO -> logger.info(format, arg1, arg2, throwable);
                    case WARN -> logger.warn(format, arg1, arg2, throwable);
                    case ERROR -> logger.error(format, arg1, arg2, throwable);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            } else {
                switch (level) {
                    case TRACE -> logger.trace(format, arg1, arg2);
                    case DEBUG -> logger.debug(format, arg1, arg2);
                    case INFO -> logger.info(format, arg1, arg2);
                    case WARN -> logger.warn(format, arg1, arg2);
                    case ERROR -> logger.error(format, arg1, arg2);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            }
        }

        @Override
        public void log(@Nonnull String format, Object... args) {
            if (slf4jBuilder != null) {
                slf4jBuilder.log(format, args);
                return;
            }
            if (throwable != null) {
                Object[] withThrowable = new Object[args.length + 1];
                System.arraycopy(args, 0, withThrowable, 0, args.length);
                withThrowable[args.length] = throwable;
                switch (level) {
                    case TRACE -> logger.trace(format, withThrowable);
                    case DEBUG -> logger.debug(format, withThrowable);
                    case INFO -> logger.info(format, withThrowable);
                    case WARN -> logger.warn(format, withThrowable);
                    case ERROR -> logger.error(format, withThrowable);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            } else {
                switch (level) {
                    case TRACE -> logger.trace(format, args);
                    case DEBUG -> logger.debug(format, args);
                    case INFO -> logger.info(format, args);
                    case WARN -> logger.warn(format, args);
                    case ERROR -> logger.error(format, args);
                    default -> throw new IllegalStateException("Unexpected level: " + level);
                }
            }
        }

        @Override
        public void log(@Nonnull Supplier<String> messageSupplier) {
            requireNonNull(messageSupplier, "messageSupplier cannot be null");
            log(messageSupplier.get());
        }
    }
}
