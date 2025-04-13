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
 */
public class MavenITmng3710PollutedClonedPluginsTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3710PollutedClonedPluginsTest() {
        super("(2.0.8,)"); // only test in 2.0.9+
    }

    @Test
    public void testitMNG3710POMInheritance() throws Exception {
        File testDir = extractResources("/mng-3710/pom-inheritance");
        File pluginDir = new File(testDir, "maven-mng3710-pomInheritance-plugin");
        File projectsDir = new File(testDir, "projects");

        Verifier verifier;

        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectsDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        File topLevelTouchFile = new File(projectsDir, "target/touch.txt");
        assertFalse(topLevelTouchFile.exists(), "Top-level touch file should NOT be created in projects tree.");

        File midLevelTouchFile = new File(projectsDir, "middle/target/touch.txt");
        assertTrue(midLevelTouchFile.exists(), "Mid-level touch file should have been created in projects tree.");

        File childLevelTouchFile = new File(projectsDir, "middle/child/target/touch.txt");
        assertTrue(childLevelTouchFile.exists(), "Child-level touch file should have been created in projects tree.");
    }

    @Test
    public void testitMNG3710OriginalModel() throws Exception {
        File testDir = extractResources("/mng-3710/original-model");
        File pluginsDir = new File(testDir, "plugins");
        File projectDir = new File(testDir, "project");

        Verifier verifier;

        verifier = newVerifier(pluginsDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        verifier.addCliArguments("org.apache.maven.its.mng3710:mavenit-mng3710-directInvoke-plugin:1:run", "validate");

        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
