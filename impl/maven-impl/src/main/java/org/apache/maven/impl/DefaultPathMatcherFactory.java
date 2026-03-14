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
package org.apache.maven.impl;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Objects;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.PathMatcherFactory;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation of {@link PathMatcherFactory} that creates {@link PathSelector}
 * instances for filtering files based on include/exclude patterns.
 * <p>
 * This implementation provides Maven's traditional include/exclude pattern behavior,
 * compatible with Maven 3 plugins like maven-compiler-plugin and maven-clean-plugin.
 *
 * @since 4.0.0
 */
@Named
@Singleton
public class DefaultPathMatcherFactory implements PathMatcherFactory {

    @Nonnull
    @Override
    public PathMatcher createPathMatcher(
            @Nonnull Path baseDirectory,
            Collection<String> includes,
            Collection<String> excludes,
            boolean useDefaultExcludes) {
        requireNonNull(baseDirectory, "baseDirectory cannot be null");

        return PathSelector.of(baseDirectory, includes, excludes, useDefaultExcludes);
    }

    @Nonnull
    @Override
    public PathMatcher createExcludeOnlyMatcher(
            @Nonnull Path baseDirectory, Collection<String> excludes, boolean useDefaultExcludes) {
        return createPathMatcher(baseDirectory, null, excludes, useDefaultExcludes);
    }

    @Nonnull
    @Override
    public PathMatcher deriveDirectoryMatcher(@Nonnull PathMatcher fileMatcher) {
        if (Objects.requireNonNull(fileMatcher) instanceof PathSelector selector) {
            return selector.createDirectoryMatcher();
        }
        return PathSelector.INCLUDES_ALL;
    }

    @Nonnull
    @Override
    public PathMatcher includesAll() {
        return PathSelector.INCLUDES_ALL;
    }
}
