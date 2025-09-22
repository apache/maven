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
package com.gitlab.tkslaw.ditests;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MojoTest
@DisplayName("Test InjectServiceMojo")
class InjectServiceMojoTests {

    @Test
    @InjectMojo(goal = "inject-service")
    @DisplayName("had its services injected by the DI container")
    void testServicesNotNull(InjectServiceMojo mojo) {
        // Preconditions
        assertAll(
                "Log, Session, and/or Project were not injected. This should not happen!",
                () -> assertNotNull(mojo.log, "log"),
                () -> assertNotNull(mojo.session, "session"),
                () -> assertNotNull(mojo.project, "project"));

        // Actual test
        assertDoesNotThrow(mojo::execute, "InjectServiceMojo::execute");
        assertAll(
                "Services not injected by DI container",
                () -> assertNotNull(mojo.artifactManager, "artifactManager"),
                () -> assertNotNull(mojo.dependencyResolver, "dependencyResolver"),
                () -> assertNotNull(mojo.toolchainManager, "toolchainManager"),
                () -> assertNotNull(mojo.osService, "osService"));
    }
}
