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

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.Version;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Source;

/**
 * A default implementation of {@code SourceRoot} built from the model.
 */
public final class DefaultSourceRoot implements SourceRoot {
    private final Path directory;

    private final List<String> includes;

    private final List<String> excludes;

    private final ProjectScope scope;

    private final Language language;

    private final String moduleName;

    private final Version targetVersion;

    private final Path targetPath;

    private final boolean stringFiltering;

    private final boolean enabled;

    /**
     * Creates a new instance from the given model.
     *
     * @param session the session of resolving extensible enumerations
     * @param baseDir the base directory for resolving relative paths
     * @param source a source element from the model
     */
    public DefaultSourceRoot(final Session session, final Path baseDir, final Source source) {
        includes = source.getIncludes();
        excludes = source.getExcludes();
        stringFiltering = source.isStringFiltering();
        enabled = source.isEnabled();
        moduleName = nonBlank(source.getModule());

        String value = nonBlank(source.getScope());
        scope = value != null ? session.requireProjectScope(value) : ProjectScope.MAIN;

        value = nonBlank(source.getLang());
        language = value != null ? session.requireLanguage(value) : Language.JAVA_FAMILY;

        value = nonBlank(source.getDirectory());
        if (value != null) {
            directory = baseDir.resolve(value);
        } else {
            directory = baseDir.resolve("src").resolve(scope.id()).resolve(language.id());
        }

        value = nonBlank(source.getTargetVersion());
        targetVersion = value != null ? session.parseVersion(value) : null;

        value = nonBlank(source.getTargetPath());
        targetPath = value != null ? baseDir.resolve(value) : null;
    }

    /**
     * Creates a new instance from the given resource.
     * This is used for migration from the previous way of declaring resources.
     *
     * @param baseDir the base directory for resolving relative paths
     * @param scope the scope of the resource (main or test)
     * @param resource a resource element from the model
     */
    public DefaultSourceRoot(final Path baseDir, ProjectScope scope, Resource resource) {
        String value = nonBlank(resource.getDirectory());
        if (value == null) {
            throw new IllegalArgumentException("Source declaration without directory value.");
        }
        directory = baseDir.resolve(value).normalize();
        includes = resource.getIncludes();
        excludes = resource.getExcludes();
        stringFiltering = Boolean.parseBoolean(resource.getFiltering());
        enabled = true;
        moduleName = null;
        this.scope = scope;
        language = Language.RESOURCES;
        targetVersion = null;
        targetPath = null;
    }

    /**
     * Creates a new instance for the given directory and scope.
     *
     * @param scope scope of source code (main or test)
     * @param language language of the source code
     * @param directory directory of the source code
     */
    public DefaultSourceRoot(final ProjectScope scope, final Language language, final Path directory) {
        this.scope = Objects.requireNonNull(scope);
        this.language = Objects.requireNonNull(language);
        this.directory = Objects.requireNonNull(directory);
        includes = List.of();
        excludes = List.of();
        moduleName = null;
        targetVersion = null;
        targetPath = null;
        stringFiltering = false;
        enabled = true;
    }

    /**
     * Creates a new instance for the given directory and scope.
     *
     * @param scope scope of source code (main or test)
     * @param language language of the source code
     * @param directory directory of the source code
     * @param includes patterns for the files to include, or {@code null} or empty if unspecified
     * @param excludes patterns for the files to exclude, or {@code null} or empty if nothing to exclude
     */
    public DefaultSourceRoot(
            final ProjectScope scope,
            final Language language,
            final Path directory,
            List<String> includes,
            List<String> excludes) {
        this.scope = Objects.requireNonNull(scope);
        this.language = language;
        this.directory = Objects.requireNonNull(directory);
        this.includes = includes != null ? List.copyOf(includes) : List.of();
        this.excludes = excludes != null ? List.copyOf(excludes) : List.of();
        moduleName = null;
        targetVersion = null;
        targetPath = null;
        stringFiltering = false;
        enabled = true;
    }

    /**
     * {@return the given value as a trimmed non-blank string, or null otherwise}.
     */
    private static String nonBlank(String value) {
        if (value != null) {
            value = value.trim();
            if (value.isBlank()) {
                value = null;
            }
        }
        return value;
    }

    /**
     * {@return the root directory where the sources are stored}.
     */
    @Override
    public Path directory() {
        return directory;
    }

    /**
     * {@return the patterns for the files to include}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Safe because unmodifiable
    public List<String> includes() {
        return includes;
    }

    /**
     * {@return the patterns for the files to exclude}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Safe because unmodifiable
    public List<String> excludes() {
        return excludes;
    }

    /**
     * {@return a matcher combining the include and exclude patterns}.
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
        return new PathSelector(directory(), actual, excludes(), useDefaultExcludes).simplify();
    }

    /**
     * {@return in which context the source files will be used}.
     */
    @Override
    public ProjectScope scope() {
        return scope;
    }

    /**
     * {@return the language of the source files}.
     */
    @Override
    public Language language() {
        return language;
    }

    /**
     * {@return the name of the Java module (or other language-specific module) which is built by the sources}.
     */
    @Override
    public Optional<String> module() {
        return Optional.ofNullable(moduleName);
    }

    /**
     * {@return the version of the platform where the code will be executed}.
     */
    @Override
    public Optional<Version> targetVersion() {
        return Optional.ofNullable(targetVersion);
    }

    /**
     * {@return an explicit target path, overriding the default value}.
     */
    @Override
    public Optional<Path> targetPath() {
        return Optional.ofNullable(targetPath);
    }

    /**
     * {@return whether resources are filtered to replace tokens with parameterized values}.
     */
    @Override
    public boolean stringFiltering() {
        return stringFiltering;
    }

    /**
     * {@return whether the directory described by this source element should be included in the build}.
     */
    @Override
    public boolean enabled() {
        return enabled;
    }

    /**
     * {@return a hash code value computed from all properties}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                directory,
                includes,
                excludes,
                scope,
                language,
                moduleName,
                targetVersion,
                targetPath,
                stringFiltering,
                enabled);
    }

    /**
     * {@return whether the two objects are of the same class with equal property values}.
     *
     * @param obj the other object to compare with this object, or {@code null}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultSourceRoot other) {
            return directory.equals(other.directory)
                    && includes.equals(other.includes)
                    && excludes.equals(other.excludes)
                    && Objects.equals(scope, other.scope)
                    && Objects.equals(language, other.language)
                    && Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(targetVersion, other.targetVersion)
                    && stringFiltering == other.stringFiltering
                    && enabled == other.enabled;
        }
        return false;
    }
}
