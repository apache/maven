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
package org.apache.maven.rtinfo;

/**
 * Provides information about the current Maven runtime.
 *
 * @since 3.0.2
 */
public interface RuntimeInformation {

    /**
     * Retrieves the current Maven version, for example "3.0.2".
     *
     * @return The current Maven version or an empty string if unknown, never {@code null}.
     */
    String getMavenVersion();

    /**
     * Checks whether the current Maven runtime matches the specified version range. A version range can either use the
     * usual mathematical syntax "[2.0.10,2.1.0),[3.0,)" or use a single version "2.2.1". The latter is a short form for
     * "[2.2.1,)", i.e. denotes the minimum version required.
     *
     * @param versionRange The version range to match the current Maven runtime against, must not be {@code null}.
     * @return {@code true} if the current Maven runtime matches the specified version range, {@code false} otherwise.
     * @throws IllegalArgumentException If the specified version range is {@code null}, empty or otherwise not a valid
     *             version specification.
     */
    boolean isMavenVersion(String versionRange);
}
