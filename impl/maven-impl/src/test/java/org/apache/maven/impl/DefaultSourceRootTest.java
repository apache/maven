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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.LenientStubber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class DefaultSourceRootTest {

    @Mock
    private Session session;

    @BeforeEach
    public void setup() {
        LenientStubber stub = Mockito.lenient();
        stub.when(session.requireProjectScope(eq("main"))).thenReturn(ProjectScope.MAIN);
        stub.when(session.requireProjectScope(eq("test"))).thenReturn(ProjectScope.TEST);
        stub.when(session.requireLanguage(eq("java"))).thenReturn(Language.JAVA_FAMILY);
        stub.when(session.requireLanguage(eq("resources"))).thenReturn(Language.RESOURCES);
    }

    /**
     * Returns the output directory relative to the base directory.
     */
    private static Function<ProjectScope, String> outputDirectory() {
        return (scope) -> {
            if (scope == ProjectScope.MAIN) {
                return "target/classes";
            } else if (scope == ProjectScope.TEST) {
                return "target/test-classes";
            } else {
                return "target";
            }
        };
    }

    @Test
    void testMainJavaDirectory() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().build());

        assertTrue(source.module().isEmpty());
        assertEquals(ProjectScope.MAIN, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "main", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testTestJavaDirectory() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().scope("test").build());

        assertTrue(source.module().isEmpty());
        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "test", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testTestResourceDirectory() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().scope("test").lang("resources").build());

        assertTrue(source.module().isEmpty());
        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.RESOURCES, source.language());
        assertEquals(Path.of("myproject", "src", "test", "resources"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testModuleMainDirectory() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().module("org.foo.bar").build());

        assertEquals("org.foo.bar", source.module().orElseThrow());
        assertEquals(ProjectScope.MAIN, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "org.foo.bar", "main", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testModuleTestDirectory() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().module("org.foo.bar").scope("test").build());

        assertEquals("org.foo.bar", source.module().orElseThrow());
        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "org.foo.bar", "test", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    /**
     * Tests that relative target paths are resolved against the right base directory.
     */
    @Test
    void testRelativeMainTargetPath() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().targetPath("user-output").build());

        assertEquals(ProjectScope.MAIN, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(
                Path.of("myproject", "target", "classes", "user-output"),
                source.targetPath().orElseThrow());
    }

    /**
     * Tests that relative target paths are resolved against the right base directory.
     */
    @Test
    void testRelativeTestTargetPath() {
        var source = DefaultSourceRoot.fromModel(
                session,
                Path.of("myproject"),
                outputDirectory(),
                Source.newBuilder().targetPath("user-output").scope("test").build());

        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(
                Path.of("myproject", "target", "test-classes", "user-output"),
                source.targetPath().orElseThrow());
    }

    /*MNG-11062*/
    @Test
    void testExtractsTargetPathFromResource() {
        // Test the Resource constructor that was broken in the regression
        Resource resource = Resource.newBuilder()
                .directory("src/test/resources")
                .targetPath("test-output")
                .build();

        DefaultSourceRoot sourceRoot = new DefaultSourceRoot(Path.of("myproject"), ProjectScope.TEST, resource);

        Optional<Path> targetPath = sourceRoot.targetPath();
        assertTrue(targetPath.isPresent(), "targetPath should be present");
        assertEquals(Path.of("myproject", "test-output"), targetPath.get());
        assertEquals(Path.of("myproject", "src", "test", "resources"), sourceRoot.directory());
        assertEquals(ProjectScope.TEST, sourceRoot.scope());
        assertEquals(Language.RESOURCES, sourceRoot.language());
    }

    /*MNG-11062*/
    @Test
    void testHandlesNullTargetPathFromResource() {
        // Test null targetPath handling
        Resource resource =
                Resource.newBuilder().directory("src/test/resources").build();
        // targetPath is null by default

        DefaultSourceRoot sourceRoot = new DefaultSourceRoot(Path.of("myproject"), ProjectScope.TEST, resource);

        Optional<Path> targetPath = sourceRoot.targetPath();
        assertFalse(targetPath.isPresent(), "targetPath should be empty when null");
    }

    /*MNG-11062*/
    @Test
    void testHandlesEmptyTargetPathFromResource() {
        // Test empty string targetPath
        Resource resource = Resource.newBuilder()
                .directory("src/test/resources")
                .targetPath("")
                .build();

        DefaultSourceRoot sourceRoot = new DefaultSourceRoot(Path.of("myproject"), ProjectScope.TEST, resource);

        Optional<Path> targetPath = sourceRoot.targetPath();
        assertFalse(targetPath.isPresent(), "targetPath should be empty for empty string");
    }

    /*MNG-11062*/
    @Test
    void testHandlesPropertyPlaceholderInTargetPath() {
        // Test property placeholder preservation
        Resource resource = Resource.newBuilder()
                .directory("src/test/resources")
                .targetPath("${project.build.directory}/custom")
                .build();

        DefaultSourceRoot sourceRoot = new DefaultSourceRoot(Path.of("myproject"), ProjectScope.MAIN, resource);

        Optional<Path> targetPath = sourceRoot.targetPath();
        assertTrue(targetPath.isPresent(), "Property placeholder targetPath should be present");
        assertEquals(Path.of("myproject", "${project.build.directory}/custom"), targetPath.get());
    }

    /*MNG-11062*/
    @Test
    void testResourceConstructorRequiresNonNullDirectory() {
        // Test that null directory throws exception
        Resource resource = Resource.newBuilder().build();
        // directory is null by default

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultSourceRoot(Path.of("myproject"), ProjectScope.TEST, resource),
                "Should throw exception for null directory");
    }

    /*MNG-11062*/
    @Test
    void testResourceConstructorPreservesOtherProperties() {
        // Test that other Resource properties are correctly preserved
        Resource resource = Resource.newBuilder()
                .directory("src/test/resources")
                .targetPath("test-classes")
                .filtering("true")
                .includes(List.of("*.properties"))
                .excludes(List.of("*.tmp"))
                .build();

        DefaultSourceRoot sourceRoot = new DefaultSourceRoot(Path.of("myproject"), ProjectScope.TEST, resource);

        // Verify all properties are preserved
        assertEquals(
                Path.of("myproject", "test-classes"), sourceRoot.targetPath().orElseThrow());
        assertTrue(sourceRoot.stringFiltering(), "Filtering should be true");
        assertEquals(1, sourceRoot.includes().size());
        assertTrue(sourceRoot.includes().contains("*.properties"));
        assertEquals(1, sourceRoot.excludes().size());
        assertTrue(sourceRoot.excludes().contains("*.tmp"));
    }
}
