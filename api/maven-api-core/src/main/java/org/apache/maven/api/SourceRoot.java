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
import java.util.List;
import java.util.Optional;

/**
 * A root directory of source files.
 * The sources may be Java main classes, test classes, resources or anything else identified by the scope.
 */
public interface SourceRoot {
    /**
     * {@return the root directory where the sources are stored}.
     * The path is relative to the <abbr>POM</abbr> file.
     */
    Path directory();

    /**
     * {@return the list of pattern matchers for the files to include}.
     * The default implementation returns an empty list, which means to apply a language-dependent pattern.
     * For example, for the Java language, the pattern includes all files with the {@code .java} suffix.
     */
    default List<PathMatcher> includes() {
        return List.of();
    }

    /**
     * {@return the list of pattern matchers for the files to exclude}.
     * The exclusions are applied after the inclusions.
     * The default implementation returns an empty list.
     */
    default List<PathMatcher> excludes() {
        return List.of();
    }

    /**
     * {@return in which context the source files will be used}.
     * The default value is {@link ProjectScope#MAIN}.
     */
    default ProjectScope scope() {
        return ProjectScope.MAIN;
    }

    /**
     * {@return the language of the source files}.
     */
    Language language();

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
