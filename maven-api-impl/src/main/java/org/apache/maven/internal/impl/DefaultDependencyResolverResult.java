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
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathType;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;

/**
 * The result of collecting dependencies with a dependency resolver.
 * New instances are initially empty. Callers must populate with calls
 * to the following methods, in that order:
 *
 * <ul>
 *   <li>{@link #addOutputDirectory(Path, Path)} (optional)</li>
 *   <li>{@link #addDependency(Node, Dependency, Predicate, Path)}</li>
 * </ul>
 *
 * @see DefaultDependencyResolver#resolve(DependencyResolverRequest)
 */
class DefaultDependencyResolverResult implements DependencyResolverResult {
    /**
     * The exceptions that occurred while building the dependency graph.
     */
    private final List<Exception> exceptions;

    /**
     * The root node of the dependency graph.
     */
    private final Node root;

    /**
     * The ordered list of the flattened dependency nodes.
     */
    private final List<Node> nodes;

    /**
     * The file paths of all dependencies, regardless on which Java tool option those paths should be placed.
     */
    private final List<Path> paths;

    /**
     * The file paths of all dependencies, dispatched according the Java options where to place them.
     */
    private final Map<PathType, List<Path>> dispatchedPaths;

    /**
     * The dependencies together with the path to each dependency.
     */
    private final Map<Dependency, Path> dependencies;

    /**
     * Information about modules in the main output. This field is initially null and is set to a non-null
     * value when the output directories have been set, or when it is too late for setting them.
     */
    private PathModularization outputModules;

    /**
     * Cache of module information about each dependency.
     */
    private final PathModularizationCache cache;

    /**
     * Creates an initially empty result. Callers should add path elements by calls
     * to {@link #addDependency(Node, Dependency, Predicate, Path)}.
     *
     * @param cache cache of module information about each dependency
     * @param exceptions the exceptions that occurred while building the dependency graph
     * @param root the root node of the dependency graph
     * @param count estimated number of dependencies
     */
    DefaultDependencyResolverResult(PathModularizationCache cache, List<Exception> exceptions, Node root, int count) {
        this.cache = cache;
        this.exceptions = exceptions;
        this.root = root;
        nodes = new ArrayList<>(count);
        paths = new ArrayList<>(count);
        dispatchedPaths = new LinkedHashMap<>();
        dependencies = new LinkedHashMap<>(count + count / 3);
    }

    /**
     * Adds the given path element to the specified type of path.
     *
     * @param type the type of path (class-path, module-path, …)
     * @param path the path element to add
     */
    private void addPathElement(PathType type, Path path) {
        dispatchedPaths.computeIfAbsent(type, (t) -> new ArrayList<>()).add(path);
    }

    /**
     * Adds main and test output directories to the result. This method adds the main output directory
     * to the module-path if it contains a {@code module-info.class}, or to the class-path otherwise.
     * For the test output directory, the rules are more complex and are governed by the fact that
     * Java does not accept the placement of two modules of the same name on the module-path.
     * So the modular test output directory usually needs to be placed in a {@code --path-module} option.
     *
     * <ul>
     *   <li>If the test output directory is modular, then:
     *     <ul>
     *       <li>If a test module name is identical to a main module name,
     *           place the test directory in a {@code --patch-module} option.</li>
     *       <li>Otherwise, place the test directory on the module-path. However, this case
     *           (a module existing only in test output, not in main output) should be uncommon.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise (test output contains no module information), then:
     *     <ul>
     *       <li>If the main output is on the module-path, place the test output
     *           on a {@code --patch-module} option.</li>
     *       <li>Otherwise (main output on the class-path), place the test output on the class-path too.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * This method must be invoked before {@link #addDependency(Node, Dependency, Predicate, Path)}
     * if output directories are desired on the class-path or module-path.
     * This method can be invoked at most once.
     *
     * @param main the main output directory, or {@code null} if none
     * @param test the test output directory, or {@code null} if none
     * @throws IOException if an error occurred while reading module information
     *
     * TODO: this is currently not called. This is intended for use by Surefire and may move there.
     */
    void addOutputDirectory(Path main, Path test) throws IOException {
        if (outputModules != null) {
            throw new IllegalStateException("Output directories must be set first and only once.");
        }
        if (main != null) {
            outputModules = cache.getModuleInfo(main);
            addPathElement(outputModules.getPathType(), main);
        } else {
            outputModules = PathModularization.NONE;
        }
        if (test != null) {
            boolean addToClasspath = true;
            PathModularization testModules = cache.getModuleInfo(test);
            boolean isModuleHierarchy = outputModules.isModuleHierarchy || testModules.isModuleHierarchy;
            for (Object value : outputModules.descriptors.values()) {
                String moduleName = name(value);
                Path subdir = test;
                if (isModuleHierarchy) {
                    // If module hierarchy is used, the directory names shall be the module names.
                    Path path = test.resolve(moduleName);
                    if (!Files.isDirectory(path)) {
                        // Main module without tests. It is okay.
                        continue;
                    }
                    subdir = path;
                }
                // When the same module is found in main and test output, the latter is patching the former.
                addPathElement(JavaPathType.patchModule(moduleName), subdir);
                addToClasspath = false;
            }
            /*
             * If the test output directory provides some modules of its own, add them.
             * Except for this unusual case, tests should never be added to the module-path.
             */
            for (Map.Entry<Path, Object> entry : testModules.descriptors.entrySet()) {
                if (!outputModules.containsModule(name(entry.getValue()))) {
                    addPathElement(JavaPathType.MODULES, entry.getKey());
                    addToClasspath = false;
                }
            }
            if (addToClasspath) {
                addPathElement(JavaPathType.CLASSES, test);
            }
        }
    }

