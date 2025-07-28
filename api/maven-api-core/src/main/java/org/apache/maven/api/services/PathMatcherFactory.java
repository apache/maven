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
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Objects;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service for creating {@link PathMatcher} objects that can be used to filter files
 * based on include/exclude patterns. This service provides a clean API for plugins
 * to create path matchers without directly depending on implementation classes.
 * <p>
 * The path matchers created by this service support Maven's traditional include/exclude
 * pattern syntax, which is compatible with the behavior of Maven 3 plugins like
 * maven-compiler-plugin and maven-clean-plugin.
 * <p>
 * Pattern syntax supports:
 * <ul>
 *   <li>Standard glob patterns with {@code *}, {@code ?}, and {@code **} wildcards</li>
 *   <li>Explicit syntax prefixes like {@code "glob:"} or {@code "regex:"}</li>
 *   <li>Maven 3 compatible behavior for patterns without explicit syntax</li>
 *   <li>Default exclusion patterns for SCM files when requested</li>
 * </ul>
 *
 * @since 4.0.0
 * @see PathMatcher
 */
@Experimental
public interface PathMatcherFactory extends Service {

    /**
     * Creates a path matcher for filtering files based on include and exclude patterns.
     * <p>
     * The pathnames used for matching will be relative to the specified base directory
     * and use {@code '/'} as separator, regardless of the hosting operating system.
     *
     * @param baseDirectory the base directory for relativizing paths during matching
     * @param includes the patterns of files to include, or null/empty for including all files
     * @param excludes the patterns of files to exclude, or null/empty for no exclusion
     * @param useDefaultExcludes whether to augment excludes with default SCM exclusion patterns
     * @return a PathMatcher that can be used to test if paths should be included
     * @throws NullPointerException if baseDirectory is null
     */
    @Nonnull
    PathMatcher createPathMatcher(
            @Nonnull Path baseDirectory,
            Collection<String> includes,
            Collection<String> excludes,
            boolean useDefaultExcludes);

    /**
     * Creates a path matcher for filtering files based on include and exclude patterns,
     * without using default exclusion patterns.
     * <p>
     * This is equivalent to calling {@link #createPathMatcher(Path, Collection, Collection, boolean)}
     * with {@code useDefaultExcludes = false}.
     *
     * @param baseDirectory the base directory for relativizing paths during matching
     * @param includes the patterns of files to include, or null/empty for including all files
     * @param excludes the patterns of files to exclude, or null/empty for no exclusion
     * @return a PathMatcher that can be used to test if paths should be included
     * @throws NullPointerException if baseDirectory is null
     */
    @Nonnull
    default PathMatcher createPathMatcher(
            @Nonnull Path baseDirectory, Collection<String> includes, Collection<String> excludes) {
        return createPathMatcher(baseDirectory, includes, excludes, false);
    }

    /**
     * Creates a path matcher that includes all files except those matching the exclude patterns.
     * <p>
     * This is equivalent to calling {@link #createPathMatcher(Path, Collection, Collection, boolean)}
     * with {@code includes = null}.
     *
     * @param baseDirectory the base directory for relativizing paths during matching
     * @param excludes the patterns of files to exclude, or null/empty for no exclusion
     * @param useDefaultExcludes whether to augment excludes with default SCM exclusion patterns
     * @return a PathMatcher that can be used to test if paths should be included
     * @throws NullPointerException if baseDirectory is null
     */
    @Nonnull
    default PathMatcher createExcludeOnlyMatcher(
            @Nonnull Path baseDirectory, Collection<String> excludes, boolean useDefaultExcludes) {
        return createPathMatcher(baseDirectory, null, excludes, useDefaultExcludes);
    }

    /**
     * Creates a path matcher that only includes files matching the include patterns.
     * <p>
     * This is equivalent to calling {@link #createPathMatcher(Path, Collection, Collection, boolean)}
     * with {@code excludes = null} and {@code useDefaultExcludes = false}.
     *
     * @param baseDirectory the base directory for relativizing paths during matching
     * @param includes the patterns of files to include, or null/empty for including all files
     * @return a PathMatcher that can be used to test if paths should be included
     * @throws NullPointerException if baseDirectory is null
     */
    @Nonnull
    default PathMatcher createIncludeOnlyMatcher(@Nonnull Path baseDirectory, Collection<String> includes) {
        return createPathMatcher(baseDirectory, includes, null, false);
    }

    /**
     * Returns a filter for directories that may contain paths accepted by the given matcher.
     * The given path matcher should be an instance created by this service.
     * The path matcher returned by this method expects <em>directory</em> paths.
     * If that matcher returns {@code false}, then the directory will definitively not contain
     * the paths selected by the matcher given in argument to this method.
     * In such case, the whole directory and all its sub-directories can be skipped.
     * In case of doubt, or if the matcher given in argument is not recognized by this method,
     * then the matcher returned by this method will return {@code true}.
     *
     * @param fileMatcher a matcher created by one of the other methods of this interface
     * @return filter for directories that may contain the selected files
     * @throws NullPointerException if fileMatcher is null
     */
    @Nonnull
    PathMatcher deriveDirectoryMatcher(@Nonnull PathMatcher fileMatcher);

    /**
     * Returns the path matcher that unconditionally returns {@code true} for all files.
     * It should be the matcher returned by the other methods of this interface when the
     * given patterns match all files.
     *
     * @return path matcher that unconditionally returns {@code true} for all files
     */
    @Nonnull
    PathMatcher includesAll();

    /**
     * {@return whether the given matcher includes all files}.
     * This method may conservatively returns {@code false} if case of doubt.
     * A return value of {@code true} means that the pattern is certain to match all files.
     *
     * @param matcher the matcher to test
     */
    default boolean isIncludesAll(@Nonnull PathMatcher matcher) {
        return Objects.requireNonNull(matcher) == includesAll();
    }
}
