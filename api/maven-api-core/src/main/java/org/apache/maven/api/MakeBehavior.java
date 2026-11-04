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

import org.apache.maven.api.annotations.Experimental;

/**
 * The reactor make behavior, controlling whether upstream and/or downstream
 * dependencies of selected projects are also built.
 *
 * @since 4.1.0
 * @see BuildInvocation#makeBehavior()
 */
@Experimental
public enum MakeBehavior {

    /**
     * Also build upstream dependencies of selected projects ({@code -am} / {@code --also-make}).
     */
    UPSTREAM,

    /**
     * Also build downstream dependents of selected projects ({@code -amd} / {@code --also-make-dependents}).
     */
    DOWNSTREAM,

    /**
     * Build both upstream dependencies and downstream dependents ({@code -am -amd}).
     */
    BOTH;

    /**
     * Returns the enum constant matching the given v3 string constant.
     *
     * @param value the string value (e.g. {@code "make-upstream"}, {@code "make-downstream"}, {@code "make-both"})
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static MakeBehavior fromString(String value) {
        return switch (value) {
            case "make-upstream" -> UPSTREAM;
            case "make-downstream" -> DOWNSTREAM;
            case "make-both" -> BOTH;
            default -> throw new IllegalArgumentException("Unknown make behavior: " + value);
        };
    }
}
