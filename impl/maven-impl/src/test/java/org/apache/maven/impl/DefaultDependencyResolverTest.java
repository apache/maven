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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDependencyResolverTest {

    @Test
    void testFilterUninterpolatedRemovesPlaceholderVersion() {
        List<Dependency> deps = new ArrayList<>();
        deps.add(new Dependency(new DefaultArtifact("g", "a", "jar", "${unresolved}"), "compile"));
        deps.add(new Dependency(new DefaultArtifact("g", "b", "jar", "1.0"), "compile"));

        List<Dependency> result = DefaultDependencyResolver.filterUninterpolated(deps);

        assertEquals(1, result.size());
        assertEquals("b", result.get(0).getArtifact().getArtifactId());
    }

    @Test
    void testFilterUninterpolatedRemovesPlaceholderGroupId() {
        List<Dependency> deps = new ArrayList<>();
        deps.add(new Dependency(new DefaultArtifact("${group}", "a", "jar", "1.0"), "compile"));
        deps.add(new Dependency(new DefaultArtifact("g", "b", "jar", "1.0"), "compile"));

        List<Dependency> result = DefaultDependencyResolver.filterUninterpolated(deps);

        assertEquals(1, result.size());
        assertEquals("b", result.get(0).getArtifact().getArtifactId());
    }

    @Test
    void testFilterUninterpolatedRemovesPlaceholderArtifactId() {
        List<Dependency> deps = new ArrayList<>();
        deps.add(new Dependency(new DefaultArtifact("g", "${art}", "jar", "1.0"), "compile"));
        deps.add(new Dependency(new DefaultArtifact("g", "b", "jar", "1.0"), "compile"));

        List<Dependency> result = DefaultDependencyResolver.filterUninterpolated(deps);

        assertEquals(1, result.size());
        assertEquals("b", result.get(0).getArtifact().getArtifactId());
    }

    @Test
    void testFilterUninterpolatedKeepsValidDeps() {
        List<Dependency> deps = new ArrayList<>();
        deps.add(new Dependency(new DefaultArtifact("g", "a", "jar", "1.0"), "compile"));
        deps.add(new Dependency(new DefaultArtifact("g", "b", "jar", "2.0"), "test"));

        List<Dependency> result = DefaultDependencyResolver.filterUninterpolated(deps);

        assertEquals(2, result.size());
    }

    @Test
    void testFilterUninterpolatedHandlesNull() {
        assertNull(DefaultDependencyResolver.filterUninterpolated(null));
    }

    @Test
    void testFilterUninterpolatedHandlesEmpty() {
        List<Dependency> empty = new ArrayList<>();
        List<Dependency> result = DefaultDependencyResolver.filterUninterpolated(empty);
        assertSame(empty, result);
    }

    @Test
    void testContainsPlaceholder() {
        assertTrue(DefaultDependencyResolver.containsPlaceholder("${foo}"));
        assertTrue(DefaultDependencyResolver.containsPlaceholder("prefix-${foo}"));
        assertTrue(DefaultDependencyResolver.containsPlaceholder("${foo}-suffix"));
        assertFalse(DefaultDependencyResolver.containsPlaceholder("no-placeholder"));
        assertFalse(DefaultDependencyResolver.containsPlaceholder(""));
        assertFalse(DefaultDependencyResolver.containsPlaceholder(null));
    }
}
