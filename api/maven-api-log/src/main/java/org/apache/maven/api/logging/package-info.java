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
 * plugins and extensions.
 *
 * <h2>Core types</h2>
 * <dl>
 *   <dt>{@link org.apache.maven.api.logging.Logger}</dt>
 *   <dd>The primary logging interface injected by the container. Supports a fluent builder
 *   API ({@code atInfo()...log(...)}) as well as SLF4J-style convenience methods
 *   ({@code info(...)}, {@code warn(...)}, etc.).</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LogEventBuilder}</dt>
 *   <dd>Fluent builder for a single log event, obtained from {@code Logger.at(LogLevel)}.
 *   When the target level is disabled, returns a no-op singleton (zero allocations).</dd>
 *
 *   <dt>{@link org.apache.maven.api.logging.LogLevel}</dt>
 *   <dd>Ordered severity enumeration: {@code TRACE}, {@code DEBUG}, {@code INFO},
 *   {@code WARN}, {@code ERROR}.</dd>
 * </dl>
 *
 * @since 4.0.0
 */
package org.apache.maven.api.logging;
