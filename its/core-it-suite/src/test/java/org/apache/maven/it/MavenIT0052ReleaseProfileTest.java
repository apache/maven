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

public class MavenIT0052ReleaseProfileTest extends AbstractMavenIntegrationTestCase {
    public MavenIT0052ReleaseProfileTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that source attachment doesn't take place when
     * -DperformRelease=true is missing.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0052() throws Exception {
        File testDir = extractResources("/it0052");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/jar-jar.txt");
        verifier.verifyFileNotPresent("target/source-jar.txt");
        verifier.verifyFileNotPresent("target/javadoc-jar.txt");
    }
}
