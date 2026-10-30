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
package org.apache.maven.api;

import java.util.Locale;

import org.apache.maven.api.annotations.Experimental;

/**
 * The reactor failure behavior, controlling how Maven reacts when a module
 * in a multi-module build fails.
 *
 * @since 4.1.0
 * @see BuildInvocation#reactorFailureBehavior()
 */
@Experimental
public enum ReactorFailureBehavior {

    /**
     * Stop at first failure ({@code -ff}, default).
     */
    FAIL_FAST,

    /**
     * Build all modules, report failures at end ({@code -fae}).
     */
    FAIL_AT_END,

    /**
     * Always exit with success ({@code -fn}).
     */
    FAIL_NEVER;

    /**
     * Returns the enum constant matching the given string (case-insensitive).
     *
     * @param value the string value (e.g. {@code "FAIL_FAST"})
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static ReactorFailureBehavior fromString(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
