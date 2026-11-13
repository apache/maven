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
package org.apache.maven.api.build.incremental;

import java.nio.file.Path;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Provider;

/**
 * Represents a build resource (input or output file) tracked by the incremental build context.
 *
 * <p>This is the base interface for both {@link Input} and {@link Output}. Every resource has
 * a file {@link #getPath() path} and a change {@link #getStatus() status}.</p>
 *
 * <p>Diagnostic messages (compiler errors, warnings) should be reported through
 * {@link org.apache.maven.api.services.DiagnosticReporter DiagnosticReporter} rather than
 * attached to individual resources.</p>
 *
 * @since 4.1.0
 * @see Input
 * @see Output
 * @see Status
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
}
