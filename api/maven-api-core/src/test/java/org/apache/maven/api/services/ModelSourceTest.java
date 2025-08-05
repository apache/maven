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
package org.apache.maven.api.services;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for ModelSource interface and its implementations.
 */
class ModelSourceTest {

    @Test
    void testBuildSourceHasNoModelId() {
        Path path = Paths.get("/tmp/pom.xml");
        ModelSource source = Sources.buildSource(path);

        assertNotNull(source);
        assertNull(source.getModelId(), "Build sources should not have a modelId");
        assertEquals(path, source.getPath());
    }

    @Test
    void testResolvedSourceWithModelId() {
        String location = "/tmp/resolved-pom.xml";
        Path path = Paths.get(location);
        String modelId = "org.apache.maven:maven-core:4.0.0";

        ModelSource source = Sources.resolvedSource(path, modelId);

        assertNotNull(source);
        assertEquals(modelId, source.getModelId(), "Resolved source should return the provided modelId");
        assertNull(source.getPath(), "Resolved sources should return null for getPath()");
        assertEquals(path.toString(), source.getLocation());
    }

    @Test
    void testModelIdFormat() {
        String location = "/tmp/test.xml";
        Path path = Paths.get(location);
        String modelId = "com.example:test-artifact:1.2.3";

        ModelSource source = Sources.resolvedSource(path, modelId);

        assertEquals(modelId, source.getModelId());
        assertTrue(modelId.matches("^[^:]+:[^:]+:[^:]+$"), "ModelId should follow groupId:artifactId:version format");
    }
}
