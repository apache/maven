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
import java.util.Collection;
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
class PathModularization {
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
     * Filename of the path specified at construction time.
     */
    private final String filename;

    /**
     * Module information for the path specified at construction time.
     * This map is usually either empty if no module was found, or a singleton map.
     * It may however contain more than one entry if module hierarchy was detected,
     * in which case there is one key per sub-directory.
     *
     * <p>Values are instances of either {@link ModuleDescriptor} or {@link String}.
     * The latter case happens when a JAR file has no {@code module-info.class} entry
     * but has an automatic name declared in {@code META-INF/MANIFEST.MF}.</p>
     *
     * <p>This map may contain null values if the constructor was invoked with {@code resolve}
     * parameter set to false. This is more efficient when only the module existence needs to
     * be tested, and module descriptors are not needed.</p>
     */
    @Nonnull
    final Map<Path, Object> descriptors;

    /**
     * Whether module hierarchy was detected. If false, then package hierarchy is assumed.
     * In a package hierarchy, the {@linkplain #descriptors} map has either zero or one entry.
     * In a module hierarchy, the descriptors map may have an arbitrary number of entries,
     * including one (so the map size cannot be used as a criterion).
     */
    final boolean isModuleHierarchy;

    /**
     * Constructs an empty instance for non-modular dependencies.
     *
     * @see #NONE
     */
    private PathModularization() {
        filename = "(none)";
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
     * <p><b>Algorithm:</b>
     * If the given path is a directory, then there is a choice:
     * </p>
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
    PathModularization(Path path, boolean resolve) throws IOException {
        filename = path.getFileName().toString();
        if (Files.isDirectory(path)) {
            /*
             * Package hierarchy: only one module with descriptor at the root.
             * This is the layout of output directories in projects using the
             * classical (Java 8 and before) way to organize source files.
             */
            Path file = path.resolve(MODULE_INFO);
            if (Files.isRegularFile(file)) {
                ModuleDescriptor descriptor = null;
                if (resolve) {
                    try (InputStream in = Files.newInputStream(file)) {
                        descriptor = ModuleDescriptor.read(in);
                    }
                }
                descriptors = Collections.singletonMap(file, descriptor);
                isModuleHierarchy = false;
                return;
            }
            /*
             * Module hierarchy: many modules, one per directory, with descriptor at the root of the sub-directory.
             * This is the layout of output directories in projects using the new (Java 9 and later) way to organize
             * source files.
             */
            if (Files.isDirectory(file)) {
                var multi = new HashMap<Path, ModuleDescriptor>();
                try (Stream<Path> subdirs = Files.list(file)) {
                    subdirs.filter(Files::isDirectory).forEach((subdir) -> {
                        Path mf = subdir.resolve(MODULE_INFO);
                        if (Files.isRegularFile(mf)) {
                            ModuleDescriptor descriptor = null;
                            if (resolve) {
                                try (InputStream in = Files.newInputStream(mf)) {
                                    descriptor = ModuleDescriptor.read(in);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            multi.put(mf, descriptor);
                        }
                    });
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
                if (!multi.isEmpty()) {
                    descriptors = Collections.unmodifiableMap(multi);
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
                ZipEntry entry = jar.getEntry(MODULE_INFO);
                if (entry != null) {
                    ModuleDescriptor descriptor = null;
                    if (resolve) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            descriptor = ModuleDescriptor.read(in);
                        }
                    }
                    descriptors = Collections.singletonMap(path, descriptor);
                    isModuleHierarchy = false;
                    return;
                }
                // No module descriptor, check manifest file.
                Manifest mf = jar.getManifest();
                if (mf != null) {
                    Object name = mf.getMainAttributes().get(AUTO_MODULE_NAME);
                    if (name instanceof String) {
                        descriptors = Collections.singletonMap(path, name);
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
     * {@return the type of path detected}. The return value is {@link JavaPathType#MODULES}
     * if the dependency is a modular JAR file or a directory containing module descriptor(s),
     * or {@link JavaPathType#CLASSES} otherwise. A JAR file without module descriptor but with
     * an "Automatic-Module-Name" manifest attribute is considered modular.
     */
    public JavaPathType getPathType() {
        return descriptors.isEmpty() ? JavaPathType.CLASSES : JavaPathType.MODULES;
    }

    /**
     * If the module has no name, adds the filename of the JAR file in the given collection.
     * This method should be invoked for dependencies placed on {@link JavaPathType#MODULES}
     * for preparing a warning asking to not deploy the build artifact on a public repository.
     * If the module has an explicit name either with a {@code module-info.class} file or with
     * an {@code "Automatic-Module-Name"} attribute in the {@code META-INF/MANIFEST.MF} file,
     * then this method does nothing.
     */
    public void addIfFilenameBasedAutomodules(Collection<String> automodulesDetected) {
        if (descriptors.isEmpty()) {
            automodulesDetected.add(filename);
        }
    }

    /**
     * {@return whether the dependency contains a module of the given name}.
     */
    public boolean containsModule(String name) {
        return descriptors.containsValue(name);
    }

    /**
     * {@return a string representation of this object for debugging purposes}.
     * This string representation may change in any future version.
     */
    @Override
    public String toString() {
        return getClass().getCanonicalName() + '[' + filename + ']';
    }
}
