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
package org.apache.maven.api.build.report;

import java.time.Instant;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Details about a build failure.
 *
 * @since 4.1.0
 * @see BuildReport#failures()
 */
@Experimental
public interface FailureReport {

    /**
     * The GAV of the module where the failure occurred
     * ({@code groupId:artifactId:version}).
     *
     * @return the module identifier, never {@code null}
     */
    @Nonnull
    String module();

    /**
     * The mojo that failed, formatted as {@code artifactId:version:goal}
     * (e.g. {@code "maven-compiler-plugin:3.15.0:compile"}).
     *
     * @return the mojo identifier, or {@code null} if the failure was not mojo-specific
     */
    @Nullable
    String mojo();

    /**
     * When the failure occurred (wall-clock time).
     *
     * @return the failure instant, never {@code null}
     */
    @Nonnull
    Instant timestamp();

    /**
     * The simple class name of the root cause exception
     * (e.g. {@code "MojoFailureException"}, {@code "LifecycleExecutionException"}).
     * <p>
     * Useful for programmatic triage — tools can pattern-match on known
     * exception types without parsing the message.
     *
     * @return the exception type name, or {@code null} if unavailable
     */
    @Nullable
    String exceptionType();

    /**
     * The exception message.
     *
     * @return the error message, never {@code null}
     */
    @Nonnull
    String message();

    /**
     * The exception stack trace, truncated to a reasonable length.
     *
     * @return the stack trace string, or {@code null} if unavailable
     */
    @Nullable
    String stackTrace();
}
