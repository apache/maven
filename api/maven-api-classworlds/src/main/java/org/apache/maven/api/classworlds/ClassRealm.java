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
import java.net.URL;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * A class loading realm that provides isolated class loading with controlled imports and exports.
 * <p>
 * A ClassRealm represents an isolated class loading environment with its own classpath
 * and controlled access to classes from other realms through imports.
 * </p>
 *
 * @since 4.1.0
 */
@Experimental
public interface ClassRealm extends Closeable {

    /**
     * Returns the unique identifier for this realm.
     *
     * @return the realm identifier
     */
    @Nonnull
    String getId();

    /**
     * Returns the class world that contains this realm.
     *
     * @return the parent class world
     */
    @Nonnull
    ClassWorld getWorld();

    /**
     * Returns the underlying ClassLoader for this realm.
     * <p>
     * This method allows access to the actual ClassLoader implementation
     * while maintaining API abstraction.
     * </p>
     *
     * @return the underlying ClassLoader
     */
    @Nonnull
    ClassLoader getClassLoader();

    /**
     * Returns the class loading strategy used by this realm.
     *
     * @return the strategy
     */
    @Nonnull
    Strategy getStrategy();

    /**
     * Adds a URL to this realm's classpath.
     *
     * @param url the URL to add
     */
    void addURL(@Nonnull URL url);

    /**
     * Returns the URLs in this realm's classpath.
     *
     * @return array of URLs in the classpath
     */
    @Nonnull
    URL[] getURLs();

    /**
     * Imports classes from the specified realm for the given package.
     *
     * @param realmId the identifier of the realm to import from
     * @param packageName the package name to import (supports wildcards)
     * @throws NoSuchRealmException if the specified realm doesn't exist
     */
    void importFrom(@Nonnull String realmId, @Nonnull String packageName) throws NoSuchRealmException;

    /**
     * Imports classes from the specified class loader for the given package.
     *
     * @param classLoader the class loader to import from
     * @param packageName the package name to import (supports wildcards)
     */
    void importFrom(@Nonnull ClassLoader classLoader, @Nonnull String packageName);

    /**
     * Returns the class loader that would handle the specified class name through imports.
     *
     * @param name the class name
     * @return the import class loader, or null if no import matches
     */
    @Nullable
    ClassLoader getImportClassLoader(@Nonnull String name);

    // Note: getImportRealms method is not included in the API interface
    // to avoid conflicts with the existing implementation signature

    /**
     * Sets the parent class loader for this realm.
     *
     * @param parentClassLoader the parent class loader, may be null
     */
    void setParentClassLoader(@Nullable ClassLoader parentClassLoader);

    /**
     * Returns the parent class loader for this realm.
     *
     * @return the parent class loader, may be null
     */
    @Nullable
    ClassLoader getParentClassLoader();

    // Note: setParentRealm method is not included in the API interface
    // to avoid conflicts with the existing implementation signature

    // Note: getParentRealm method is not included in the API interface
    // to avoid conflicts with the existing implementation signature

    // Note: createChildRealm method is not included in the API interface
    // to avoid conflicts with the existing implementation signature

    /**
     * Loads a class from this realm only (not from imports or parent).
     *
     * @param name the class name
     * @return the loaded class, or null if not found
     */
    @Nullable
    Class<?> loadClassFromSelf(@Nonnull String name);

    /**
     * Loads a class from imported realms/classloaders.
     *
     * @param name the class name
     * @return the loaded class, or null if not found
     */
    @Nullable
    Class<?> loadClassFromImport(@Nonnull String name);

    /**
     * Loads a class from the parent class loader.
     *
     * @param name the class name
     * @return the loaded class, or null if not found
     */
    @Nullable
    Class<?> loadClassFromParent(@Nonnull String name);

    /**
     * Loads a resource from this realm only (not from imports or parent).
     *
     * @param name the resource name
     * @return the resource URL, or null if not found
     */
    @Nullable
    URL loadResourceFromSelf(@Nonnull String name);

    /**
     * Loads a resource from imported realms/classloaders.
     *
     * @param name the resource name
     * @return the resource URL, or null if not found
     */
    @Nullable
    URL loadResourceFromImport(@Nonnull String name);

    /**
     * Loads a resource from the parent class loader.
     *
     * @param name the resource name
     * @return the resource URL, or null if not found
     */
    @Nullable
    URL loadResourceFromParent(@Nonnull String name);
}
