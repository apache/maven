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
package org.apache.maven.internal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Information about the modules contained in a path element.
 * The path element may be a JAR file or a directory. Directories may use either package hierarchy
 * or module hierarchy, but not module source hierarchy. The latter is excluded because this class
 * is for path elements of compiled codes.
 */
final class PathModularization {
    /**
     * A unique constant for all non-modular dependencies.
     */
    public static final PathModularization NONE = new PathModularization();

    /**
     * Name of the file to use as a sentinel value for deciding if a directory or a JAR is modular.
     */
    private static final String MODULE_INFO = "module-info.class";

    /**
     * The attribute for automatic module name in {@code META-INF/MANIFEST.MF} files.
     */
    private static final Attributes.Name AUTO_MODULE_NAME = new Attributes.Name("Automatic-Module-Name");

    /**
     * Module information for the path specified at construction time.
     * This map is usually either empty if no module was found, or a singleton map.
     * It may however contain more than one entry if module hierarchy was detected,
     * in which case there is one key per sub-directory.
     *
     * <p>This map may contain null values if the constructor was invoked with {@code resolve}
     * parameter set to false. This is more efficient when only the module existence needs to
     * be tested, and module descriptors are not needed.</p>
     *
     * @see #getModuleNames()
     */
    private final Map<Path, String> descriptors;

    /**
     * Whether module hierarchy was detected. If false, then package hierarchy is assumed.
     * In a package hierarchy, the {@linkplain #descriptors} map has either zero or one entry.
     * In a module hierarchy, the descriptors map may have an arbitrary number of entries,
     * including one (so the map size cannot be used as a criterion).
     *
     * @see #isModuleHierarchy()
     */
    private final boolean isModuleHierarchy;

    /**
     * Constructs an empty instance for non-modular dependencies.
     *
     * @see #NONE
     */
    private PathModularization() {
        descriptors = Collections.emptyMap();
        isModuleHierarchy = false;
    }

