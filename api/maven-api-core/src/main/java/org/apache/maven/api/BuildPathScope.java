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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Build path scope.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface BuildPathScope {
    @Nonnull
    String id();

    @Nonnull
    ProjectScope projectScope();

    @Nonnull
    Collection<DependencyScope> getDependencyScopes();

    default BuildPathScope union(BuildPathScope other) {
        return new BuildPathScope() {
            @Override
            public String id() {
                return BuildPathScope.this.id() + "+" + other.id();
            }

            @Override
            public ProjectScope projectScope() {
                return BuildPathScope.this.projectScope().compareTo(other.projectScope()) < 1
                        ? other.projectScope()
                        : BuildPathScope.this.projectScope();
            }

            @Override
            public Collection<DependencyScope> getDependencyScopes() {
                HashSet<DependencyScope> result = new HashSet<>(BuildPathScope.this.getDependencyScopes());
                result.addAll(other.getDependencyScopes());
                return result;
            }
        };
    }

    BuildPathScope MAIN_COMPILE = new BuildPathScope() {
        @Override
        public String id() {
            return "main-compile";
        }

        @Override
        public ProjectScope projectScope() {
            return ProjectScope.MAIN;
        }

        @Override
        public Collection<DependencyScope> getDependencyScopes() {
            return Collections.unmodifiableList(
                    Arrays.asList(DependencyScope.COMPILE_ONLY, DependencyScope.COMPILE, DependencyScope.PROVIDED));
        }
    };

    BuildPathScope MAIN_RUNTIME = new BuildPathScope() {
        @Override
        public String id() {
            return "main-runtime";
        }

        @Override
        public ProjectScope projectScope() {
            return ProjectScope.MAIN;
        }

        @Override
        public Collection<DependencyScope> getDependencyScopes() {
            return Collections.unmodifiableList(Arrays.asList(DependencyScope.COMPILE, DependencyScope.RUNTIME));
        }
    };

    BuildPathScope TEST_COMPILE = new BuildPathScope() {
        @Override
        public String id() {
            return "test-compile";
        }

        @Override
        public ProjectScope projectScope() {
            return ProjectScope.TEST;
        }

        @Override
        public Collection<DependencyScope> getDependencyScopes() {
            return Collections.unmodifiableList(Arrays.asList(
                    DependencyScope.COMPILE,
                    DependencyScope.PROVIDED,
                    DependencyScope.TEST_ONLY,
                    DependencyScope.TEST));
        }
    };

    BuildPathScope TEST_RUNTIME = new BuildPathScope() {
        @Override
        public String id() {
            return "test-runtime";
        }

        @Override
        public ProjectScope projectScope() {
            return ProjectScope.TEST;
        }

        @Override
        public Collection<DependencyScope> getDependencyScopes() {
            return Collections.unmodifiableList(Arrays.asList(
                    DependencyScope.COMPILE,
                    DependencyScope.RUNTIME,
                    DependencyScope.PROVIDED,
                    DependencyScope.TEST,
                    DependencyScope.TEST_RUNTIME));
        }
    };
}
