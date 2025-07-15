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

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.LenientStubber;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void testMainJavaDirectory() {
        var source = new DefaultSourceRoot(
                session, Path.of("myproject"), Source.newBuilder().build());

        assertTrue(source.module().isEmpty());
        assertEquals(ProjectScope.MAIN, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "main", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testTestJavaDirectory() {
        var source = new DefaultSourceRoot(
                session, Path.of("myproject"), Source.newBuilder().scope("test").build());

        assertTrue(source.module().isEmpty());
        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "test", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testTestResourceDirectory() {
        var source = new DefaultSourceRoot(
                session,
                Path.of("myproject"),
                Source.newBuilder().scope("test").lang("resources").build());

        assertTrue(source.module().isEmpty());
        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.RESOURCES, source.language());
        assertEquals(Path.of("myproject", "src", "test", "resources"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testModuleMainDirectory() {
        var source = new DefaultSourceRoot(
                session,
                Path.of("myproject"),
                Source.newBuilder().module("org.foo.bar").build());

        assertEquals("org.foo.bar", source.module().orElseThrow());
        assertEquals(ProjectScope.MAIN, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "org.foo.bar", "main", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }

    @Test
    void testModuleTestDirectory() {
        var source = new DefaultSourceRoot(
                session,
                Path.of("myproject"),
                Source.newBuilder().module("org.foo.bar").scope("test").build());

        assertEquals("org.foo.bar", source.module().orElseThrow());
        assertEquals(ProjectScope.TEST, source.scope());
        assertEquals(Language.JAVA_FAMILY, source.language());
        assertEquals(Path.of("myproject", "src", "org.foo.bar", "test", "java"), source.directory());
        assertTrue(source.targetVersion().isEmpty());
    }
}
