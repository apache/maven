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
package org.apache.maven.api.services.model;

/**
 * Provide a service of {@link UrlNormalizer} that simplifies URL strings by removing parent directory
 * references ("/../") and collapsing path segments. This implementation performs purely
 * string-based normalization without full URL parsing or validation.
 *
 * <p>The normalization process iteratively removes "/../" segments by eliminating the preceding path segment,
 * effectively resolving relative path traversals.
 *
 * <p>Note that this implementation does not guarantee that the resulting URL is valid or reachable; it simply
 * produces a more canonical representation of the input string.
 *
 */
public interface UrlNormalizer {

    /**
     * Normalizes the specified URL.
     *
     * @param url The URL to normalize, may be {@code null}.
     * @return The normalized URL or {@code null} if the input was {@code null}.
     */
    String normalize(String url);
}
