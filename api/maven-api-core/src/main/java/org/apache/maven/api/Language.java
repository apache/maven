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
import org.apache.maven.api.annotations.Nullable;

/**
 * Language.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Language {
    /**
     * The "none" language. It is not versioned, family is same to itself, and compatible with itself only.
     * In turn, every {@link Language} implementation must be compatible with {@code NONE} language.
     */
    Language NONE = new Language() {
        @Override
        public String id() {
            return "none";
        }

        @Override
        public Language family() {
            return this;
        }

        @Override
        public Version version() {
            return null;
        }

        @Override
        public boolean isCompatibleWith(Language language) {
            return this == language;
        }
    };

    // TODO: this should be moved out from here to Java Support (builtin into core)
    Language JAVA_FAMILY = new Language() {
        @Override
        public String id() {
            return "java";
        }

        @Override
        public Language family() {
            return this;
        }

        @Override
        public Version version() {
            return null;
        }

        public boolean isCompatibleWith(Language language) {
            Language family = language.family();
            return this == family || NONE == family;
        }
    };

    @Nonnull
    String id();

    @Nonnull
    Language family();

    @Nullable
    Version version();

    /**
     * Returns {@code true} if this language is compatible with provided language.
     * For example "Java 8" is compatible with "Java 11", but other way is not true.
     * <p>
     * Important note: every implementation must return {@code true} if passed in language is {@link #NONE}.
     * <p>
     * By default, every language is compatible with itself and {@link #NONE} language. Override if needed.
     */
    default boolean isCompatibleWith(Language language) {
        return this == language || NONE == language.family();
    }
}
