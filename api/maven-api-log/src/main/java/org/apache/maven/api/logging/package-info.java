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

/**
 * Maven unified logging API — a first-class, SLF4J-free logging facade for
 * plugins, extensions, and Maven internals.
 *
 * <h2>Overview</h2>
 * <p>
 * This package replaces the older {@code org.apache.maven.api.plugin.Log}
 * plugin facade and the SLF4J-backed CLI logger with a single, consistent
 * API that works uniformly across mojos, extensions, and Maven's own
 * internal components.
 * </p>
 *
 * <h2>Core types</h2>
 * <dl>
 *   <dt>{@link org.apache.maven.api.logging.Logger}</dt>
 *   <dd>The primary logging interface. Obtained via {@link org.apache.maven.api.logging.Logger#of(Class)}
 *   or injected by the container. Supports a fluent builder API ({@code atInfo()…log(…)})
 *   as well as SLF4J-style convenience methods ({@code info(…)}, {@code warn(…)}, etc.).
 *   Instances are safe to hold in static or instance fields — context such as
 *   {@code projectId} and {@code mojoId} is resolved from a per-thread
 *   {@link org.apache.maven.api.logging.LogSink} at dispatch time.</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LogEventBuilder}</dt>
 *   <dd>Fluent builder for a single log event. Obtained from {@code Logger.atLevel()}.
 *   Accumulates level, message template, arguments, throwable, and key-value pairs
 *   before dispatching via {@code log(…)}. When the target level is disabled, returns
 *   a no-op singleton (zero allocations).</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LogEvent}</dt>
 *   <dd>Immutable record of a single log event. Carries the logger name, level, raw
 *   message template, arguments (lazy formatting), throwable, and optional
 *   {@code projectId} / {@code mojoId} context. Passed to {@link org.apache.maven.api.logging.LogSink}
 *   for consumption.</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LogSink}</dt>
 *   <dd>Consumer of {@link org.apache.maven.api.logging.LogEvent}s. This is the primary
 *   extension point for embedders and build tools: implement {@code LogSink} and register
 *   it via {@link org.apache.maven.api.logging.LoggerManager#setLogSink(LogSink)} (or via
 *   {@link java.util.ServiceLoader} discovery) to redirect all Maven log output to any
 *   destination — a UI, a structured log store, a test buffer, etc.</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LoggerManager}</dt>
 *   <dd>Central control surface: obtains loggers, installs the active sink, configures
 *   root and per-logger levels, and tracks the highest severity reached (for
 *   {@code --fail-on-severity}). Accessed via {@link org.apache.maven.api.logging.LoggerManager#get()},
 *   which is set up by Maven before any logging occurs.</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LogLevel}</dt>
 *   <dd>Ordered severity enumeration: {@code TRACE}, {@code DEBUG}, {@code INFO},
 *   {@code WARN}, {@code ERROR}.</dd>
 * </dl>
 *
 * <h2>Getting a logger</h2>
 * <p>
 * The typical pattern for plugin and extension code (where the container has already
 * initialized the {@link org.apache.maven.api.logging.LoggerManager}) is:
 * </p>
 * <pre>{@code
 * private final Logger log = Logger.of(MyComponent.class);
 * }</pre>
 * <p>
 * <strong>Note:</strong> unlike {@code System.getLogger()}, {@code Logger.of()} throws
 * {@link IllegalStateException} if called before
 * {@link org.apache.maven.api.logging.LoggerManager#set(LoggerManager)} has been
 * invoked. Static field initializers run at class-load time, which can precede Maven's
 * bootstrap. Prefer instance-field initialization (as above) or lazy initialization
 * for classes that may be loaded during early bootstrap:
 * </p>
 * <pre>{@code
 * // Safe for early-bootstrap classes:
 * private Logger log() {
 *     return Logger.of(MyComponent.class);
 * }
 * }</pre>
 * <p>
 * Framework code that does not have a class reference can use the string overload:
 * </p>
 * <pre>{@code
 * Logger log = Logger.of("org.apache.maven.my-component");
 * }</pre>
 *
 * <h2>Logging events</h2>
 * <p>Fluent API (preferred — enables lazy formatting and structured key-value pairs):</p>
 * <pre>{@code
 * log.atInfo()
 *    .withKeyValue("artifact", artifact.getId())
 *    .log("Resolved {} files", count);
 * }</pre>
 * <p>Convenience methods (SLF4J-style shorthand):</p>
 * <pre>{@code
 * log.info("Compiling {} source files", count);
 * log.warn("Deprecated key: {}", key, e);
 * }</pre>
 *
 * <h2>Embedder integration</h2>
 * <p>
 * To redirect Maven log output, implement {@link org.apache.maven.api.logging.LogSink}
 * and either:
 * </p>
 * <ul>
 *   <li>Register it via {@code ServiceLoader} — place a
 *       {@code META-INF/services/org.apache.maven.api.logging.LogSink} file on the
 *       classpath pointing to your implementation. Maven discovers it before any
 *       logging occurs.</li>
 *   <li>Or call {@link org.apache.maven.api.logging.LoggerManager#setLogSink(LogSink)}
 *       programmatically during embedder setup.</li>
 * </ul>
 *
 * @since 4.1.0
 */
package org.apache.maven.api.logging;
