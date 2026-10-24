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
package org.apache.maven.api.logging;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Central entry point for Maven's logging system.
 * <p>
 * Provides access to loggers, the active {@link LogSink}, level configuration,
 * and severity tracking for {@code --fail-on-severity}. A single instance is
 * registered at startup via {@link #set(LoggerManager)} and accessed statically
 * via {@link #get()}.
 *
 * @since 4.1.0
 */
@Experimental
public interface LoggerManager {

    /**
     * Returns a {@link Logger} instance for the given name.
     */
    @Nonnull
    Logger getLogger(@Nonnull String name);

    /**
     * Returns a {@link Logger} instance for the given class.
     */
    @Nonnull
    default Logger getLogger(@Nonnull Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Returns the active log sink, or {@code null} if none is installed.
     */
    @Nullable
    LogSink getLogSink();

    /**
     * Installs or removes the active log sink.
     */
    void setLogSink(@Nullable LogSink sink);

    /**
     * Returns whether logging at {@code level} is enabled for the named logger.
     */
    boolean isLevelEnabled(@Nonnull String loggerName, @Nonnull LogLevel level);

    /**
     * Sets the root logger level (typically from {@code -X}, {@code -q} CLI flags).
     * Takes effect after {@link #reconfigure()}.
     */
    void setRootLoggerLevel(@Nonnull LogLevel level);

    /**
     * Reloads level configuration and resets severity tracking state.
     */
    void reconfigure();

    /**
     * Returns {@code true} if any log event at or above {@code level}
     * has been emitted since the last {@link #reconfigure()}.
     * <p>
     * Used by {@code --fail-on-severity}: the CLI stores the threshold
     * and checks at build end.
     */
    boolean hasReached(@Nonnull LogLevel level);

    /**
     * Returns the global {@code LogManager} instance.
     */
    static LoggerManager get() {
        return Holder.instance;
    }

    /**
     * Sets the global {@code LogManager} instance.
     */
    static void set(LoggerManager manager) {
        Holder.instance = manager;
    }

    /**
     * @hidden
     */
    final class Holder {
        private Holder() {}

        static volatile LoggerManager instance;
    }
}
