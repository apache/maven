package org.apache.maven.model.building;

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

import org.apache.maven.building.Source;

/**
 * Caches auxiliary data used during model building like already processed raw/effective models. The data in the cache
 * is meant for exclusive consumption by the model builder and is opaque to the cache implementation. The cache key is
 * formed by a combination of group id, artifact id, version and tag. The first three components generally refer to the
 * identify of a model. The tag allows for further classification of the associated data on the sole discretion of the
 * model builder.
 *
 * @author Benjamin Bentmann
 * @author Robert Scholte
 */
public interface ModelCache
{
    /**
     * Puts the specified data into the cache.
     *
     * @param path The path of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @param data The data to store in the cache, must not be {@code null}.
     * @since 4.0.0
     */
    default void put( Source path, String tag, Object data )
    {
        // only useful for ReactorModelCache
    }

    /**
     * Gets the specified data from the cache.
     *
     * @param path The path of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @return The requested data or {@code null} if none was present in the cache.
     * @since 4.0.0
     */
    default Object get( Source path, String tag )
    {
        // only useful for ReactorModelCache
        return null;
    }

    /**
     * Puts the specified data into the cache.
     *
     * @param groupId The group id of the cache record, must not be {@code null}.
     * @param artifactId The artifact id of the cache record, must not be {@code null}.
     * @param version The version of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @param data The data to store in the cache, must not be {@code null}.
     */
    void put( String groupId, String artifactId, String version, String tag, Object data );

    /**
     * Gets the specified data from the cache.
     *
     * @param groupId The group id of the cache record, must not be {@code null}.
     * @param artifactId The artifact id of the cache record, must not be {@code null}.
     * @param version The version of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @return The requested data or {@code null} if none was present in the cache.
     */
    Object get( String groupId, String artifactId, String version, String tag );

    /**
     * Puts the specified data into the cache.
     *
     * @param path The path of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @param data The data to store in the cache, must not be {@code null}.
     * @since 4.0.0
     */
    default <T> void put( Source path, ModelCacheTag<T> tag, T data )
    {
        put( path, tag.getName(), tag.intoCache( data ) );
    }

    /**
     * Gets the specified data from the cache.
     *
     * @param path The path of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @return The requested data or {@code null} if none was present in the cache.
     * @since 4.0.0
     */
    default <T> T get( Source path, ModelCacheTag<T> tag )
    {
        Object obj = get( path, tag.getName() );
        return ( obj != null ) ? tag.fromCache( tag.getType().cast( obj ) ) : null;
    }

    /**
     * Puts the specified data into the cache.
     *
     * @param groupId The group id of the cache record, must not be {@code null}.
     * @param artifactId The artifact id of the cache record, must not be {@code null}.
     * @param version The version of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @param data The data to store in the cache, must not be {@code null}.
     */
    default <T> void put( String groupId, String artifactId, String version, ModelCacheTag<T> tag, T data )
    {
        put( groupId, artifactId, version, tag.getName(), tag.intoCache( data ) );
    }

    /**
     * Gets the specified data from the cache.
     *
     * @param groupId The group id of the cache record, must not be {@code null}.
     * @param artifactId The artifact id of the cache record, must not be {@code null}.
     * @param version The version of the cache record, must not be {@code null}.
     * @param tag The tag of the cache record, must not be {@code null}.
     * @return The requested data or {@code null} if none was present in the cache.
     */
    default <T> T get( String groupId, String artifactId, String version, ModelCacheTag<T> tag )
    {
        Object obj = get( groupId, artifactId, version, tag.getName() );
        return ( obj != null ) ? tag.fromCache( tag.getType().cast( obj ) ) : null;
    }

}
