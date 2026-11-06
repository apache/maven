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
import org.apache.maven.api.annotations.Provider;

/**
 * Central entry point for Maven's logging system.
 * <p>
 * Provides access to loggers, the active {@link LogSink}, level configuration,
 * and severity tracking for {@code --fail-on-severity}.
 * <p>
 * The singleton is resolved lazily on the first {@link #get()} call using
 * {@link ServiceLoader}. If no provider is found, a buffering fallback is
 * installed that accumulates events until a real implementation is set via
 * {@link #set(LoggerManager)}.
 *
 * @since 4.2.0
 */
@Experimental
@Provider
public interface LoggerManager {

    /**
     * Returns a {@link Logger} instance for the given name.
     *
     * @since 4.2.0
     */
    @Nonnull
    Logger getLogger(@Nonnull String name);

    /**
     * Returns a {@link Logger} instance for the given class.
     *
     * @since 4.2.0
     */
    @Nonnull
    default Logger getLogger(@Nonnull Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Returns the active log sink, or {@code null} if none is installed.
     *
     * @since 4.2.0
     */
    @Nullable
    LogSink getLogSink();

    /**
     * Installs or removes the active log sink.
     *
     * @since 4.2.0
     */
    void setLogSink(@Nullable LogSink sink);

    /**
     * Returns whether logging at {@code level} is enabled for the named logger.
     *
     * @since 4.2.0
     */
    boolean isLevelEnabled(@Nonnull String loggerName, @Nonnull LogLevel level);

    /**
     * Sets the root logger level (typically from {@code -X}, {@code -q} CLI flags).
     * The new level takes effect immediately.
     *
     * @since 4.2.0
     */
    void setRootLoggerLevel(@Nonnull LogLevel level);

    /**
     * Reloads level configuration and resets severity tracking state.
     *
     * @since 4.2.0
     */
    void reconfigure();

    /**
     * Returns {@code true} if any log event at or above {@code level}
     * has been emitted since the last {@link #reconfigure()}.
     * <p>
     * Used by {@code --fail-on-severity}: the CLI stores the threshold
     * and checks at build end.
     *
     * @since 4.2.0
     */
    boolean hasReached(@Nonnull LogLevel level);

    /**
     * Registers a classloader as belonging to a specific plugin.
     * Used by the plugin manager to enable attribution of log events
     * to the plugin that produced them.
     *
     * @param classLoader the plugin's classloader
     * @param pluginId the plugin identifier (e.g. {@code "groupId:artifactId:version"})
     * @since 4.2.0
     */
    default void registerPluginClassLoader(@Nonnull ClassLoader classLoader, @Nonnull String pluginId) {}

    /**
     * Returns the plugin identifier for the given classloader, or {@code null}
     * if the classloader is not a registered plugin classloader.
     *
     * @param classLoader the classloader to look up
     * @return the plugin identifier, or {@code null}
     * @since 4.2.0
     */
    @Nullable
    default String getPluginId(@Nullable ClassLoader classLoader) {
        return null;
    }

    /**
     * Returns the global {@code LoggerManager} instance.
     * <p>
     * On the first call, synchronously discovers an implementation via
     * {@link ServiceLoader}. If none is found, a buffering fallback is
     * installed that accumulates events until a real implementation is
     * set via {@link #set(LoggerManager)}.
     *
     * @return the global manager, never {@code null}
     * @since 4.2.0
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
     * <p>
     * If the previous manager was the bootstrap buffering manager,
     * any accumulated events are drained to the new manager's
     * {@link LogSink}.
     *
     * @param manager the manager to install, or {@code null} to clear
     * @since 4.2.0
     */
    static void set(LoggerManager manager) {
        LoggerManager old = Holder.instance;
        Holder.instance = manager;
        if (old instanceof BufferingLoggerManager buffering && manager != null && old != manager) {
            LogSink sink = manager.getLogSink();
            if (sink != null) {
                buffering.drainTo(sink);
            }
        }
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
                // ServiceLoader failure — fall through to buffering fallback
            } finally {
                DISCOVERING.remove();
            }

            mgr = new BufferingLoggerManager();
            instance = mgr;
            return mgr;
        }
    }
}
