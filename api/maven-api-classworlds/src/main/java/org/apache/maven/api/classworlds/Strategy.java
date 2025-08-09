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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * A strategy for defining how classes and resources are located in class realms.
 * <p>
 * Different strategies can implement different class loading behaviors, such as
 * parent-first, child-first, or custom delegation patterns.
 * </p>
 *
 * @since 4.1.0
 */
@Experimental
public interface Strategy {

    /**
     * Loads a class using this strategy.
     *
     * @param name the fully qualified class name
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    @Nonnull
    Class<?> loadClass(@Nonnull String name) throws ClassNotFoundException;

    /**
     * Finds a resource using this strategy.
     *
     * @param name the resource name
     * @return the resource URL, or null if not found
     */
    @Nullable
    URL getResource(@Nonnull String name);

    /**
     * Finds all resources with the given name using this strategy.
     *
     * @param name the resource name
     * @return an enumeration of resource URLs
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    Enumeration<URL> getResources(@Nonnull String name) throws IOException;

    /**
     * Returns the class realm that this strategy operates on.
     *
     * @return the associated class realm
     */
    @Nonnull
    ClassRealm getRealm();
}
