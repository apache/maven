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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Source;

/**
 * A default implementation of {@code SourceRoot} built from the model.
 *
 * @param scope               in which context the source files will be used (main or test)
 * @param language            language of the source files
 * @param moduleName          name of the Java module which is built by the sources
 * @param targetVersionOrNull version of the platform where the code will be executed
 * @param directory           root directory where the sources are stored
 * @param includes            patterns for the files to include, or empty if unspecified
 * @param excludes            patterns for the files to exclude, or empty if nothing to exclude
 * @param stringFiltering     whether resources are filtered to replace tokens with parameterized values
 * @param targetPathOrNull    an explicit target path, overriding the default value
 * @param enabled             whether the directory described by this source element should be included in the build
 */
public record DefaultSourceRoot(
        @Nonnull ProjectScope scope,
        @Nonnull Language language,
        @Nullable String moduleName,
        @Nullable Version targetVersionOrNull,
        @Nonnull Path directory,
        @Nonnull List<String> includes,
        @Nonnull List<String> excludes,
        boolean stringFiltering,
        @Nullable Path targetPathOrNull,
        boolean enabled)
        implements SourceRoot {

    /**
     * Creates a simple instance with no Java module, no target version, and no include or exclude pattern.
     *
     * @param scope     in which context the source files will be used (main or test)
     * @param language  the language of the source files
     * @param directory the root directory where the sources are stored
     */
    public DefaultSourceRoot(ProjectScope scope, Language language, Path directory) {
        this(scope, language, null, null, directory, null, null, false, null, true);
    }

    /**
     * Canonical constructor.
     *
     * @param scope               in which context the source files will be used (main or test)
     * @param language            language of the source files
     * @param moduleName          name of the Java module which is built by the sources
     * @param targetVersionOrNull version of the platform where the code will be executed
     * @param directory           root directory where the sources are stored
     * @param includes            patterns for the files to include, or {@code null} or empty if unspecified
     * @param excludes            patterns for the files to exclude, or {@code null} or empty if nothing to exclude
     * @param stringFiltering     whether resources are filtered to replace tokens with parameterized values
     * @param targetPathOrNull    an explicit target path, overriding the default value
     * @param enabled             whether the directory described by this source element should be included in the build
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultSourceRoot(
            @Nonnull ProjectScope scope,
            @Nonnull Language language,
            @Nullable String moduleName,
            @Nullable Version targetVersionOrNull,
            @Nonnull Path directory,
            @Nullable List<String> includes,
            @Nullable List<String> excludes,
            boolean stringFiltering,
            @Nullable Path targetPathOrNull,
            boolean enabled) {
        this.scope = Objects.requireNonNull(scope);
        this.language = Objects.requireNonNull(language);
        this.moduleName = nonBlank(moduleName).orElse(null);
        this.targetVersionOrNull = targetVersionOrNull;
        this.directory = directory.normalize();
        this.includes = (includes != null) ? List.copyOf(includes) : List.of();
        this.excludes = (excludes != null) ? List.copyOf(excludes) : List.of();
        this.stringFiltering = stringFiltering;
        this.targetPathOrNull = (targetPathOrNull != null) ? targetPathOrNull.normalize() : null;
        this.enabled = enabled;
    }

    /**
     * Creates a new instance from the given model.
     *
     * @param session    the session of resolving extensible enumerations
     * @param baseDir    the base directory for resolving relative paths
     * @param outputDir  supplier of output directory relative to {@code baseDir}
     * @param source     a source element from the model
     */
    public static DefaultSourceRoot fromModel(
            Session session, Path baseDir, Function<ProjectScope, String> outputDir, Source source) {
        ProjectScope scope =
                nonBlank(source.getScope()).map(session::requireProjectScope).orElse(ProjectScope.MAIN);
        Language language =
                nonBlank(source.getLang()).map(session::requireLanguage).orElse(Language.JAVA_FAMILY);
        String moduleName = nonBlank(source.getModule()).orElse(null);
        return new DefaultSourceRoot(
                scope,
                language,
                moduleName,
                nonBlank(source.getTargetVersion()).map(session::parseVersion).orElse(null),
                nonBlank(source.getDirectory()).map(baseDir::resolve).orElseGet(() -> {
                    Path src = baseDir.resolve("src");
                    if (moduleName != null) {
                        src = src.resolve(moduleName);
                    }
                    return src.resolve(scope.id()).resolve(language.id());
                }),
                source.getIncludes(),
                source.getExcludes(),
                source.isStringFiltering(),
                nonBlank(source.getTargetPath()).map(Path::of).orElse(null),
                source.isEnabled());
    }

    /**
     * Creates a new instance from the given resource.
     * This is used for migration from the previous way of declaring resources.
     * <p>
     * <strong>Important:</strong> The {@code targetPath} from the resource is stored as-is
     * (converted to a {@link Path} but not resolved against any directory). This preserves
     * the Maven 3.x behavior where {@code targetPath} is relative to the output directory,
     * not the project base directory. The actual resolution happens later via
     * {@link SourceRoot#targetPath(Project)}.
     * </p>
     *
     * @param baseDir the base directory for resolving relative paths (used only for the source directory)
     * @param scope the scope of the resource (main or test)
     * @param resource a resource element from the model
     */
    public DefaultSourceRoot(final Path baseDir, ProjectScope scope, Resource resource) {
        this(
                scope,
                Language.RESOURCES,
                null,
                null,
                baseDir.resolve(nonBlank(resource.getDirectory())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Source declaration without directory value."))),
                resource.getIncludes(),
                resource.getExcludes(),
                Boolean.parseBoolean(resource.getFiltering()),
                nonBlank(resource.getTargetPath()).map(Path::of).orElse(null),
                true);
    }

    /**
     * {@return the given value as a trimmed non-blank string, or empty otherwise}
     */
    private static Optional<String> nonBlank(String value) {
        if (value != null) {
            value = value.trim();
            if (!value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * {@return a matcher combining the include and exclude patterns}
     *
     * @param defaultIncludes the default includes if unspecified by the user
     * @param useDefaultExcludes whether to add the default set of patterns to exclude,
     *        mostly Source Code Management (<abbr>SCM</abbr>) files
     */
    @Override
    public PathMatcher matcher(Collection<String> defaultIncludes, boolean useDefaultExcludes) {
        Collection<String> actual = includes();
        if (actual == null || actual.isEmpty()) {
            actual = defaultIncludes;
        }
        return PathSelector.of(directory(), actual, excludes(), useDefaultExcludes);
    }

    /**
     * {@return the name of the Java module (or other language-specific module) which is built by the sources}
     */
    @Override
    public Optional<String> module() {
        return Optional.ofNullable(moduleName);
    }

    /**
     * {@return the version of the platform where the code will be executed}
     */
    @Override
    public Optional<Version> targetVersion() {
        return Optional.ofNullable(targetVersionOrNull);
    }

    /**
     * {@return an explicit target path, overriding the default value}
     * <p>
     * The returned path, if present, is stored as provided in the configuration and is typically
     * relative to the output directory. Use {@link #targetPath(Project)} to get the fully
     * resolved absolute path.
     * </p>
     */
    @Override
    public Optional<Path> targetPath() {
        return Optional.ofNullable(targetPathOrNull);
    }
}
