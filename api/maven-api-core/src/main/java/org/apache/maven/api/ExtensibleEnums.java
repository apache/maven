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
package org.apache.maven.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for creating extensible enum implementations.
 * This class provides factory methods for creating instances of extensible enums
 * such as Language, PathScope, and ProjectScope.
 *
 * @since 4.0.0
 */
abstract class ExtensibleEnums {

    /**
     * Creates a new Language instance with the specified ID.
     *
     * @param id the identifier for the language
     * @return a new Language instance
     */
    static Language language(String id) {
        return new DefaultLanguage(id);
    }

    /**
     * Creates a new PathScope instance with the specified ID, project scope, and dependency scopes.
     *
     * @param id the identifier for the path scope
     * @param projectScope the project scope associated with this path scope
     * @param dependencyScopes the dependency scopes associated with this path scope
     * @return a new PathScope instance
     */
    static PathScope pathScope(String id, ProjectScope projectScope, DependencyScope... dependencyScopes) {
        return new DefaultPathScope(id, projectScope, dependencyScopes);
    }

    /**
     * Creates a new ProjectScope instance with the specified ID.
     *
     * @param id the identifier for the project scope
     * @return a new ProjectScope instance
     */
    static ProjectScope projectScope(String id) {
        return new DefaultProjectScope(id);
    }

    /**
     * Base implementation of the ExtensibleEnum interface.
     * Provides common functionality for all extensible enum implementations.
     */
    private static class DefaultExtensibleEnum implements ExtensibleEnum {

        private final String id;

        /**
         * Creates a new DefaultExtensibleEnum with the specified ID.
         *
         * @param id the identifier for this enum value, must not be null
         */
        DefaultExtensibleEnum(String id) {
            this.id = Objects.requireNonNull(id);
        }

        /**
         * Returns the identifier for this enum value.
         *
         * @return the identifier
         */
        public String id() {
            return id;
        }

        @Override
        public int hashCode() {
            return id().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && getClass() == obj.getClass() && id().equals(((DefaultExtensibleEnum) obj).id());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + id() + "]";
        }
    }

    /**
     * Implementation of the PathScope interface.
     */
    private static class DefaultPathScope extends DefaultExtensibleEnum implements PathScope {
        private final ProjectScope projectScope;
        private final Set<DependencyScope> dependencyScopes;

        /**
         * Creates a new DefaultPathScope with the specified ID, project scope, and dependency scopes.
         *
         * @param id the identifier for this path scope
         * @param projectScope the project scope associated with this path scope, must not be null
         * @param dependencyScopes the dependency scopes associated with this path scope, must not be null
         */
        DefaultPathScope(String id, ProjectScope projectScope, DependencyScope... dependencyScopes) {
            super(id);
            this.projectScope = Objects.requireNonNull(projectScope);
            this.dependencyScopes =
                    Collections.unmodifiableSet(new HashSet<>(Arrays.asList(Objects.requireNonNull(dependencyScopes))));
        }

        /**
         * Returns the project scope associated with this path scope.
         *
         * @return the project scope
         */
        @Override
        public ProjectScope projectScope() {
            return projectScope;
        }

        /**
         * Returns the dependency scopes associated with this path scope.
         *
         * @return an unmodifiable set of dependency scopes
         */
        @Override
        public Set<DependencyScope> dependencyScopes() {
            return dependencyScopes;
        }
    }

    /**
     * Implementation of the ProjectScope interface.
     */
    private static class DefaultProjectScope extends DefaultExtensibleEnum implements ProjectScope {

        /**
         * Creates a new DefaultProjectScope with the specified ID.
         *
         * @param id the identifier for this project scope
         */
        DefaultProjectScope(String id) {
            super(id);
        }
    }

    /**
     * Implementation of the Language interface.
     */
    private static class DefaultLanguage extends DefaultExtensibleEnum implements Language {

        /**
         * Creates a new DefaultLanguage with the specified ID.
         *
         * @param id the identifier for this language
         */
        DefaultLanguage(String id) {
            super(id);
        }
    }
}
