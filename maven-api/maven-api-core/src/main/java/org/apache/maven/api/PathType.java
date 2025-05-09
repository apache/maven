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
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * The option of a command-line tool where to place the paths to some dependencies.
 * A {@code PathType} can identify the Java class-path, the Java module path,
 * or another kind of path for another programming language for example.
 * Path types are often exclusive. For example, a dependency should not be
 * both on the Java class path and on the Java module path.
 *
 * @see org.apache.maven.api.services.DependencyResolverResult#getDispatchedPaths()
 *
 * @since 4.0.0
 */
@Experimental
public interface PathType {
    /**
     * The type for all paths that could not be placed in any of the types requested by a caller.
     * This type can appear in the return value of a call to
     * {@link Session#resolveDependencies resolveDependencies(...)} when at least one dependency
     * cannot be associated to any type specified in the {@code desiredTypes} argument.
     * Plugins can choose to report a warning to users when unresolved paths exist.
     */
    PathType UNRESOLVED = new PathType() {
        @Override
        public String name() {
            return "UNRESOLVED";
        }

        @Override
        public String id() {
            return "UNRESOLVED";
        }

        @Override
        public Optional<String> option() {
            return Optional.empty();
        }

        @Override
        public String[] option(Iterable<? extends Path> paths) {
            return new String[0];
        }
    };

    /**
     * Returns the unique name of this path type, including the module to patch if any.
     * For example, if this type is {@link JavaPathType#MODULES}, then this method returns {@code "MODULES"}.
     * But if this type was created by {@code JavaPathType.patchModule("foo.bar")}, then this method returns
     * {@code "PATCH_MODULE:foo.bar"}.
     *
     * @return the programmatic name together with the module name on which it applies
     * @see #toString()
     */
    @Nonnull
    String id();

    /**
     * Returns the name of the tool option for this path. For example, if this path type
     * is {@link JavaPathType#MODULES}, then this method returns {@code "--module-path"}.
     * The option does not include the {@linkplain JavaPathType.Modular#moduleName() module name}
     * on which it applies.
     *
     * @return the name of the tool option for this path type
     */
    @Nonnull
    Optional<String> option();

    /**
     * Returns the option followed by a string representation of the given path elements.
     * The path elements are separated by an option-specific or platform-specific separator.
     * If the given {@code paths} argument contains no element, then this method returns an empty string.
     *
     * <h4>Examples</h4>
     * If {@code paths} is a list containing two elements, {@code dir/path1} and {@code dir/path2}, then:
     *
     * <ul>
     *   <li>If this type is {@link JavaPathType#MODULES}, then this method returns
     *       {@code {"--module-path", "dir/path1:dir/path2"}} on Unix or
     *       {@code {"--module-path", "dir\path1;dir\path2"}} on Windows.</li>
     *   <li>If this type was created by {@code JavaPathType.patchModule("foo.bar")}, then the method returns
     *       {@code {"--patch-module", "foo.bar=dir/path1:dir/path2"}} on Unix or
     *       {@code {"--patch-module", "foo.bar=dir\path1;dir\path2"}} on Windows.</li>
     * </ul>
     *
     * @param paths the path to format as a string
     * @return the option associated to this path type followed by the given path elements,
     *         or an empty array if there is no path element.
     */
    @Nonnull
    String[] option(Iterable<? extends Path> paths);

    /**
     * Returns the name of this path type. For example, if this path type
     * is {@link JavaPathType#MODULES}, then this method returns {@code "MODULES"}.
     *
     * @return the programmatic name of this path type
     */
    @Nonnull
    String name();

    /**
     * {@return a string representation for this extensible enum describing a path type}
     * For example {@code "PathType[PATCH_MODULE:foo.bar]"}.
     */
    @Nonnull
    @Override
    String toString();
}
