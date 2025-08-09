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
package org.apache.maven.api.classworlds;

import java.io.Closeable;
import java.util.function.Predicate;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * A collection of {@link ClassRealm}s, indexed by id.
 * <p>
 * A ClassWorld provides a container for managing multiple class loading realms,
 * each with their own classpath and import/export relationships.
 * </p>
 *
 * @since 4.1.0
 */
@Experimental
public interface ClassWorld extends Closeable {

    /**
     * Creates a new class realm with the specified id.
     *
     * @param id the unique identifier for the realm
     * @return the newly created class realm
     * @throws DuplicateRealmException if a realm with the same id already exists
     */
    @Nonnull
    ClassRealm newRealm(@Nonnull String id) throws DuplicateRealmException;

    /**
     * Creates a new class realm with the specified id and base class loader.
     *
     * @param id the unique identifier for the realm
     * @param classLoader the base class loader for the realm, may be null
     * @return the newly created class realm
     * @throws DuplicateRealmException if a realm with the same id already exists
     */
    @Nonnull
    ClassRealm newRealm(@Nonnull String id, @Nullable ClassLoader classLoader) throws DuplicateRealmException;

    /**
     * Creates a new filtered class realm with the specified id, base class loader, and filter.
     *
     * @param id the unique identifier for the realm
     * @param classLoader the base class loader for the realm, may be null
     * @param filter the filter to apply to class loading, may be null
     * @return the newly created class realm
     * @throws DuplicateRealmException if a realm with the same id already exists
     */
    @Nonnull
    ClassRealm newRealm(@Nonnull String id, @Nullable ClassLoader classLoader, @Nullable Predicate<String> filter)
            throws DuplicateRealmException;

    /**
     * Retrieves the class realm with the specified id.
     *
     * @param id the realm identifier
     * @return the class realm
     * @throws NoSuchRealmException if no realm with the specified id exists
     */
    @Nonnull
    ClassRealm getRealm(@Nonnull String id) throws NoSuchRealmException;

    /**
     * Retrieves the class realm with the specified id, or null if it doesn't exist.
     *
     * @param id the realm identifier
     * @return the class realm, or null if not found
     */
    @Nullable
    ClassRealm getClassRealm(@Nonnull String id);

    // Note: getRealms method is not included in the API interface
    // to avoid conflicts with the existing implementation signature

    /**
     * Disposes of the class realm with the specified id.
     *
     * @param id the realm identifier
     * @throws NoSuchRealmException if no realm with the specified id exists
     */
    void disposeRealm(@Nonnull String id) throws NoSuchRealmException;

    /**
     * Adds a listener to be notified of realm lifecycle events.
     *
     * @param listener the listener to add
     */
    void addListener(@Nonnull ClassWorldListener listener);

    /**
     * Removes a listener from realm lifecycle event notifications.
     *
     * @param listener the listener to remove
     */
    void removeListener(@Nonnull ClassWorldListener listener);
}
