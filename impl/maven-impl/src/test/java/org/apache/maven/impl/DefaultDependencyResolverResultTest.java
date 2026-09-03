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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Focused tests for {@link DefaultDependencyResolverResult#getDispatchedPaths()} immutability
 * and insertion-order preservation.
 */
class DefaultDependencyResolverResultTest {

    @Test
    void getDispatchedPathsOuterMapIsUnmodifiable() throws Exception {
        DefaultDependencyResolverResult result = populatedResult();
        Map<PathType, List<Path>> dispatched = result.getDispatchedPaths();

        assertThrows(UnsupportedOperationException.class, dispatched::clear);
        assertThrows(UnsupportedOperationException.class, () -> dispatched.remove(JavaPathType.CLASSES));
        assertThrows(
                UnsupportedOperationException.class,
                () -> dispatched.put(JavaPathType.PROCESSOR_CLASSES, new ArrayList<>()));
    }

    @Test
    void getDispatchedPathsNestedListsAreUnmodifiable() throws Exception {
        Path first = Path.of("first.jar");
        Path second = Path.of("second.jar");
        DefaultDependencyResolverResult result = newResult();
        addDependency(result, "a", JavaPathType.CLASSES, first);
        addDependency(result, "b", JavaPathType.CLASSES, second);

        List<Path> classes = result.getDispatchedPaths().get(JavaPathType.CLASSES);

        assertThrows(UnsupportedOperationException.class, classes::clear);
        assertThrows(UnsupportedOperationException.class, () -> classes.add(Path.of("extra.jar")));
        assertThrows(UnsupportedOperationException.class, () -> classes.remove(0));

        assertEquals(List.of(first, second), result.getDispatchedPaths().get(JavaPathType.CLASSES));
        assertEquals(List.of(first, second), result.getPaths());
        assertEquals(2, result.getDependencies().size());
    }

    @Test
    void getDispatchedPathsPreservesInsertionOrder() throws Exception {
        Path modulesPath = Path.of("module.jar");
        Path classPath1 = Path.of("classes-1.jar");
        Path classPath2 = Path.of("classes-2.jar");
        DefaultDependencyResolverResult result = newResult();
        // MODULES is added first so key order is insertion order, not enum declaration order.
        addDependency(result, "mod", JavaPathType.MODULES, modulesPath);
        addDependency(result, "c1", JavaPathType.CLASSES, classPath1);
        addDependency(result, "c2", JavaPathType.CLASSES, classPath2);

        Map<PathType, List<Path>> dispatched = result.getDispatchedPaths();
        assertEquals(List.of(JavaPathType.MODULES, JavaPathType.CLASSES), List.copyOf(dispatched.keySet()));
        assertEquals(List.of(modulesPath), dispatched.get(JavaPathType.MODULES));
        assertEquals(List.of(classPath1, classPath2), dispatched.get(JavaPathType.CLASSES));
        assertEquals(List.of(modulesPath, classPath1, classPath2), result.getPaths());
    }

    @Test
    void getDispatchedPathsDoesNotAffectSiblingViews() throws Exception {
        Path first = Path.of("first.jar");
        Path second = Path.of("second.jar");
        DefaultDependencyResolverResult result = newResult();
        addDependency(result, "a", JavaPathType.CLASSES, first);
        addDependency(result, "b", JavaPathType.CLASSES, second);

        Map<PathType, List<Path>> dispatched = result.getDispatchedPaths();
        assertThrows(UnsupportedOperationException.class, dispatched::clear);
        assertThrows(
                UnsupportedOperationException.class,
                () -> dispatched.get(JavaPathType.CLASSES).clear());

        assertEquals(List.of(first, second), result.getPaths());
        assertEquals(2, result.getDependencies().size());
        assertTrue(result.getDependencies().containsValue(first));
        assertTrue(result.getDependencies().containsValue(second));
        assertThrows(
                UnsupportedOperationException.class, () -> result.getPaths().add(Path.of("x.jar")));
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.getDependencies().clear());
    }

    private static DefaultDependencyResolverResult populatedResult() throws Exception {
        DefaultDependencyResolverResult result = newResult();
        addDependency(result, "a", JavaPathType.CLASSES, Path.of("a.jar"));
        addDependency(result, "b", JavaPathType.MODULES, Path.of("b.jar"));
        return result;
    }

    private static DefaultDependencyResolverResult newResult() {
        return new DefaultDependencyResolverResult(
                mock(DependencyResolverRequest.class),
                new PathModularizationCache(Runtime.version()),
                new ArrayList<>(),
                mock(Node.class),
                4);
    }

    private static void addDependency(
            DefaultDependencyResolverResult result, String artifactId, PathType pathType, Path path) throws Exception {
        Dependency dep = mock(Dependency.class);
        Type type = mock(Type.class);
        when(dep.getGroupId()).thenReturn("g");
        when(dep.getArtifactId()).thenReturn(artifactId);
        when(dep.getType()).thenReturn(type);
        when(type.getPathTypes()).thenReturn(Set.of(pathType));
        result.addDependency(mock(Node.class), dep, t -> true, path);
    }
}
