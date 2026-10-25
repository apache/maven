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

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Central entry point for Maven's logging system.
 * <p>
 * Provides access to loggers, the active {@link LogSink}, level configuration,
 * and severity tracking for {@code --fail-on-severity}.
 * <p>
 * The singleton is resolved lazily on the first {@link #get()} call using
 * {@link ServiceLoader}. If no provider is found, a no-op fallback is
 * installed. Embedders and tests can override with {@link #set(LoggerManager)}.
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
     * The new level takes effect immediately.
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
     * Returns the global {@code LoggerManager} instance.
     * <p>
     * On the first call, synchronously discovers an implementation via
     * {@link ServiceLoader}. If none is found, a no-op fallback is installed.
     * The result is cached — subsequent calls return immediately.
     *
     * @return the global manager, never {@code null}
     */
    @Nonnull
    static LoggerManager get() {
        LoggerManager mgr = Holder.instance;
        if (mgr != null) {
            return mgr;
        }
        return Holder.discover();
    }

    /**
     * Installs the global {@code LoggerManager} instance.
     * <p>
     * Typically used by embedders or tests to override the
     * {@link ServiceLoader}-discovered implementation.
     *
     * @param manager the manager to install, or {@code null} to clear
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

        private static final ThreadLocal<Boolean> DISCOVERING = new ThreadLocal<>();

        static synchronized LoggerManager discover() {
            LoggerManager mgr = instance;
            if (mgr != null) {
                return mgr;
            }

            // Re-entrancy guard: if ServiceLoader loading triggers logging,
            // return a no-op manager to avoid infinite recursion.
            // Do NOT write to `instance` here — the outer discover() call
            // will install the real implementation once ServiceLoader finishes.
            if (DISCOVERING.get() == Boolean.TRUE) {
                return new NopLoggerManager();
            }

            DISCOVERING.set(Boolean.TRUE);
            try {
                for (LoggerManager discovered :
                        ServiceLoader.load(LoggerManager.class, LoggerManager.class.getClassLoader())) {
                    instance = discovered;
                    return discovered;
                }
            } catch (Exception | ServiceConfigurationError ignored) {
                // ServiceLoader failure — fall through to no-op
            } finally {
                DISCOVERING.remove();
            }

            mgr = new NopLoggerManager();
            instance = mgr;
            return mgr;
        }
    }
}
