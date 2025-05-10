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

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A root directory of source files.
 * The sources may be Java main classes, test classes, resources or anything else identified by the scope.
 *
 * <h2>Default values</h2>
 * The properties in this interface are defined in the {@code <Source>} element of the
 * {@linkplain org.apache.maven.api.model.Model Maven project descriptor}.
 * For each property, the default value is either empty or documented in the project descriptor.
 */
public interface SourceRoot {
    /**
     * {@return the root directory where the sources are stored}.
     * The path is relative to the <abbr>POM</abbr> file.
     *
     * <h4>Default implementation</h4>
     * The default value is <code>src/{@linkplain #scope() scope}/{@linkplain #language() language}</code>
     * as a relative path. Implementation classes may override this default with an absolute path instead.
     */
    default Path directory() {
        return Path.of("src", scope().id(), language().id());
    }

    /**
     * {@return the list of patterns for the files to include}.
     * The path separator is {@code /} on all platforms, including Windows.
     * The prefix before the {@code :} character, if present and longer than 1 character, is the syntax.
     * If no syntax is specified, or if its length is 1 character (interpreted as a Windows drive),
     * the default is a Maven-specific variation of the {@code "glob"} pattern.
     *
     * <p>
     * The default implementation returns an empty list, which means to apply a language-dependent pattern.
     * For example, for the Java language, the pattern includes all files with the {@code .java} suffix.
     *
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     */
    default List<String> includes() {
        return List.of();
    }

    /**
     * {@return the list of patterns for the files to exclude}.
     * The exclusions are applied after the inclusions.
     * The default implementation returns an empty list.
     */
    default List<String> excludes() {
        return List.of();
    }

    /**
     * {@return a matcher combining the include and exclude patterns}.
     * If the user did not specify any includes, the given {@code defaultIncludes} are used.
     * These defaults depend on the plugin.
     * For example, the default include of the Java compiler plugin is <code>"**&sol;*.java"</code>.
     *
     * <p>If the user did not specify any excludes, the default is often files generated
     * by Source Code Management (<abbr>SCM</abbr>) software or by the operating system.
     * Examples: <code>"**&sol;.gitignore"</code>, <code>"**&sol;.DS_Store"</code>.</p>
     *
     * @param defaultIncludes the default includes if unspecified by the user
     * @param useDefaultExcludes whether to add the default set of patterns to exclude,
     *        mostly Source Code Management (<abbr>SCM</abbr>) files
     */
    PathMatcher matcher(Collection<String> defaultIncludes, boolean useDefaultExcludes);

    /**
     * {@return in which context the source files will be used}.
     * Not to be confused with dependency scope.
     * The default value is {@code "main"}.
     *
     * @see ProjectScope#MAIN
     */
    default ProjectScope scope() {
        return ProjectScope.MAIN;
    }

    /**
     * {@return the language of the source files}.
     * The default value is {@code "java"}.
     *
     * @see Language#JAVA_FAMILY
     */
    default Language language() {
        return Language.JAVA_FAMILY;
    }

    /**
     * {@return the name of the Java module (or other language-specific module) which is built by the sources}.
     * The default value is empty.
     */
    default Optional<String> module() {
        return Optional.empty();
    }

    /**
     * {@return the version of the platform where the code will be executed}.
     * In a Java environment, this is the value of the {@code --release} compiler option.
     * The default value is empty.
     */
    default Optional<Version> targetVersion() {
        return Optional.empty();
    }

    /**
     * {@return an explicit target path, overriding the default value}.
     * When a target path is explicitly specified, the values of the {@link #module()} and {@link #targetVersion()}
     * elements are not used for inferring the path (they are still used as compiler options however).
     * It means that for scripts and resources, the files below the path specified by {@link #directory()}
     * are copied to the path specified by {@code targetPath()} with the exact same directory structure.
     */
    default Optional<Path> targetPath() {
        return Optional.empty();
    }

    /**
     * {@return whether resources are filtered to replace tokens with parameterized values}.
     * The default value is {@code false}.
     */
    default boolean stringFiltering() {
        return false;
    }

    /**
     * {@return whether the directory described by this source element should be included in the build}.
     * The default value is {@code true}.
     */
    default boolean enabled() {
        return true;
    }
}
