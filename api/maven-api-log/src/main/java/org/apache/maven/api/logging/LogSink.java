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

/**
 * Central dispatch point for log events.
 * <p>
 * All logging paths — the fluent {@link Log} API, SLF4J
 * ({@code MavenSimpleLogger}), and JUL ({@code MavenJulHandler}) — converge
 * here. A single {@code LogSink} implementation controls level filtering and
 * routing for all sources.
 * <p>
 * Implementations are installed at Maven startup and remain in place for the
 * lifetime of the build. The sink is not DI-managed; it is set via a static
 * setter in the logging infrastructure.
 *
 * @since 4.1.0
 */
@Experimental
public interface LogSink {

    /**
     * Dispatches a structured log event to all configured outputs.
     *
     * @param event the log event, never {@code null}
     */
    void accept(@Nonnull LogEvent event);

    /**
     * Returns {@code true} if this sink needs source metadata
     * (caller class name, method name) to be resolved before
     * {@link #accept(LogEvent)} is called.
     * <p>
     * Source metadata resolution involves a {@link StackWalker} walk and
     * should be skipped when not required.
     *
     * @return {@code true} if metadata is needed
     */
    default boolean needsMetadata() {
        return false;
    }
}
