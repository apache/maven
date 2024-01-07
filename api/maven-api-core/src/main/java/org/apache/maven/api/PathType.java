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
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * The option of a command-line tool where to place the paths to some dependencies.
 * A {@code PathType} can identify the Java class-path, the Java module-path,
 * or another kind of path for another programming language for example.
 * This class is like an enumeration, except that it is extensible:
 * plugins can define their own kinds of path.
 *
 * <p>Path types are often exclusive. For example, a dependency should not be both
 * on the Java class-path and on the Java module-path.</p>
 *
 * @see org.apache.maven.api.services.DependencyResolverResult#getDispatchedPaths()
 *
 * @since 4.0.0
 */
@Experimental
public abstract class PathType {
    /**
     * The programmatic name of this path type.
     */
    @Nonnull
    private final String name;

    /**
     * The tools option for this path, or {@code null} if none.
     *
     * @see #option()
     */
    final String option;

    /**
     * Creates a new enumeration value for a path associated to the given tool option.
     *
     * @param name the programmatic name of this path type
     * @param option the tool option for this path, or {@code null} if none
     */
    protected PathType(final String name, final String option) {
        this.name = Objects.requireNonNull(name);
        this.option = option;
    }

    /**
     * Returns the programmatic name of this path type. For example, if this path type
     * is {@link JavaPathType#MODULES}, then this method returns {@code "MODULES"}.
     *
     * @return the programmatic name of this path type
     *
     * @see #toString()
     */
    @Nonnull
    public String name() {
        return name;
    }

    /**
     * Returns the name of the tool option for this path. For example, if this path type
     * is {@link JavaPathType#MODULES}, then this method returns {@code "--module-path"}.
     * The option does not include the {@linkplain JavaPathType#moduleName() module name}
     * on which it applies.
     *
     * @return the name of the tool option for this path type
     */
    @Nonnull
    public Optional<String> option() {
        return Optional.ofNullable(option);
    }

    /**
     * Returns the option followed by a string representation of the given path elements.
     * The path elements are separated by an option-specific or platform-specific separator.
     * If the given {@code paths} argument contains no element, then this method returns an empty string.
     *
     * @param paths the path to format as a string
     * @return the option associated to this path type followed by the given path elements,
     *         or an empty string if there is no path element.
     */
    @Nonnull
    public abstract String option(Iterable<? extends Path> paths);

    /**
     * Returns the programmatic name of this path type, including the class name. For example,
     * if this type is {@link JavaPathType#MODULES}, then this method returns {@code "JavaPathType.MODULES"}.
     *
     * @return the programmatic name of this path type
     */
    @Nonnull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '.' + name;
    }

    /**
     * Returns a hash code value for this type.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() + name.hashCode() + 31 * Objects.hashCode(option);
    }

    /**
     * Compares this type with the given object for equality.
     *
     * @param obj the object to compare with this type
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final PathType other = (PathType) obj;
            return name.equals(other.name) && Objects.equals(option, other.option);
        }
        return false;
    }
}
