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
package org.apache.maven.impl.cache;

import org.apache.maven.api.cache.CacheRetention;

/**
 * Configuration for cache behavior including scope and reference type.
 * 
 * @param scope the cache retention scope
 * @param referenceType the reference type to use for cache entries
 */
public record CacheConfig(CacheRetention scope, Cache.ReferenceType referenceType) {
    
    /**
     * Default cache configuration with REQUEST_SCOPED and SOFT reference type.
     */
    public static final CacheConfig DEFAULT = new CacheConfig(CacheRetention.REQUEST_SCOPED, Cache.ReferenceType.SOFT);
    
    /**
     * Creates a cache configuration with the specified scope and default SOFT reference type.
     */
    public static CacheConfig withScope(CacheRetention scope) {
        return new CacheConfig(scope, Cache.ReferenceType.SOFT);
    }
    
    /**
     * Creates a cache configuration with the specified reference type and default REQUEST_SCOPED scope.
     */
    public static CacheConfig withReferenceType(Cache.ReferenceType referenceType) {
        return new CacheConfig(CacheRetention.REQUEST_SCOPED, referenceType);
    }
}
