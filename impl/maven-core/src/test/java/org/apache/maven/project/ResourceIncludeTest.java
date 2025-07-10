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
package org.apache.maven.project;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the fix of issue #2486: Includes are not added to existing project resource.
 */
class ResourceIncludeTest {

    private MavenProject project;

    @BeforeEach
    void setUp() {
        project = new MavenProject();
        // Set a dummy pom file to establish the base directory
        project.setFile(new java.io.File("./pom.xml"));

        // Add a resource source root to the project
        project.addSourceRoot(
                new DefaultSourceRoot(ProjectScope.MAIN, Language.RESOURCES, Path.of("src/main/resources")));
    }

    @Test
    void testAddIncludeToExistingResource() {
        // Get the first resource
        List<Resource> resources = project.getResources();
        assertEquals(1, resources.size(), "Should have one resource");

        Resource resource = resources.get(0);
        assertEquals(Path.of("src/main/resources").toString(), resource.getDirectory());
        assertTrue(resource.getIncludes().isEmpty(), "Initially should have no includes");

        // Add an include - this should work now
        resource.addInclude("test");

        // Verify the include was added
        assertEquals(1, resource.getIncludes().size(), "Should have one include");
        assertEquals("test", resource.getIncludes().get(0), "Include should be 'test'");

        // Verify that getting resources again still shows the include
        List<Resource> resourcesAfter = project.getResources();
        assertEquals(1, resourcesAfter.size(), "Should still have one resource");
        Resource resourceAfter = resourcesAfter.get(0);
        assertEquals(1, resourceAfter.getIncludes().size(), "Should still have one include");
        assertEquals("test", resourceAfter.getIncludes().get(0), "Include should still be 'test'");
    }

    @Test
    void testAddMultipleIncludes() {
        Resource resource = project.getResources().get(0);

        // Add multiple includes
        resource.addInclude("*.xml");
        resource.addInclude("*.properties");

        // Verify both includes are present
        assertEquals(2, resource.getIncludes().size(), "Should have two includes");
        assertTrue(resource.getIncludes().contains("*.xml"), "Should contain *.xml");
        assertTrue(resource.getIncludes().contains("*.properties"), "Should contain *.properties");

        // Verify persistence
        Resource resourceAfter = project.getResources().get(0);
        assertEquals(2, resourceAfter.getIncludes().size(), "Should still have two includes");
        assertTrue(resourceAfter.getIncludes().contains("*.xml"), "Should still contain *.xml");
        assertTrue(resourceAfter.getIncludes().contains("*.properties"), "Should still contain *.properties");
    }

    @Test
    void testRemoveInclude() {
        Resource resource = project.getResources().get(0);

        // Add includes
        resource.addInclude("*.xml");
        resource.addInclude("*.properties");
        assertEquals(2, resource.getIncludes().size());

        // Remove one include
        resource.removeInclude("*.xml");

        // Verify only one include remains
        assertEquals(1, resource.getIncludes().size(), "Should have one include");
        assertEquals("*.properties", resource.getIncludes().get(0), "Should only have *.properties");

        // Verify persistence
        Resource resourceAfter = project.getResources().get(0);
        assertEquals(1, resourceAfter.getIncludes().size(), "Should still have one include");
        assertEquals("*.properties", resourceAfter.getIncludes().get(0), "Should still only have *.properties");
    }

    @Test
    void testSetIncludes() {
        Resource resource = project.getResources().get(0);

        // Set includes directly
        resource.setIncludes(List.of("*.txt", "*.md"));

        // Verify includes were set
        assertEquals(2, resource.getIncludes().size(), "Should have two includes");
        assertTrue(resource.getIncludes().contains("*.txt"), "Should contain *.txt");
        assertTrue(resource.getIncludes().contains("*.md"), "Should contain *.md");

        // Verify persistence
        Resource resourceAfter = project.getResources().get(0);
        assertEquals(2, resourceAfter.getIncludes().size(), "Should still have two includes");
        assertTrue(resourceAfter.getIncludes().contains("*.txt"), "Should still contain *.txt");
        assertTrue(resourceAfter.getIncludes().contains("*.md"), "Should still contain *.md");
    }

    @Test
    void testSourceRootOrderingPreserved() {
        // Add multiple resource source roots
        project.addSourceRoot(
                new DefaultSourceRoot(ProjectScope.MAIN, Language.RESOURCES, Path.of("src/main/resources2")));
        project.addSourceRoot(
                new DefaultSourceRoot(ProjectScope.MAIN, Language.RESOURCES, Path.of("src/main/resources3")));

        // Verify initial order
        List<Resource> resources = project.getResources();
        assertEquals(3, resources.size(), "Should have three resources");
        assertEquals(Path.of("src/main/resources").toString(), resources.get(0).getDirectory());
        assertEquals(Path.of("src/main/resources2").toString(), resources.get(1).getDirectory());
        assertEquals(Path.of("src/main/resources3").toString(), resources.get(2).getDirectory());

        // Modify the middle resource
        resources.get(1).addInclude("*.properties");

        // Verify order is preserved after modification
        List<Resource> resourcesAfter = project.getResources();
        assertEquals(3, resourcesAfter.size(), "Should still have three resources");
        assertEquals(
                Path.of("src/main/resources").toString(), resourcesAfter.get(0).getDirectory());
        assertEquals(
                Path.of("src/main/resources2").toString(), resourcesAfter.get(1).getDirectory());
        assertEquals(
                Path.of("src/main/resources3").toString(), resourcesAfter.get(2).getDirectory());

        // Verify the modification was applied to the correct resource
        assertTrue(
                resourcesAfter.get(1).getIncludes().contains("*.properties"),
                "Middle resource should have the include");
        assertTrue(resourcesAfter.get(0).getIncludes().isEmpty(), "First resource should not have includes");
        assertTrue(resourcesAfter.get(2).getIncludes().isEmpty(), "Third resource should not have includes");
    }

    @Test
    void testUnderlyingSourceRootsUpdated() {
        Resource resource = project.getResources().get(0);

        // Add an include
        resource.addInclude("*.xml");

        // Verify that the underlying SourceRoot collection was updated
        java.util.stream.Stream<org.apache.maven.api.SourceRoot> resourceSourceRoots =
                project.getEnabledSourceRoots(ProjectScope.MAIN, Language.RESOURCES);

        java.util.List<org.apache.maven.api.SourceRoot> sourceRootsList = resourceSourceRoots.toList();
        assertEquals(1, sourceRootsList.size(), "Should have one resource source root");

        org.apache.maven.api.SourceRoot sourceRoot = sourceRootsList.get(0);
        assertTrue(sourceRoot.includes().contains("*.xml"), "Underlying SourceRoot should contain the include");
    }
}
