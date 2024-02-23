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
package org.apache.maven.api.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Nonnull;

public interface Lookup extends Service {
    /**
     * Performs a lookup for given typed component.
     *
     * @param type The component type.
     * @return The component.
     * @param <T> The component type.
     * @throws LookupException if no such component or there is some provisioning related issue.
     */
    @Nonnull
    <T> T lookup(Class<T> type);

    /**
     * Performs a lookup for given typed component.
     *
     * @param type The component type.
     * @param name The component name.
     * @return The component.
     * @param <T> The component type.
     * @throws LookupException if no such component or there is some provisioning related issue.
     */
    @Nonnull
    <T> T lookup(Class<T> type, String name);

    /**
     * Performs a lookup for optional typed component.
     *
     * @param type The component type.
     * @return Optional carrying component or empty optional if no such component.
     * @param <T> The component type.
     * @throws LookupException if there is some provisioning related issue.
     */
    @Nonnull
    <T> Optional<T> lookupOptional(Class<T> type);

    /**
     * Performs a lookup for optional typed component.
     *
     * @param type The component type.
     * @param name The component name.
     * @return Optional carrying component or empty optional if no such component.
     * @param <T> The component type.
     * @throws LookupException if there is some provisioning related issue.
     */
    @Nonnull
    <T> Optional<T> lookupOptional(Class<T> type, String name);

    /**
     * Performs a collection lookup for given typed components.
     *
     * @param type The component type.
     * @return The list of components. The list may be empty if no components found.
     * @param <T> The component type.
     * @throws LookupException if there is some provisioning related issue.
     */
    @Nonnull
    <T> List<T> lookupList(Class<T> type);

    /**
     * Performs a collection lookup for given typed components.
     *
     * @param type The component type.
     * @return The map of components. The map may be empty if no components found.
     * @param <T> The component type.
     * @throws LookupException if there is some provisioning related issue.
     */
    @Nonnull
    <T> Map<String, T> lookupMap(Class<T> type);
}
