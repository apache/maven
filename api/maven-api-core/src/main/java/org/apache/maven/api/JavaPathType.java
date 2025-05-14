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

import javax.tools.DocumentationTool;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * The option of a Java command-line tool where to place the paths to some dependencies.
 * A {@code PathType} can identify the class path, the module path, the patches for a specific module,
 * or another kind of path.
 *
 * <p>One path type is handled in a special way: unlike other options,
 * the paths specified in a {@code --patch-module} Java option is effective only for a specified module.
 * This type is created by calls to {@link #patchModule(String)} and a new instance must be created for
 * every module to patch.</p>
 *
 * <p>Path types are often exclusive. For example, a dependency should not be both on the Java class path
 * and on the Java module path.</p>
 *
 * <h2>Relationship with Java compiler standard location</h2>
 * This enumeration is closely related to the {@link JavaFileManager.Location} enumerations.
 * A difference is that the latter enumerates input and output files, while {@code JavaPathType}
 * enumerates only input dependencies. Another difference is that {@code JavaPathType} contains
 * some enumeration values used only at runtime and therefore not available in {@code javax.tool},
 * such as agent paths.
 *
 * @see org.apache.maven.api.services.DependencyResolverResult#getDispatchedPaths()
 *
 * @since 4.0.0
 */
@Experimental
public enum JavaPathType implements PathType {
    /**
     * The path identified by the Java {@code --class-path} option.
     * Used for compilation, execution and Javadoc among others.
     * The Java tools location is {@link StandardLocation#CLASS_PATH}.
     *
     * <h4>Context-sensitive interpretation</h4>
     * A dependency with this path type will not necessarily be placed on the class path.
     * There are two circumstances where the dependency may nevertheless be placed somewhere else:
     *
     * <ul>
     *   <li>If {@link #MODULES} path type is also set, then the dependency can be placed either on the
     *       class path or on the module path, but only one of those. The choice is up to the plugin,
     *       possibly using heuristic rules (Maven 3 behavior).</li>
     *   <li>If a {@link #patchModule(String)} is also set and the main JAR file is placed on the module path,
     *       then the test dependency will be placed on the Java {@code --patch-module} option instead of the
     *       class path.</li>
     * </ul>
     */
    CLASSES(StandardLocation.CLASS_PATH, "--class-path"),

    /**
     * The path identified by the Java {@code --module-path} option.
     * Used for compilation, execution and Javadoc among others.
     * The Java tools location is {@link StandardLocation#MODULE_PATH}.
     *
     * <h4>Context-sensitive interpretation</h4>
     * A dependency with this flag will not necessarily be placed on the module path.
     * There are two circumstances where the dependency may nevertheless be placed somewhere else:
     *
     * <ul>
     *   <li>If {@link #CLASSES} path type is also set, then the dependency <em>should</em> be placed on the
     *       module path, but is also compatible with placement on the class path. Compatibility can
     *       be achieved, for example, by repeating in the {@code META-INF/services/} directory the services
     *       that are declared in the {@code module-info.class} file. In that case, the path type can be chosen
     *       by the plugin.</li>
     *   <li>If a {@link #patchModule(String)} is also set and the main JAR file is placed on the module path,
     *       then the test dependency will be placed on the Java {@code --patch-module} option instead of the
     *       {@code --module-path} option.</li>
     * </ul>
     */
    MODULES(StandardLocation.MODULE_PATH, "--module-path"),

    /**
     * The path identified by the Java {@code --upgrade-module-path} option.
     * The Java tools location is {@link StandardLocation#UPGRADE_MODULE_PATH}.
     */
    UPGRADE_MODULES(StandardLocation.UPGRADE_MODULE_PATH, "--upgrade-module-path"),

    /**
     * The path identified by the Java {@code --patch-module} option.
     * The Java tools location is {@link StandardLocation#PATCH_MODULE_PATH}.
     * <p>
     * Note that this option is incomplete, because it must be followed by a module name.
     * Use this type only when the module to patch is unknown.
     *
     * @see #patchModule(String)
     */
    PATCH_MODULE(StandardLocation.PATCH_MODULE_PATH, "--patch-module"),

    /**
     * The path identified by the Java {@code --processor-path} option.
     * The Java tools location is {@link StandardLocation#ANNOTATION_PROCESSOR_PATH}.
     */
    PROCESSOR_CLASSES(StandardLocation.ANNOTATION_PROCESSOR_PATH, "--processor-path"),

    /**
     * The path identified by the Java {@code --processor-module-path} option.
     * The Java tools location is {@link StandardLocation#ANNOTATION_PROCESSOR_MODULE_PATH}.
     */
    PROCESSOR_MODULES(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, "--processor-module-path"),

    /**
     * The path identified by the Java {@code -agentpath} option.
     */
    AGENT(null, "-agentpath"),

    /**
     * The path identified by the Javadoc {@code -doclet} option.
     * The Java tools location is {@link DocumentationTool.Location#DOCLET_PATH}.
     */
    DOCLET(DocumentationTool.Location.DOCLET_PATH, "-doclet"),

    /**
     * The path identified by the Javadoc {@code -tagletpath} option.
     * The Java tools location is {@link DocumentationTool.Location#TAGLET_PATH}.
     */
    TAGLETS(DocumentationTool.Location.TAGLET_PATH, "-tagletpath");

    /**
     * Creates a path identified by the Java {@code --patch-module} option.
     * Contrarily to the other types of paths, this path is applied to only
     * one specific module. Used for compilation and execution among others.
     *
     * <h4>Context-sensitive interpretation</h4>
     * This path type makes sense only when a main module is added on the module path by another dependency.
     * In no main module is found, the patch dependency may be added on the class path or module path
     * depending on whether {@link #CLASSES} or {@link #MODULES} is present.
     *
     * @param moduleName name of the module on which to apply the path
     * @return an identification of the patch-module path for the given module.
     *
     * @see Modular#moduleName()
     */
    @Nonnull
    public static Modular patchModule(@Nonnull String moduleName) {
        return PATCH_MODULE.new Modular(moduleName);
    }

    /**
     * The {@code javax.tool} enumeration value corresponding to this {@code JavaPathType}, or {@code null} if none.
     *
     * @see #location()
     */
    private final JavaFileManager.Location location;

    /**
     * The tools option for this path, or {@code null} if none.
     *
     * @see #option()
     */
    private final String option;

    /**
     * Creates a new enumeration value for a path associated to the given tool option.
     *
     * @param location the {@code javax.tool} enumeration value, or {@code null} if none.
     * @param option the Java tools option for this path, or {@code null} if none
     */
    JavaPathType(JavaFileManager.Location location, String option) {
        this.location = location;
        this.option = option;
    }

    /**
     * Returns the unique name of this path type.
     *
     * @return the programmatic name of this enumeration value
     */
    @Override
    public String id() {
        return name();
    }

    /**
     * Returns the identification of this path in the {@code javax.tool} API.
     * The value may be an instance of {@link StandardLocation} or {@link DocumentationTool.Location},
     * depending on which tool will use this location.
     *
     * @return the {@code javax.tool} enumeration value corresponding to this {@code JavaPathType}
     */
    public Optional<JavaFileManager.Location> location() {
        return Optional.ofNullable(location);
    }

    /**
     * Returns the path type associated to the given {@code javax.tool} location.
     * This method is the converse of {@link #location()}.
     *
     * @param location identification of a path in the {@code javax.tool} API
     * @return Java path type associated to the given location
     */
    public static Optional<JavaPathType> valueOf(JavaFileManager.Location location) {
        for (JavaPathType type : JavaPathType.values()) {
            if (location.equals(type.location)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the name of the tool option for this path. For example, if this path type
     * is {@link #MODULES}, then this method returns {@code "--module-path"}. The option
     * does not include the {@linkplain Modular#moduleName() module name} on which it applies.
     *
     * @return the name of the tool option for this path type
     */
    @Nonnull
    @Override
    public Optional<String> option() {
        return Optional.ofNullable(option);
    }

    /**
     * Returns the option followed by a string representation of the given path elements.
     * For example, if this type is {@link #MODULES}, then the option is {@code "--module-path"}
     * followed by the specified path elements.
     *
     * @param paths the path to format as a tool option
     * @return the option associated to this path type followed by the given path elements,
     *         or an empty array if there is no path element
     * @throws IllegalStateException if no option is associated to this path type
     */
    @Nonnull
    @Override
    public String[] option(Iterable<? extends Path> paths) {
        return format(null, paths);
    }

    /**
     * Implementation shared with {@link Modular}.
     */
    final String[] format(String moduleName, Iterable<? extends Path> paths) {
        if (option == null) {
            throw new IllegalStateException("No option is associated to this path type.");
        }
        String prefix = (moduleName == null) ? "" : (moduleName + '=');
        StringJoiner joiner = new StringJoiner(File.pathSeparator, prefix, "");
        joiner.setEmptyValue("");
        for (Path p : paths) {
            joiner.add(p.toString());
        }
        String value = joiner.toString();
        if (value.isEmpty()) {
            return new String[0];
        }
        return new String[] {option, value};
    }

    /**
     * {@return a string representation of this path type for debugging purposes}.
     */
    @Override
    public String toString() {
        return "PathType[" + id() + "]";
    }

    /**
     * Type of path which is applied to only one specific Java module.
     * The main case is the Java {@code --patch-module} option.
     *
     * @see #PATCH_MODULE
     * @see #patchModule(String)
     */
    public final class Modular implements PathType {
        /**
         * Name of the module for which a path is specified.
         */
        @Nonnull
        private final String moduleName;

        /**
         * Creates a new path type for the specified module.
         *
         * @param moduleName name of the module for which a path is specified
         */
        private Modular(@Nonnull String moduleName) {
            this.moduleName = Objects.requireNonNull(moduleName);
        }

        /**
         * Returns the type of path without indication about the target module.
         * This is usually {@link #PATCH_MODULE}.
         *
         * @return type of path without indication about the target module
         */
        @Nonnull
        public JavaPathType rawType() {
            return JavaPathType.this;
        }

        /**
         * Returns the name of the tool option for this path, including the module name.
         *
         * @return name of the tool option for this path, including the module name
         */
        @Override
        public String id() {
            return JavaPathType.this.name() + ":" + moduleName;
        }

        /**
         * Returns the name of the tool option for this path, not including the module name.
         *
         * @return name of the tool option for this path, not including the module name
         */
        @Nonnull
        @Override
        public String name() {
            return JavaPathType.this.name();
        }

        /**
         * Returns the name of the module for which a path is specified
         *
         * @return name of the module for which a path is specified
         */
        @Nonnull
        public String moduleName() {
            return moduleName;
        }

        /**
         * Returns the name of the tool option for this path.
         * The option does not include the {@linkplain #moduleName() module name} on which it applies.
         *
         * @return the name of the tool option for this path type
         */
        @Nonnull
        @Override
        public Optional<String> option() {
            return JavaPathType.this.option();
        }

        /**
         * Returns the option followed by a string representation of the given path elements.
         * The path elements are separated by an option-specific or platform-specific separator.
         * If the given {@code paths} argument contains no element, then this method returns an empty string.
         *
         * @param paths the path to format as a string
         * @return the option associated to this path type followed by the given path elements,
         *         or an empty array if there is no path element.
         */
        @Nonnull
        @Override
        public String[] option(Iterable<? extends Path> paths) {
            return format(moduleName, paths);
        }

        /**
         * Returns the programmatic name of this path type, including the module to patch.
         * For example, if this type was created by {@code JavaPathType.patchModule("foo.bar")},
         * then this method returns {@code "PathType[PATCH_MODULE:foo.bar]")}.
         *
         * @return the programmatic name together with the module name on which it applies
         */
        @Nonnull
        @Override
        public String toString() {
            return "PathType[" + id() + "]";
        }
    }
}
