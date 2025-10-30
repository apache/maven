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

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/2576">GH-2576</a>.
 * <p>
 * The issue occurs when a project has a dependency which defines a custom repository needed to load its parent.
 * The -itr option should not use any transitive repository, so this project should fail.
 *
 */
class MavenITgh2576ItrNotHonoredTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testItrNotHonored() throws Exception {
        File testDir = extractResources("/gh-2576-itr-not-honored").getAbsoluteFile();

        Verifier verifier = new Verifier(testDir.toString());
        verifier.deleteArtifacts("org.apache.maven.its.gh2576");

        verifier = new Verifier(new File(testDir, "parent").toString());
        verifier.addCliArguments("install:install-file", "-Dfile=pom.xml", "-DpomFile=pom.xml", "-DlocalRepositoryPath=../repo/");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // use maven 3 personality so that we don't flatten the pom
        verifier = new Verifier(new File(testDir, "dep").toString());
        verifier.addCliArguments("install", "-Dmaven.maven3Personality");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = new Verifier(new File(testDir, "consumer").toString());
        verifier.addCliArguments("install", "-itr");
        assertThrows(VerificationException.class, verifier::execute);
    }
}