    /**
     * Adds a dependency node to the result.
     *
     * @param node the dependency node
     */
    void addNode(Node node) {
        nodes.add(node);
    }

    /**
     * Adds a dependency to the result. This method populates the {@link #nodes}, {@link #paths},
     * {@link #dispatchedPaths} and {@link #dependencies} collections with the given arguments.
     *
     * @param node the dependency node
     * @param dep the dependency for the given node, or {@code null} if none
     * @param filter filter the paths accepted by the tool which will consume the path.
     * @param path the path to the dependency, or {@code null} if the dependency was null
     * @throws IOException if an error occurred while reading module information
     */
    void addDependency(Node node, Dependency dep, Predicate<PathType> filter, Path path) throws IOException {
        nodes.add(node);
        if (dep == null) {
            return;
        }
        if (dependencies.put(dep, path) != null) {
            throw new IllegalStateException("Duplicated key: " + dep);
        }
        if (path == null) {
            return;
        }
        paths.add(path);
        /*
         * Dispatch the dependency to class-path, module-path, patch-module path, etc.
         * according the dependency properties. We need to process patch-module first,
         * because this type depends on whether a module of the same name has already
         * been added on the module-type.
         */
        // DependencyProperties properties = dep.getDependencyProperties();
        Set<PathType> pathTypes = dep.getType().getPathTypes();
        if (containsPatches(pathTypes)) {
            if (outputModules == null) {
                // For telling users that it is too late for setting the output directory.
                outputModules = PathModularization.NONE;
            }
            PathType type = null;
            for (Map.Entry<Path, Object> info :
                    cache.getModuleInfo(path).descriptors.entrySet()) {
                String moduleName = name(info.getValue());
                type = JavaPathType.patchModule(moduleName);
                if (!containsModule(moduleName)) {
                    /*
                     * Not patching an existing module. This case should be unusual. If it nevertheless
                     * happens, add on class-path or module-path if allowed, or keep patching otherwise.
                     * The latter case (keep patching) is okay if the main module will be defined later.
                     */
                    type = cache.selectPathType(pathTypes, filter, path).orElse(type);
                }
                addPathElement(type, info.getKey());
                // There is usually no more than one element, but nevertheless allow multi-modules.
            }
            /*
             * If the dependency has no module information, search for an artifact of the same groupId
             * and artifactId. If one is found, we are patching that module. If none is found, add the
             * dependency as a normal dependency.
             */
            if (type == null) {
                Path main = findArtifactPath(dep.getGroupId(), dep.getArtifactId());
                if (main != null) {
                    for (Map.Entry<Path, Object> info :
                            cache.getModuleInfo(main).descriptors.entrySet()) {
                        type = JavaPathType.patchModule(name(info.getValue()));
                        addPathElement(type, info.getKey());
                        // There is usually no more than one element, but nevertheless allow multi-modules.
                    }
                }
            }
            if (type != null) {
                return; // Dependency added, we are done.
            }
        }
        addPathElement(cache.selectPathType(pathTypes, filter, path).orElse(PathType.UNRESOLVED), path);
    }

    /**
     * Returns whether the given set of path types contains at least one patch for a module.
     */
    private boolean containsPatches(Set<PathType> types) {
        for (PathType type : types) {
            if (type instanceof JavaPathType.Modular) {
                type = ((JavaPathType.Modular) type).rawType();
            }
            if (JavaPathType.PATCH_MODULE.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether at least one previously added modular dependency contains a module of the given name.
     *
     * @param moduleName name of the module to search
     */
    private boolean containsModule(String moduleName) throws IOException {
        for (Path path : dispatchedPaths.getOrDefault(JavaPathType.MODULES, Collections.emptyList())) {
            if (cache.getModuleInfo(path).containsModule(moduleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches an artifact of the given group and artifact identifiers, and returns its path
     *
     * @param group the group identifier to search
     * @param artifact the artifact identifier to search
     * @return path to the desired artifact, or {@code null} if not found
     */
    private Path findArtifactPath(String group, String artifact) throws IOException {
        for (Map.Entry<Dependency, Path> entry : dependencies.entrySet()) {
            Dependency dep = entry.getKey();
            if (group.equals(dep.getGroupId()) && artifact.equals(dep.getArtifactId())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public List<Exception> getExceptions() {
        return exceptions;
    }

    @Override
    public Node getRoot() {
        return root;
    }

    @Override
    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public List<Path> getPaths() {
        return paths;
    }

    @Override
    public Map<PathType, List<Path>> getDispatchedPaths() {
        return dispatchedPaths;
    }

    @Override
    public Map<Dependency, Path> getDependencies() {
        return dependencies;
    }

    @Override
    public Optional<ModuleDescriptor> getModuleDescriptor(Path dependency) throws IOException {
        Object value = cache.getModuleInfo(dependency).descriptors.get(dependency);
        return (value instanceof ModuleDescriptor) ? Optional.of((ModuleDescriptor) value) : Optional.empty();
    }

    @Override
    public Optional<String> getModuleName(Path dependency) throws IOException {
        return Optional.ofNullable(
                name(cache.getModuleInfo(dependency).descriptors.get(dependency)));
    }

    /**
     * Returns the module name for the given value of the {@link PathModularization#descriptors} map.
     */
    private static String name(final Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof ModuleDescriptor) {
            return ((ModuleDescriptor) value).name();
        } else {
            return null;
        }
    }

    @Override
    public Optional<String> warningForFilenameBasedAutomodules() {
        try {
            return cache.warningForFilenameBasedAutomodules(dispatchedPaths.get(JavaPathType.MODULES));
        } catch (IOException e) {
            throw new DependencyResolverException("Cannot read module information.", e);
        }
    }
}
