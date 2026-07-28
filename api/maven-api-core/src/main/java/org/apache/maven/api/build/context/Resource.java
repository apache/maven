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
package org.apache.maven.api.build.context;

import java.nio.file.Path;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.Provider;

/**
 * Represents a build resource (input or output file) tracked by the incremental build context.
 *
 * @since 4.0.0
 */
@Experimental
@NotThreadSafe
@Provider
public interface Resource {

    /**
     * {@return the path of this resource}
     */
    @Nonnull
    Path getPath();

    /**
     * {@return the change status of this resource relative to the previous build}
     */
    @Nonnull
    Status getStatus();

    /**
     * Attaches a diagnostic message to this resource at the given source location.
     *
     * @param line     the 1-based line number, or {@code 0} if unknown
     * @param column   the 1-based column number, or {@code 0} if unknown
     * @param message  the human-readable message text
     * @param severity the severity level
     * @param cause    the underlying cause, or {@code null}
     */
    void addMessage(
            int line, int column, @Nonnull String message, @Nonnull Severity severity, @Nullable Throwable cause);
}
