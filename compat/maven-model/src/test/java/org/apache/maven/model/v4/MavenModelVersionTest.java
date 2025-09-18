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
package org.apache.maven.model.v4;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenModelVersionTest {

    private static Model model;

    @BeforeAll
    static void setup() throws Exception {
        try (InputStream is = MavenModelVersionTest.class.getResourceAsStream("/xml/pom.xml")) {
            model = new MavenStaxReader().read(is);
        }
    }

    @Test
    void testV4Model() {
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(model));
    }

    @Test
    void testV4ModelVersion() {
        Model m = model.withModelVersion("4.1.0");
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelRoot() {
        Model m = model.withRoot(true);
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelPreserveModelVersion() {
        Model m = model.withPreserveModelVersion(true);
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelPriority() {
        Model m = model.withBuild(Build.newInstance()
                .withPlugins(Collections.singleton(Plugin.newInstance()
                        .withExecutions(Collections.singleton(
                                PluginExecution.newInstance().withPriority(5))))));
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelWithNewMaven4Scopes() {
        // Test compile-only scope
        Dependency compileOnlyDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("compile-only-dep")
                .version("1.0.0")
                .scope("compile-only")
                .build();

        Model m1 = model.withDependencies(Arrays.asList(compileOnlyDep));
        // Should return "4.1.0" because compile-only scope requires Maven 4.1.0+
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m1));

        // Test test-only scope
        Dependency testOnlyDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("test-only-dep")
                .version("1.0.0")
                .scope("test-only")
                .build();

        Model m2 = model.withDependencies(Arrays.asList(testOnlyDep));
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m2));

        // Test test-runtime scope
        Dependency testRuntimeDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("test-runtime-dep")
                .version("1.0.0")
                .scope("test-runtime")
                .build();

        Model m3 = model.withDependencies(Arrays.asList(testRuntimeDep));
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m3));

        // Test new scopes in dependency management
        DependencyManagement depMgmt = DependencyManagement.newBuilder()
                .dependencies(Arrays.asList(compileOnlyDep))
                .build();

        Model m4 = model.withDependencyManagement(depMgmt);
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m4));
    }

    @Test
    void testV4ModelWithStandardScopes() {
        // Test that standard scopes don't require 4.1.0
        Dependency compileDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("compile-dep")
                .version("1.0.0")
                .scope("compile")
                .build();

        Model m1 = model.withDependencies(Arrays.asList(compileDep));
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(m1));

        Dependency testDep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("test-dep")
                .version("1.0.0")
                .scope("test")
                .build();

        Model m2 = model.withDependencies(Arrays.asList(testDep));
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(m2));
    }
}