    /**
     * Finds module information in the given JAR file, output directory, or test output directory.
     * If no module is found, or if module information cannot be extracted, then this constructor
     * builds an empty map.
     *
     * <p>If the {@code resolve} parameter value is {@code false}, then some or all map values may
     * be null instead of the actual module name. This option can avoid the cost of reading module
     * descriptors when only the modules existence needs to be verified.</p>
     *
     * <h4>Algorithm</h4>
     * If the given path is a directory, then there is a choice:
     * <ul>
     *   <li><b>Package hierarchy:</b> if a {@code module-info.class} file is found at the root,
     *       then builds a singleton map with the module name declared in that descriptor.</li>
     *   <li><b>Module hierarchy:</b> if {@code module-info.class} files are found in sub-directories,
     *       at a deep intentionally restricted to one level, then builds a map of module names found
     *       in the descriptor of each sub-directory.</li>
     * </ul>
     *
     * Otherwise if the given path is a JAR file, then there is a choice:
     * <ul>
     *   <li>If a {@code module-info.class} file is found in the root directory or in a
     *       {@code "META-INF/versions/{n}/"} subdirectory, builds a singleton map with
     *       the module name declared in that descriptor.</li>
     *   <li>Otherwise if an {@code "Automatic-Module-Name"} attribute is declared in the
     *       {@code META-INF/MANIFEST.MF} file, builds a singleton map with the value of that attribute.</li>
     * </ul>
     *
     * Otherwise builds an empty map.
     *
     * @param path directory or JAR file to test
     * @param resolve whether the module names are requested. If false, null values may be used instead
     * @throws IOException if an error occurred while reading the JAR file or the module descriptor
     */
    PathModularization(final Path path, final boolean resolve) throws IOException {
        if (Files.isDirectory(path)) {
            /*
             * Package hierarchy: only one module with descriptor at the root.
             * This is the layout of output directories in projects using the
             * classical (Java 8 and before) way to organize source files.
             */
            Path file = path.resolve(MODULE_INFO);
            if (Files.isRegularFile(file)) {
                String name = null;
                if (resolve) {
                    try (InputStream in = Files.newInputStream(file)) {
                        name = getModuleName(in);
                    }
                }
                descriptors = Collections.singletonMap(file, name);
                isModuleHierarchy = false;
                return;
            }
            /*
             * Module hierarchy: many modules, one per directory, with descriptor at the root of the sub-directory.
             * This is the layout of output directories in projects using the new (Java 9 and later) way to organize
             * source files.
             */
            if (Files.isDirectory(file)) {
                final Map<Path, String> names = new HashMap<>();
                try (Stream<Path> subdirs = Files.list(file)) {
                    subdirs.filter(Files::isDirectory).forEach((subdir) -> {
                        Path mf = subdir.resolve(MODULE_INFO);
                        if (Files.isRegularFile(mf)) {
                            String name = null;
                            if (resolve) {
                                try (InputStream in = Files.newInputStream(mf)) {
                                    name = getModuleName(in);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            names.put(mf, name);
                        }
                    });
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
                if (!names.isEmpty()) {
                    descriptors = Collections.unmodifiableMap(names);
                    isModuleHierarchy = true;
                    return;
                }
            }
        } else if (Files.isRegularFile(path)) {
            /*
             * JAR file: can contain only one module, with descriptor at the root.
             * If no descriptor, the "Automatic-Module-Name" manifest attribute is
             * taken as a fallback.
             */
            try (JarFile jar = new JarFile(path.toFile())) {
                final ZipEntry entry = jar.getEntry(MODULE_INFO);
                if (entry != null) {
                    String name = null;
                    if (resolve) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            name = getModuleName(in);
                        }
                    }
                    descriptors = Collections.singletonMap(path, name);
                    isModuleHierarchy = false;
                    return;
                }
                // No module descriptor, check manifest file.
                final Manifest mf = jar.getManifest();
                if (mf != null) {
                    final Object name = mf.getMainAttributes().get(AUTO_MODULE_NAME);
                    if (name instanceof String) {
                        descriptors = Collections.singletonMap(path, (String) name);
                        isModuleHierarchy = false;
                        return;
                    }
                }
            }
        }
        descriptors = Collections.emptyMap();
        isModuleHierarchy = false;
    }

    /**
     * Returns the module name declared in the given {@code module-info} descriptor.
     * The input stream may be for a file or for an entry in a JAR file.
     */
    @Nonnull
    private static String getModuleName(final InputStream in) throws IOException {
        return ModuleDescriptor.read(in).name();
    }

    /**
     * Returns the type of path detected. The return value is {@link JavaPathType#MODULES}
     * if the dependency is a modular JAR file or a directory containing module descriptor(s),
     * or {@link JavaPathType#CLASSES} otherwise. A JAR file without module descriptor but with
     * an "Automatic-Module-Name" manifest attribute is considered modular.
     */
    public JavaPathType getPathType() {
        return descriptors.isEmpty() ? JavaPathType.CLASSES : JavaPathType.MODULES;
    }

    /**
     * Returns whether module hierarchy was detected. If false, then package hierarchy is assumed.
     * In a package hierarchy, the {@linkplain #getModuleNames()} map of modules has either zero or one entry.
     * In a module hierarchy, the descriptors map may have an arbitrary number of entries,
     * including one (so the map size cannot be used as a criterion).
     */
    public boolean isModuleHierarchy() {
        return isModuleHierarchy;
    }

    /**
     * Returns the module names for the path specified at construction time.
     * This map is usually either empty if no module was found, or a singleton map.
     * It may however contain more than one entry if module hierarchy was detected,
     * in which case there is one key per sub-directory.
     *
     * <p>This map may contain null values if the constructor was invoked with {@code resolve}
     * parameter set to false. This is more efficient when only the module existence needs to
     * be tested, and module descriptors are not needed.</p>
     */
    @Nonnull
    public Map<Path, String> getModuleNames() {
        return descriptors;
    }

    /**
     * Returns whether the dependency contains a module of the given name.
     */
    public boolean containsModule(final String name) {
        return descriptors.containsValue(name);
    }
}
