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
package org.apache.maven.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathType;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.impl.resolver.type.DefaultType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for {@link DefaultDependencyResolverResult}. */
public class DefaultDependencyResolverResultTest {

    private static DefaultType createJarType(PathType pathType) {
        return new DefaultType("jar", org.apache.maven.api.Language.JAVA_FAMILY, "jar", null, false, pathType);
    }

    @Test
    public void testAddDependencyWithNullDependencyAddsNodeOnly() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        Node node = mock(Node.class);
        // addDependency with null dependency should only add the node
        result.addDependency(node, null, (Predicate<PathType>) (t) -> true, null);

        assertEquals(1, result.getNodes().size());
        assertEquals(0, result.getDependencies().size());
        assertEquals(0, result.getPaths().size());
        assertTrue(result.getDispatchedPaths().isEmpty());
    }

    @Test
    public void testAddDependencyDuplicateThrows() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        Dependency dep = mock(Dependency.class);
        when(dep.getGroupId()).thenReturn("g");
        when(dep.getArtifactId()).thenReturn("a");
        when(dep.getType()).thenReturn(createJarType(JavaPathType.MODULES));

        Node node = mock(Node.class);
        Path p = Files.createTempFile("dup", ".jar");

        result.addDependency(node, dep, (Predicate<PathType>) (t) -> true, p);

        // adding the same dependency again should throw
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> result.addDependency(node, dep, (Predicate<PathType>) (t) -> true, p));
        assertTrue(ex.getMessage().contains("Duplicated key"));
    }

    @Test
    public void testAddDependencyWithAutomaticModuleNameAndGetModuleName() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        // create a jar with Automatic-Module-Name manifest attribute
        Path jar = Files.createTempFile("auto-module", ".jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), "auto.mod");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar), mf)) {
            // empty jar with manifest
        }

        Dependency dep = mock(Dependency.class);
        when(dep.getGroupId()).thenReturn("g");
        when(dep.getArtifactId()).thenReturn("a");
        when(dep.getType()).thenReturn(createJarType(JavaPathType.MODULES));

        Node node = mock(Node.class);

        result.addDependency(node, dep, (Predicate<PathType>) (t) -> true, jar);

        assertEquals(1, result.getDependencies().size());
        assertEquals(1, result.getPaths().size());

        Optional<String> moduleName = result.getModuleName(jar);
        assertTrue(moduleName.isPresent());
        assertEquals("auto.mod", moduleName.get());

        Optional<java.lang.module.ModuleDescriptor> descriptor = result.getModuleDescriptor(jar);
        assertTrue(descriptor.isEmpty());
    }

    @Test
    public void testSelectPathTypeUnknownsBecomeUnresolved() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        Dependency dep = mock(Dependency.class);
        when(dep.getGroupId()).thenReturn("g.u");
        when(dep.getArtifactId()).thenReturn("a.u");
        Path p = Files.createTempFile("unres", ".jar");

        // Type returns a known CLASSES and an unknown custom PathType => selectPathType should return empty
        when(dep.getType()).thenReturn(createJarType(JavaPathType.UNRESOLVED));

        Node node = mock(Node.class);
        result.addDependency(node, dep, (Predicate<PathType>) (t) -> true, p);

        assertTrue(result.getDispatchedPaths().containsKey(org.apache.maven.api.PathType.UNRESOLVED));
        assertTrue(result.getDispatchedPaths()
                .get(org.apache.maven.api.PathType.UNRESOLVED)
                .contains(p));
    }

    @Test
    public void testAddOutputDirectoryWithNullMainPlacesTestOnClasspath() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        Path testDir = Files.createTempDirectory("test-out");
        result.addOutputDirectory(null, testDir);

        assertTrue(result.getDispatchedPaths().containsKey(JavaPathType.CLASSES));
        assertEquals(1, result.getDispatchedPaths().get(JavaPathType.CLASSES).size());
        assertEquals(
                testDir, result.getDispatchedPaths().get(JavaPathType.CLASSES).get(0));
    }

    @Test
    public void testAddOutputDirectoryCalledTwiceThrows() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        Path testDir = Files.createTempDirectory("test-out2");
        result.addOutputDirectory(null, testDir);

        assertThrows(IllegalStateException.class, () -> result.addOutputDirectory(null, testDir));
    }

    @Test
    public void testReturnedCollectionsAreUnmodifiable() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        Node n1 = mock(Node.class);
        Node n2 = mock(Node.class);
        result.addNode(n1);
        result.addNode(n2);

        // nodes list should be unmodifiable
        assertThrows(
                UnsupportedOperationException.class, () -> result.getNodes().add(mock(Node.class)));

        // add a dependency to populate paths
        Dependency dep = mock(Dependency.class);
        when(dep.getGroupId()).thenReturn("g3");
        when(dep.getArtifactId()).thenReturn("a3");
        when(dep.getType()).thenReturn(createJarType(JavaPathType.CLASSES));

        Path p = Files.createTempFile("path", ".jar");
        result.addDependency(n1, dep, (Predicate<PathType>) (t) -> true, p);

        assertThrows(
                UnsupportedOperationException.class, () -> result.getPaths().add(Path.of("x")));
    }

    @Test
    public void testAddOutputDirectoryWithMainPlacesMainOnModulePath() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 4);

        // create a jar with Automatic-Module-Name manifest attribute to simulate a modular main output
        Path mainJar = Files.createTempFile("main-module", ".jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), "main.mod");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(mainJar), mf)) {
            // empty jar with manifest
        }

        result.addOutputDirectory(mainJar, null);

        // main output should have been placed on the module path
        assertTrue(result.getDispatchedPaths().containsKey(JavaPathType.MODULES));
        assertEquals(1, result.getDispatchedPaths().get(JavaPathType.MODULES).size());
        assertEquals(
                mainJar, result.getDispatchedPaths().get(JavaPathType.MODULES).get(0));
    }

    @Test
    public void testAddDependencyPatchingExistingModuleUsesPatchModule() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 8);

        // first dependency: modular artifact that will be placed on module path
        Path moduleJar1 = Files.createTempFile("mod1", ".jar");
        Manifest mf1 = new Manifest();
        mf1.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf1.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), "modA");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(moduleJar1), mf1)) {
            // empty jar with manifest
        }

        Dependency dep1 = mock(Dependency.class);
        when(dep1.getGroupId()).thenReturn("g1");
        when(dep1.getArtifactId()).thenReturn("a1");
        when(dep1.getType()).thenReturn(createJarType(JavaPathType.MODULES));

        Node node = mock(Node.class);
        // add first dependency -> should be on MODULES
        result.addDependency(node, dep1, (Predicate<PathType>) (t) -> true, moduleJar1);

        // second dependency: a patch-module for the same module name "modA"
        Path moduleJar2 = Files.createTempFile("mod2", ".jar");
        Manifest mf2 = new Manifest();
        mf2.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf2.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), "modA");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(moduleJar2), mf2)) {
            // empty jar with manifest
        }

        Dependency dep2 = mock(Dependency.class);
        when(dep2.getGroupId()).thenReturn("g2");
        when(dep2.getArtifactId()).thenReturn("a2");
        when(dep2.getType())
                .thenReturn(new DefaultType(
                        "jar",
                        org.apache.maven.api.Language.JAVA_FAMILY,
                        "jar",
                        null,
                        false,
                        JavaPathType.PATCH_MODULE));

        // add second dependency -> should detect existing module and dispatch as patch-module(modA)
        result.addDependency(node, dep2, (Predicate<PathType>) (t) -> true, moduleJar2);

        JavaPathType.Modular patchForModA = JavaPathType.patchModule("modA");
        assertTrue(result.getDispatchedPaths().containsKey(patchForModA));
        assertTrue(result.getDispatchedPaths().get(patchForModA).contains(moduleJar2));
    }

    @Test
    public void testAddDependencyPatchingByArtifactWhenNoModuleInfoButMatchingArtifactExists() throws Exception {
        DependencyResolverRequest req = mock(DependencyResolverRequest.class);
        List<Exception> exceptions = new ArrayList<>();
        Node root = mock(Node.class);
        PathModularizationCache cache = new PathModularizationCache(Runtime.version());

        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(req, cache, exceptions, root, 8);

        // main artifact (provides module info)
        Path mainJar = Files.createTempFile("main-artifact", ".jar");
        Manifest mfMain = new Manifest();
        mfMain.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mfMain.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), "modB");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(mainJar), mfMain)) {
            // empty jar with manifest
        }

        Dependency mainDep = mock(Dependency.class);
        when(mainDep.getGroupId()).thenReturn("gX");
        when(mainDep.getArtifactId()).thenReturn("aX");
        when(mainDep.getType())
                .thenReturn(new DefaultType(
                        "jar", org.apache.maven.api.Language.JAVA_FAMILY, "jar", null, false, JavaPathType.MODULES));

        Node node = mock(Node.class);
        // add main artifact
        result.addDependency(node, mainDep, (Predicate<PathType>) (t) -> true, mainJar);

        // patch artifact which has no module info itself
        Path patchJar = Files.createTempFile("patch-no-modinfo", ".jar");
        // create an empty jar without Automatic-Module-Name
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(patchJar), new Manifest())) {
            // empty jar
        }

        Dependency patchDep = mock(Dependency.class);
        when(patchDep.getGroupId()).thenReturn("gX");
        when(patchDep.getArtifactId()).thenReturn("aX"); // same identifiers -> findArtifactPath should find mainDep
        when(patchDep.getType())
                .thenReturn(new DefaultType(
                        "jar",
                        org.apache.maven.api.Language.JAVA_FAMILY,
                        "jar",
                        null,
                        false,
                        JavaPathType.PATCH_MODULE));

        // add the patch dependency; since it has no module info, findArtifactPath should pick up mainJar and add a
        // patch for modB
        result.addDependency(node, patchDep, (Predicate<PathType>) (t) -> true, patchJar);

        JavaPathType.Modular patchForModB = JavaPathType.patchModule("modB");
        assertTrue(result.getDispatchedPaths().containsKey(patchForModB));
        // The code will add the main artifact's descriptor path for patching (info.getKey()), assert mainJar present
        assertTrue(result.getDispatchedPaths().get(patchForModB).contains(mainJar));
    }
}
