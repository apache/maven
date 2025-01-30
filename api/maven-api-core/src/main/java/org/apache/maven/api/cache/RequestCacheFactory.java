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
 * Factory interface for creating new RequestCache instances.
 * Implementations should handle the creation and configuration of cache instances
 * based on the current Maven session and environment.
 *
 * @since 4.0.0
 * @see RequestCache
 */
@Experimental
public interface RequestCacheFactory {

    /**
     * Creates a new RequestCache instance.
     * The created cache should be configured according to the current Maven session
     * and environment settings.
     *
     * @return A new RequestCache instance
     */
    RequestCache createCache();
}
