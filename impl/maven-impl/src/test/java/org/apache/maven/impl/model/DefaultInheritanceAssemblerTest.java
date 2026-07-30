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
package org.apache.maven.impl.model;

import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultInheritanceAssemblerTest {

    private DefaultInheritanceAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new DefaultInheritanceAssembler();
    }

    @Test
    void testAssembleWithNullArtifactIdDoesNotThrowNpe() {
        Model parent = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("parent")
                .version("1.0")
                .build();

        Model child = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .version("1.0")
                .build();

        assertNotNull(assembler);
        assertDoesNotThrow(() -> assembler.assembleModelInheritance(child, parent, null, null));
    }

    @Test
    void testAssembleWithRootProjectDirectoryDoesNotThrowNpe() {
        Model parent = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("parent")
                .version("1.0")
                .build();

        // child has pomFile at root, so getProjectDirectory() returns root path
        // and getFileName() on root path returns null
        Model child = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("child")
                .version("1.0")
                .pomFile(Paths.get("/pom.xml"))
                .build();

        assertNotNull(assembler);
        assertDoesNotThrow(() -> assembler.assembleModelInheritance(child, parent, null, null));
    }

    @Test
    void testAssembleWithNullArtifactIdAndRootProjectDirectoryDoesNotThrowNpe() {
        Model parent = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("parent")
                .version("1.0")
                .modules(List.of("../child/pom.xml"))
                .build();

        Model child = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .version("1.0")
                .pomFile(Paths.get("/pom.xml"))
                .build();

        assertNotNull(assembler);
        assertDoesNotThrow(() -> assembler.assembleModelInheritance(child, parent, null, null));
    }
}
