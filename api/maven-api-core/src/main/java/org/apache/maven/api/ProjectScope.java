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
 * Project scope.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
@SuppressWarnings("checkstyle:magicnumber")
public interface ProjectScope extends Comparable<ProjectScope> {
    @Nonnull
    String id();

    int ordinal();

    @Override
    default int compareTo(ProjectScope o) {
        return this.ordinal() - o.ordinal();
    }

    /**
     * Main scope.
     */
    ProjectScope MAIN = new ProjectScope() {
        @Override
        public String id() {
            return "main";
        }

        @Override
        public int ordinal() {
            return 10;
        }
    };

    /**
     * Test scope.
     */
    ProjectScope TEST = new ProjectScope() {
        @Override
        public String id() {
            return "test";
        }

        @Override
        public int ordinal() {
            return 20;
        }
    };
}
