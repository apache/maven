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
     * The default value depends on whether a {@linkplain #module() module name} is specified in this source root:
     * <ul>
     *   <li>
     *     If no module name, then the default directory is
     *     <code>src/{@linkplain #scope() scope}/{@linkplain #language() language}</code>.
     *   </li><li>
     *     If a module name is present, then the default directory is
     *     <code>src/{@linkplain #language() language}/{@linkplain #module() module}/{@linkplain #scope() scope}</code>.
     *   </li>
     * </ul>
     *
     * The default value is relative.
     * Implementation may override with absolute path instead.
     */
    default Path directory() {
        return module().map((module) -> Path.of("src", language().id(), module, scope().id()))
                .orElseGet(() -> Path.of("src", scope().id(), language().id()));
    }

    /**
     * {@return the list of pattern matchers for the files to include}.
     * The path separator is {@code /} on all platforms, including Windows.
     * The patterns are used to match paths relative to the {@code directory}.
     * The prefix before the {@code :} character, if present, is the syntax.
     * If no syntax is specified, the default is a Maven-specific variation
     * of the {@code "glob"} pattern.
     *
     * <p>
     * The default implementation returns an empty list, which means to apply a language-dependent pattern.
     * For example, for the Java language, the pattern includes all files with the {@code .java} suffix.
     */
    default List<String> includes() {
        return List.of();
    }

    /**
     * {@return the list of pattern matchers for the files to exclude}.
     * The exclusions are applied after the inclusions.
     * The default implementation returns an empty list.
     */
    default List<String> excludes() {
        return List.of();
    }

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
