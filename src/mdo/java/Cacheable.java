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
package ${package};

/**
 * Interface for objects that can be cached in the CacheManager.
 * Implementing classes should ensure that cacheEquals() and cacheIdentityHash()
 * use all relevant fields for determining cache identity.
 */
public interface Cacheable {
    /**
     * Returns true if this object should be considered equal to another for caching purposes.
     * This is separate from equals() to allow different equality semantics.
     *
     * @param other the object to compare with
     * @return true if the objects are equal for caching purposes
     */
    default boolean cacheEquals(Object other) {
        return CacheManager.getInstance().cacheEquals(this, other);
    }

    /**
     * Returns a hash code for cache identity purposes.
     * This is separate from hashCode() to allow different hashing semantics.
     *
     * @return the cache identity hash code
     */
    int cacheIdentityHash();
}
