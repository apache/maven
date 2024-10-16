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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.PathType;

/**
 * Cache of {@link PathModularization} instances computed for given {@link Path} elements.
 * The cache is used for avoiding the need to reopen the same files many times when the
 * same dependency is used for different scope. For example a path used for compilation
 * is typically also used for tests.
 */
class PathModularizationCache {
    /**
     * Module information for each JAR file or output directories.
     * Cached when first requested to avoid decoding the module descriptors multiple times.
     *
     * @see #getModuleInfo(Path)
     */
    private final Map<Path, PathModularization> moduleInfo;

    /**
     * Whether JAR files are modular. This map is redundant with {@link #moduleInfo},
     * but cheaper to compute when the module names are not needed.
     *
     * @see #getPathType(Path)
     */
    private final Map<Path, PathType> pathTypes;

    /**
     * Creates an initially empty cache.
     */
    PathModularizationCache() {
        moduleInfo = new HashMap<>();
        pathTypes = new HashMap<>();
    }

    /**
     * Gets module information for the given JAR file or output directory.
     * Module descriptors are read when first requested, then cached.
     */
    PathModularization getModuleInfo(Path path) throws IOException {
        PathModularization info = moduleInfo.get(path);
        if (info == null) {
            info = new PathModularization(path, true);
            moduleInfo.put(path, info);
            pathTypes.put(path, info.getPathType());
        }
        return info;
    }

    /**
     * Returns {@link JavaPathType#MODULES} if the given JAR file or output directory is modular.
     * This is used in heuristic rules for deciding whether to place a dependency on the class-path
     * or on the module-path when the {@code "jar"} artifact type is used.
     */
    private PathType getPathType(Path path) throws IOException {
        PathType type = pathTypes.get(path);
        if (type == null) {
            type = new PathModularization(path, false).getPathType();
            pathTypes.put(path, type);
        }
        return type;
    }

    /**
     * Selects the type of path where to place the given dependency.
     * This method returns one of the values specified in the given collection.
     * This method does not handle the patch-module paths, because the patches
     * depend on which modules have been previously added on the module-paths.
     *
     * <p>If the dependency can be a constituent of both the class-path and the module-path,
     * then the path type is determined by checking if the dependency is modular.</p>
     *
     * @param types types of path where a dependency can be placed
     * @param filter filter the paths accepted by the tool which will consume the path
     * @param path path to the JAR file or output directory of the dependency
     * @return where to place the dependency, or an empty value if the placement cannot be determined
     * @throws IOException if an error occurred while reading module information
     */
    Optional<PathType> selectPathType(Set<PathType> types, Predicate<PathType> filter, Path path) throws IOException {
        PathType selected = null;
        boolean classes = false;
        boolean modules = false;
        boolean unknown = false;
        boolean processorClasses = false;
        boolean processorModules = false;
        for (PathType type : types) {
            if (filter.test(type)) {
                if (JavaPathType.CLASSES.equals(type)) {
                    classes = true;
                } else if (JavaPathType.MODULES.equals(type)) {
                    modules = true;
                } else if (JavaPathType.PROCESSOR_CLASSES.equals(type)) {
                    processorClasses = true;
                } else if (JavaPathType.PROCESSOR_MODULES.equals(type)) {
                    processorModules = true;
                } else {
                    unknown = true;
                }
                if (selected == null) {
                    selected = type;
                } else if (unknown) {
                    // More than one filtered value, and we don't know how to handle at least one of them.
                    // TODO: add a plugin mechanism for allowing plugin to specify their selection algorithm.
                    return Optional.empty();
                }
            }
        }
        /*
         * If the dependency can be both on the class-path and the module-path, we need to chose one of these.
         * The choice done below will overwrite the current `selected` value because the latter is only the
         * first value encountered in iteration order, which may be random.
         */
        if (classes | modules) {
            if (classes & modules) {
                selected = getPathType(path);
            } else if (classes) {
                selected = JavaPathType.CLASSES;
            } else {
                selected = JavaPathType.MODULES;
            }
        } else if (processorClasses & processorModules) {
            selected = getPathType(path);
            if (JavaPathType.CLASSES.equals(selected)) {
                selected = JavaPathType.PROCESSOR_CLASSES;
            } else if (JavaPathType.MODULES.equals(selected)) {
                selected = JavaPathType.PROCESSOR_MODULES;
            }
        }
        return Optional.ofNullable(selected);
    }

    /**
     * If the module-path contains a filename-based auto-module, prepares a warning message.
     * It is caller's responsibility to send the message to a logger.
     *
     * @param modulePaths content of the module path, or {@code null} if none
     * @return warning message if at least one filename-based auto-module was found
     * @throws IOException if an error occurred while reading module information
     */
    Optional<String> warningForFilenameBasedAutomodules(Collection<Path> modulePaths) throws IOException {
        if (modulePaths == null) {
            return Optional.empty();
        }
        var automodulesDetected = new ArrayList<String>();
        for (Path p : modulePaths) {
            getModuleInfo(p).addIfFilenameBasedAutomodules(automodulesDetected);
        }
        if (automodulesDetected.isEmpty()) {
            return Optional.empty();
        }
        String lineSeparator = System.lineSeparator();
        var joiner = new StringJoiner(
                lineSeparator + "  - ",
                "Filename-based automodules detected on the module-path: " + lineSeparator + "  - ",
                lineSeparator + "Please don't publish this project to a public artifact repository.");
        automodulesDetected.forEach(joiner::add);
        return Optional.of(joiner.toString());
    }
}
