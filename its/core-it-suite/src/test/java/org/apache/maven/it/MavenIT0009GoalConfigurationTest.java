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
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

public class MavenIT0009GoalConfigurationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test plugin configuration and goal configuration that overrides what the
     * mojo has specified.
     *
     * @throws Exception in case of failure
     */
    @Test
    @DisabledIf(
            value = "isWindowsWithJDK25",
            disabledReason = "JDK-25 - JDK-8354450 files ending with space are not supported on Windows")
    public void testit0009() throws Exception {

        // Inline version check: [3.1.0,) - current Maven version supports space in XML
        boolean supportSpaceInXml = true;

        File testDir = extractResources("/it0009");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyFilePresent(supportSpaceInXml ? "target/  pluginItem  " : "target/pluginItem");
        verifier.verifyFilePresent("target/goalItem");
        verifier.verifyFileNotPresent("target/bad-item");
        verifier.verifyErrorFreeLog();
    }

    static boolean isWindowsWithJDK25() {
        return OS.WINDOWS.isCurrentOs() && JRE.currentVersionNumber() >= 25;
    }
}
