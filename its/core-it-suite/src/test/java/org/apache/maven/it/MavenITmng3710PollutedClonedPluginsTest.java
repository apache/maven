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

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3710">MNG-3710</a>.
 *
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 * @since 2.0.8
 *
 */
public class MavenITmng3710PollutedClonedPluginsTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testitMNG3710POMInheritance() throws Exception {
        Path testDir = extractResources("/mng-3710/pom-inheritance");
        File pluginDir = testDir.resolve("maven-mng3710-pomInheritance-plugin");
        File projectsDir = testDir.resolve("projects");

        Verifier verifier;

        verifier = newVerifier(pluginDir.toString());
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectsDir.toString());
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        File topLevelTouchFile = projectsDir.resolve("target/touch.txt");
        assertFalse(topLevelTouchFile.exists(), "Top-level touch file should NOT be created in projects tree.");

        File midLevelTouchFile = projectsDir.resolve("middle/target/touch.txt");
        assertTrue(midLevelTouchFile.exists(), "Mid-level touch file should have been created in projects tree.");

        File childLevelTouchFile = projectsDir.resolve("middle/child/target/touch.txt");
        assertTrue(childLevelTouchFile.exists(), "Child-level touch file should have been created in projects tree.");
    }

    @Test
    public void testitMNG3710OriginalModel() throws Exception {
        Path testDir = extractResources("/mng-3710/original-model");
        File pluginsDir = testDir.resolve("plugins");
        File projectDir = testDir.resolve("project");

        Verifier verifier;

        verifier = newVerifier(pluginsDir.toString());
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.toString());

        verifier.addCliArguments("org.apache.maven.its.mng3710:mavenit-mng3710-directInvoke-plugin:1:run", "validate");

        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
