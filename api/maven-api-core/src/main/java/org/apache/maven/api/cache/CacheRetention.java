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
package org.apache.maven.api.cache;

import org.apache.maven.api.annotations.Experimental;

/**
 * Enumeration defining different retention periods for cached data.
 * Each value represents a specific scope and lifetime for cached items.
 *
 * @since 4.0.0
 */
@Experimental
public enum CacheRetention {
    /**
     * Data should be persisted across Maven invocations.
     * Suitable for:
     * - Dependency resolution results
     * - Compilation outputs
     * - Downloaded artifacts
     */
    PERSISTENT,

    /**
     * Data should be retained for the duration of the current Maven session.
     * Suitable for:
     * - Build-wide configuration
     * - Project model caching
     * - Inter-module metadata
     */
    SESSION_SCOPED,

    /**
     * Data should only be retained for the current build request.
     * Suitable for:
     * - Plugin execution results
     * - Temporary build artifacts
     * - Phase-specific data
     */
    REQUEST_SCOPED,

    /**
     * Caching should be disabled for this data.
     * Suitable for:
     * - Sensitive information
     * - Non-deterministic operations
     * - Debug or development data
     */
    DISABLED
}
