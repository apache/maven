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
 * Partial cache configuration that allows specifying only scope or reference type.
 * Used for merging configurations from multiple selectors.
 * 
 * @param scope the cache retention scope (nullable)
 * @param referenceType the reference type to use for cache entries (nullable)
 */
public record PartialCacheConfig(CacheRetention scope, Cache.ReferenceType referenceType) {
    
    /**
     * Creates a partial configuration with only scope specified.
     */
    public static PartialCacheConfig withScope(CacheRetention scope) {
        return new PartialCacheConfig(scope, null);
    }
    
    /**
     * Creates a partial configuration with only reference type specified.
     */
    public static PartialCacheConfig withReferenceType(Cache.ReferenceType referenceType) {
        return new PartialCacheConfig(null, referenceType);
    }
    
    /**
     * Creates a complete partial configuration with both scope and reference type.
     */
    public static PartialCacheConfig complete(CacheRetention scope, Cache.ReferenceType referenceType) {
        return new PartialCacheConfig(scope, referenceType);
    }
    
    /**
     * Merges this configuration with another, with this configuration taking precedence
     * for non-null values.
     * 
     * @param other the other configuration to merge with
     * @return a new merged configuration
     */
    public PartialCacheConfig mergeWith(PartialCacheConfig other) {
        if (other == null) {
            return this;
        }
        
        CacheRetention mergedScope = this.scope != null ? this.scope : other.scope;
        Cache.ReferenceType mergedRefType = this.referenceType != null ? this.referenceType : other.referenceType;
        
        return new PartialCacheConfig(mergedScope, mergedRefType);
    }
    
    /**
     * Converts this partial configuration to a complete CacheConfig, using defaults for missing values.
     * 
     * @return a complete CacheConfig
     */
    public CacheConfig toComplete() {
        CacheRetention finalScope = scope != null ? scope : CacheRetention.REQUEST_SCOPED;
        Cache.ReferenceType finalRefType = referenceType != null ? referenceType : Cache.ReferenceType.SOFT;
        
        return new CacheConfig(finalScope, finalRefType);
    }
    
    /**
     * Checks if this configuration is empty (both values are null).
     */
    public boolean isEmpty() {
        return scope == null && referenceType == null;
    }
    
    /**
     * Checks if this configuration is complete (both values are non-null).
     */
    public boolean isComplete() {
        return scope != null && referenceType != null;
    }
}
