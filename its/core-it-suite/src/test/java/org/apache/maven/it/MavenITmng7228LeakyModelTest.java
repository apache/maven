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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenITmng7228LeakyModelTest extends AbstractMavenIntegrationTestCase {

    protected MavenITmng7228LeakyModelTest() {
        // broken: 4.0.0-alpha-3 - 4.0.0-alpha-6
        super();
    }

    @Test
    void testLeakyModel() throws Exception {
        File testDir = extractResources("/mng-7228-leaky-model");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true); // TODO: why?

        verifier.addCliArgument("-e");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(new File(testDir, "settings.xml").getAbsolutePath());
        verifier.addCliArgument("-Pmanual-profile");

        verifier.addCliArgument("install");

        verifier.execute();

        verifier.verifyErrorFreeLog();

        String classifier = "build";
        String pom = FileUtils.readFileToString(new File(
                verifier.getArtifactPath("org.apache.maven.its.mng7228", "test", "1.0.0-SNAPSHOT", "pom", classifier)));

        assertTrue(pom.contains("projectProperty"));
        assertFalse(pom.contains("activeProperty"), "POM should not contain activeProperty but was: " + pom);
        assertFalse(pom.contains("manualProperty"), "POM should not contain manualProperty but was: " + pom);

        assertTrue(pom.contains("project-repo"));
        assertFalse(pom.contains("active-repo"), "POM should not contain active-repo but was: " + pom);
        assertFalse(pom.contains("manual-repo"), "POM should not contain manual-repo but was: " + pom);

        assertTrue(pom.contains("project-plugin-repo"));
        assertFalse(pom.contains("active-plugin-repo"), "POM should not contain active-plugin-repo but was: " + pom);
        assertFalse(pom.contains("manual-plugin-repo"), "POM should not contain manual-plugin-repo but was: " + pom);
    }
}
