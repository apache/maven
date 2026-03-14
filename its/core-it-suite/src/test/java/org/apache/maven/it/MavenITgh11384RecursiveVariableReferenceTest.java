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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11384">GH-11384</a>.
 *
 * Verifies that ${project.url} can refer to a property named "project.url" without causing
 * a recursive variable reference error. This pattern is used by slack-sdk-parent.
 *
 * @since 4.0.0-rc-4
 */
class MavenITgh11384RecursiveVariableReferenceTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that ${project.url} in the url field can reference a property named project.url
     * without causing a recursive variable reference error.
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/gh-11384").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("help:effective-pom");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that the URL was correctly interpolated from the property
        verifier.verifyTextInLog("<url>https://github.com/slackapi/java-slack-sdk</url>");
    }
}

