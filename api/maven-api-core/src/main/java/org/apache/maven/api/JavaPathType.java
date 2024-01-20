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

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * The option of a Java command-line tool where to place the paths to some dependencies.
 * A {@code PathType} can identify the class-path, the module-path, the patches for a specific module,
 * or another kind of path. This class is like an enumeration, except that it is extensible:
 * plugins can define their own kinds of path.
 *
 * <p>One path type is handled in a special way: unlike other options,
 * the paths specified in a {@code --patch-module} Java option is effective only for a specified module.
 * This type is created by calls to {@link #patchModule(String)} and a new instance must be created for
 * every module to patch.</p>
 *
 * <p>Path types are often exclusive. For example, a dependency should not be both on the Java class-path
 * and on the Java module-path.</p>
 *
 * @see org.apache.maven.api.services.DependencyResolverResult#getDispatchedPaths()
 *
 * @since 4.0.0
 */
@Experimental
public final class JavaPathType extends PathType {
    /**
     * The path identified by the Java {@code --class-path} option.
     * Used for compilation, execution and Javadoc among others.
     *
     * <h4>Context-sensitive interpretation</h4>
     * A dependency with this path type will not necessarily be placed on the class-path.
     * There are two circumstances where the dependency may nevertheless be placed somewhere else:
     *
     * <ul>
     *   <li>If {@link #MODULES} path type is also set, then the dependency can be placed either on the
     *       class-path or on the module-path, but only one of those. The choice is up to the plugin,
     *       possibly using heuristic rules (Maven 3 behavior).</li>
     *   <li>If a {@link #patchModule(String)} is also set and the main JAR file is placed on the module-path,
     *       then the test dependency will be placed on the Java {@code --patch-module} option instead of the
     *       class-path.</li>
     * </ul>
     */
    public static final JavaPathType CLASSES = new JavaPathType("CLASSES", "--class-path", null);

    /**
     * The path identified by the Java {@code --module-path} option.
     * Used for compilation, execution and Javadoc among others.
     *
     * <h4>Context-sensitive interpretation</h4>
     * A dependency with this flag will not necessarily be placed on the module-path.
     * There are two circumstances where the dependency may nevertheless be placed somewhere else:
     *
     * <ul>
     *   <li>If {@link #CLASSES} path type is also set, then the dependency <em>should</em> be placed on the
     *       module-path, but is also compatible with placement on the class-path. Compatibility can
     *       be achieved, for example, by repeating in the {@code META-INF/services/} directory the services
     *       that are declared in the {@code module-info.class} file. In that case, the path type can be chosen
     *       by the plugin.</li>
     *   <li>If a {@link #patchModule(String)} is also set and the main JAR file is placed on the module-path,
     *       then the test dependency will be placed on the Java {@code --patch-module} option instead of the
     *       {@code --module-path} option.</li>
     * </ul>
     */
    public static final JavaPathType MODULES = new JavaPathType("MODULES", "--module-path", null);

    /**
     * The path identified by the Java {@code --upgrade-module-path} option.
     */
    public static final JavaPathType UPGRADE_MODULES =
            new JavaPathType("UPGRADE_MODULES", "--upgrade-module-path", null);

    /**
     * The path identified by the Java {@code --patch-module} option.
     * Note that this option is incomplete, because it must be followed by a module name.
     * Use this type only when the module to patch is unknown.
     *
     * @see #patchModule(String)
     */
    public static final JavaPathType PATCH_MODULE = new JavaPathType("PATCH_MODULE", "--patch-module", null);

    /**
     * The path identified by the Java {@code --processor-path} option.
     */
    public static final JavaPathType PROCESSOR_CLASSES =
            new JavaPathType("PROCESSOR_CLASSES", "--processor-path", null);

    /**
     * The path identified by the Java {@code --processor-module-path} option.
     */
    public static final JavaPathType PROCESSOR_MODULES =
            new JavaPathType("PROCESSOR_MODULES", "--processor-module-path", null);

    /**
     * The path identified by the Java {@code -agentpath} option.
     */
    public static final JavaPathType AGENT = new JavaPathType("AGENT", "-agentpath", null);

    /**
     * The path identified by the Javadoc {@code -doclet} option.
     */
    public static final JavaPathType DOCLET = new JavaPathType("DOCLET", "-doclet", null);

    /**
     * The path identified by the Javadoc {@code -tagletpath} option.
     */
    public static final JavaPathType TAGLETS = new JavaPathType("TAGLETS", "-tagletpath", null);

    /**
     * All predefined enumeration values.
     */
    private static final JavaPathType[] VALUES = {
        CLASSES, MODULES, UPGRADE_MODULES, PROCESSOR_CLASSES, PROCESSOR_MODULES, AGENT, DOCLET, TAGLETS
    };

    /**
     * Creates a path identified by the Java {@code --patch-module} option.
     * Contrarily to the other types of paths, this path is applied to only
     * one specific module. Used for compilation and execution among others.
     *
     * <h4>Context-sensitive interpretation</h4>
     * This path type makes sense only when a main module is added on the module-path by another dependency.
     * In no main module is found, the patch dependency may be added on the class-path or module-path
     * depending on whether {@link #CLASSES} or {@link #MODULES} is present.
     *
     * @param moduleName name of the module on which to apply the path
     * @return an identification of the patch-module path for the given module.
     *
     * @see #moduleName()
     * @see #toString()
     */
    @Nonnull
    public static JavaPathType patchModule(final String moduleName) {
        return new JavaPathType("patchModule", "--patch-module", Objects.requireNonNull(moduleName));
    }

    /**
     * Creates a path for the specified option. If the {@code option} argument is one of the options
     * associated to above-listed enumeration values, then that value is returned. Otherwise, this
     * method returns a new {@code JavaPathType} instance for the given option.
     *
     * @param name the programmatic name of the new path type
     * @param option the option associated to the path type, or {@code null} if none
     * @return path type for the given option
     */
    @Nonnull
    public static JavaPathType forOption(final String name, final String option) {
        for (final JavaPathType value : VALUES) {
            if (value.option.equals(option)) {
                return value;
            }
        }
        return new JavaPathType(name, option, null);
    }

    /**
     * Name of the module to patch, or {@code null} if this path type is not for the {@code --patch-module} option.
     */
    private final String moduleName;

    /**
     * Creates a new enumeration value for a path associated to the given tool option.
     *
     * @param name the programmatic name of this path type
     * @param option the Java tools option for this path, or {@code null} if none
     * @param moduleName name of the module to patch, or {@code null} if none
     */
    private JavaPathType(final String name, final String option, final String moduleName) {
        super(name, option);
        this.moduleName = moduleName;
    }

    /**
     * Returns the name of the module on which this option applies.
     * This is present only for instances created by {@link #patchModule(String)}.
     *
     * @return name of the module on which this option applies
     */
    @Nonnull
    public Optional<String> moduleName() {
        return Optional.ofNullable(moduleName);
    }

    /**
     * Returns the programmatic name of this path type, including the class name and module to patch if any.
     * For example, if this type is {@link #MODULES}, then this method returns {@code JavaPathType.MODULES}.
     * If this type was created by {@code patchModule("foo.bar")}, then this method returns
     * {@code JavaPathType.patchModule("foo.bar")}.
     *
     * @return the programmatic name together with the module name on which it applies
     *
     * @see #name()
     * @see #moduleName()
     */
    @Nonnull
    @Override
    public String toString() {
        String s = super.toString();
        if (moduleName != null) {
            s = s + "(\"" + moduleName + "\")";
        }
        return s;
    }

    /**
     * Returns the option followed by a string representation of the given path elements. For example,
     * if this type is {@link #MODULES}, then the option is {@code "--module-path"}. But if this type is
     * {@code patchModule("foo.bar")}, then the option is {@code "--patch-module foo.bar"}. The option is followed
     * by the given path elements, separated by the {@linkplain File#separatorChar platform-specific separator}.
     * If the given {@code paths} argument contains no element, then this method returns an empty string.
     *
     * @param paths the path to format as a tool option
     * @return the option associated to this path type followed by the given path elements,
     *         or an empty string if there is no path element
     * @throws IllegalStateException if no option is associated to this path type
     */
    @Nonnull
    @Override
    public String option(final Iterable<? extends Path> paths) {
        if (option == null) {
            throw new IllegalStateException("No option is associated to this path type.");
        }
        String prefix = (moduleName == null) ? (option + ' ') : (option + ' ' + moduleName + '=');
        StringJoiner joiner = new StringJoiner(File.pathSeparator, prefix, "");
        joiner.setEmptyValue("");
        for (Path p : paths) {
            joiner.add(p.toString());
        }
        return joiner.toString();
    }

    /**
     * Returns a hash code value for this type.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37 * Objects.hashCode(moduleName);
    }

    /**
     * Compares this type with the given object for equality.
     *
     * @param obj the object to compare with this type
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final JavaPathType other = (JavaPathType) obj;
            return Objects.equals(moduleName, other.moduleName);
        }
        return false;
    }
}
