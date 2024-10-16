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

import java.nio.file.Path;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * A Source specific to load POMs.  The {@link #resolve(ModelLocator, String)} method
 * will be used to find POMs for subprojects.
 *
 * @since 4.0.0
 */
public interface ModelSource extends Source {

    interface ModelLocator {
        /**
         * Returns the file containing the pom or null if a pom can not be
         * found at the given file or in the given directory.
         *
         * @since 4.0.0
         */
        @Nullable
        Path locateExistingPom(@Nonnull Path project);
    }

    @Nullable
    ModelSource resolve(@Nonnull ModelLocator modelLocator, @Nonnull String relative);

    @Nonnull
    static ModelSource fromPath(@Nonnull Path path) {
        return fromPath(path, null);
    }

    @Nonnull
    static ModelSource fromPath(@Nonnull Path path, @Nullable String location) {
        return new PathSource(nonNull(path, "path cannot be null"), location);
    }
}
