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
 * Integration test for <a href="https://github.com/apache/maven/issues/13911">GH-13911</a>.
 *
 * <p>Verifies that an annotation processor (AP) built from source in the same Maven reactor
 * can be used by a sibling module during its {@code compile} phase.  The POM {@code provided}
 * dependency on the AP module gives Maven the ordering information it needs; no special
 * reactor-plugin detection (MNG-1088 class) is required.</p>
 */
class MavenITgh13911ReactorAnnotationProcessorTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that the sequential builder correctly orders the AP module before the client
     * module and that the annotation processor generates its output file.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testReactorAnnotationProcessor() throws Exception {
        Path testDir = extractResources("/gh-13911-reactor-ap");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("client/target");
        verifier.deleteArtifacts("org.apache.maven.its.gh13911");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // The annotation processor must have run and written its marker resource
        verifier.verifyFilePresent("client/target/classes/META-INF/generated-by-gh13911.txt");
    }
}
