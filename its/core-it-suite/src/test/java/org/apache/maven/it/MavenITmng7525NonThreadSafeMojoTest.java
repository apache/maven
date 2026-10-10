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
package org.apache.maven.it;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Verifies that parallel reactor builds serialize executions of a mojo that is not marked as thread-safe.
 */
class MavenITmng7525NonThreadSafeMojoTest extends AbstractMavenIntegrationTestCase {

    @Test
    void serializesNonThreadSafeMojoExecutions() throws Exception {
        Path projectDir = extractResources("mng-7525-non-thread-safe-mojo");

        Verifier verifier = newVerifier(projectDir);
        verifier.execute("install", "-pl", "plugin");
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir);
        verifier.execute("-T2", "validate");
        verifier.verifyErrorFreeLog();
    }
}
