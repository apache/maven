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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11074">GH-11074</a>.
 *
 * When a pom.xml contains a {@code <url>${project.url}</url>}, it now fails with Maven 4
 * with an error like:
 * {@code [ERROR] recursive variable reference: project.url}
 *
 * This was not failing in Maven 3, but should fail in Maven 4 to prevent infinite loops
 * during variable resolution.
 */
public class MavenITgh11074RecursiveProjectUrlTest extends AbstractMavenIntegrationTestCase {

    public MavenITgh11074RecursiveProjectUrlTest() {
        super("(,)"); // Test for all Maven versions to see the behavior difference
    }

    /**
     * Test that recursive project.url reference fails in Maven 4+ but was tolerated in Maven 3.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRecursiveProjectUrl() throws Exception {
        File testDir = extractResources("/gh-11074-recursive-project-url");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");

        if (matchesVersionRange("[4.0.0-alpha-1,)")) {
            // Maven 4+ should fail with recursive variable reference error
            assertThrows(
                    Exception.class,
                    () -> {
                        verifier.execute();
                        verifier.verifyErrorFreeLog();
                    },
                    "Maven 4+ should fail when project.url contains recursive reference");

            // Verify the specific error message is present
            verifier.verifyTextInLog("recursive variable reference: project.url");
        } else {
            // Maven 3 should not fail (though it may produce warnings)
            verifier.execute();
            verifier.verifyErrorFreeLog();
        }
    }

    /**
     * Test that recursive project.url reference in dependency POM produces warning in Maven 4+.
     * The build succeeds but Maven 4+ correctly identifies and warns about the invalid POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRecursiveProjectUrlInDependencyPom() throws Exception {
        File testDir = extractResources("/gh-11074-recursive-project-url-dependency");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.gh11074");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");

        if (matchesVersionRange("[4.0.0-alpha-1,)")) {
            // Maven 4+ should succeed but show warning about invalid dependency POM
            verifier.execute();
            verifier.verifyErrorFreeLog();

            // Verify the specific warning message is present
            verifier.verifyTextInLog("recursive variable reference: project.url");
            verifier.verifyTextInLog("The POM for org.apache.maven.its.gh11074:bad-dependency:jar:1.0 is invalid");
        } else {
            // Maven 3 should not fail (though it may produce warnings)
            verifier.execute();
            verifier.verifyErrorFreeLog();
        }
    }
}
