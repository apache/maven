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
package org.apache.maven.testing.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
class MojoExtensionModelTest {

    private static final String COORDINATES = "test:test-plugin:0.0.1-SNAPSHOT:goal";

    private static final String MINIMAL_POM = "<project>"
            + "<groupId>customGroupId</groupId>"
            + "<artifactId>customArtifactId</artifactId>"
            + "<version>2.0</version>"
            + "</project>";

    @Inject
    Project project;

    @Test
    void beforeEachUsesDefaultModelWhenNoPom() {
        assertNotNull(project);
        Model model = project.getModel();
        assertNotNull(model);
        assertEquals("myGroupId", model.getGroupId());
        assertEquals("myArtifactId", model.getArtifactId());
        assertEquals("1.0-SNAPSHOT", model.getVersion());
        assertEquals("jar", model.getPackaging());
        assertNotNull(model.getBuild());
        assertNotNull(model.getBuild().getDirectory());
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = MINIMAL_POM)
    void mergedModelIncludesDefaultBuildPaths() {
        Model model = project.getModel();
        assertEquals("customGroupId", model.getGroupId());
        assertEquals("customArtifactId", model.getArtifactId());
        assertEquals("2.0", model.getVersion());
        assertNotNull(model.getBuild());
        assertTrue(
                Paths.get(model.getBuild().getDirectory()).endsWith(Paths.get("target")),
                "Build directory from defaultModel should be present after merge");
        assertTrue(
                Paths.get(model.getBuild().getOutputDirectory()).endsWith(Paths.get("target", "classes")),
                "Output directory from defaultModel should be present after merge");
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = MINIMAL_POM)
    void storedModelHasAlignedPaths() {
        Model model = project.getModel();
        assertNotNull(model.getBuild());
        Path buildDir = Paths.get(model.getBuild().getDirectory());
        assertTrue(buildDir.isAbsolute(), "Build directory should be absolute after path alignment");
    }
}
