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
package org.apache.maven.artifact.repository;

/**
 * Caches auxiliary data used during repository access like already processed metadata. The data in the cache is meant
 * for exclusive consumption by the repository system and is opaque to the cache implementation.
 *
 * @author Benjamin Bentmann
 */
@Deprecated
//
// Used by Tycho and will break users and force them to upgrade to Maven 3.1 so we should really leave
// this here, possibly indefinitely.
//
public interface RepositoryCache {

    /**
     * Puts the specified data into the cache. <strong>Warning:</strong> The cache will directly save the provided
     * reference. If the cached data is mutable, i.e. could be modified after being put into the cache, the caller is
     * responsible for creating a copy of the original data and store the copy in the cache.
     *
     * @param request The repository request from which this cache was retrieved, must not be {@code null}.
     * @param key The key to use associate the data with, must not be {@code null}.
     * @param data The data to store in the cache, may be {@code null}.
     */
    void put(RepositoryRequest request, Object key, Object data);

    /**
     * Gets the specified data from the cache. <strong>Warning:</strong> The cache will directly return the saved
     * reference. If the cached data is to be modified after its retrieval, the caller is responsible to create a copy
     * of the returned data and use this instead of the cache record.
     *
     * @param request The repository request from which this cache was retrieved, must not be {@code null}.
     * @param key The key to use for lookup of the data, must not be {@code null}.
     * @return The requested data or {@code null} if none was present in the cache.
     */
    Object get(RepositoryRequest request, Object key);
}
