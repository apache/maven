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
import java.util.List;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

import static org.apache.maven.it.Verifier.verifyTextInLog;
import static org.apache.maven.it.Verifier.verifyTextNotInLog;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for
 * <a href="https://issues.apache.org/jira/browse/MNG-7967">MNG-7967</a>.
 * Allow to exclude plugins from validation. Affected is Maven 4.0.0-alpha-9.
 */
class MavenITmng7967ArtifactHandlerLanguageTest extends AbstractMavenIntegrationTestCase {

    protected MavenITmng7967ArtifactHandlerLanguageTest() {
        super("(,4.0.0-alpha-9),(4.0.0-alpha-9,)");
    }

    @Test
    void javadocIsExecutedAndFailed() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7967-artifact-handler-language");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.addCliArgument("org.apache.maven.plugins:maven-javadoc-plugin:3.6.3:jar");

        boolean invocationFailed = false;
        // whatever outcome we do not fail here, but later
        try {
            verifier.execute();
        } catch (VerificationException e) {
            invocationFailed = true;
        }

        List<String> logs = verifier.loadLogLines();

        // javadoc:jar uses language to detect is this "Java classpath-capable package"
        verifyTextNotInLog(logs, "[INFO] Not executing Javadoc as the project is not a Java classpath-capable package");

        // javadoc invocation should actually fail the build
        verifyTextInLog(logs, "[INFO] BUILD FAILURE");

        // javadoc invocation should actually fail the build
        verifyTextInLog(logs, "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-javadoc-plugin");

        assertTrue(invocationFailed, "The Maven invocation should have failed: the javadoc should error out");
    }
}
