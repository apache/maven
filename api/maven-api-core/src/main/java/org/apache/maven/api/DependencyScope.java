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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Dependency scope.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface DependencyScope {
    @Nonnull
    String id();

    boolean isTransitive();

    DependencyScope NONE = new DependencyScope() {
        @Override
        public String id() {
            return "none";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };

    DependencyScope COMPILE_ONLY = new DependencyScope() {
        @Override
        public String id() {
            return "compile-only";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };

    DependencyScope COMPILE = new DependencyScope() {
        @Override
        public String id() {
            return "compile";
        }

        @Override
        public boolean isTransitive() {
            return true;
        }
    };

    DependencyScope RUNTIME = new DependencyScope() {
        @Override
        public String id() {
            return "runtime";
        }

        @Override
        public boolean isTransitive() {
            return true;
        }
    };

    DependencyScope PROVIDED = new DependencyScope() {
        @Override
        public String id() {
            return "provided";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };

    DependencyScope TEST_ONLY = new DependencyScope() {
        @Override
        public String id() {
            return "test-only";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };

    DependencyScope TEST = new DependencyScope() {
        @Override
        public String id() {
            return "test";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };

    DependencyScope TEST_RUNTIME = new DependencyScope() {
        @Override
        public String id() {
            return "test-runtime";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };

    DependencyScope SYSTEM = new DependencyScope() {
        @Override
        public String id() {
            return "system";
        }

        @Override
        public boolean isTransitive() {
            return false;
        }
    };
}
