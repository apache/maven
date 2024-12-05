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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenITmng5214DontMapWsdlToJar extends AbstractMavenIntegrationTestCase {
    public MavenITmng5214DontMapWsdlToJar() {
        super("[3.1,)");
    }

    /**
     * Test that the code that allows test-jar and ejb-client dependencies to resolve to the
     * target/classes or target/test-class is *not* applies to other types, e.g. wsdl.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitTestPhase() throws Exception {
        File setupDir = extractResources("/mng-5214/dependency");

        Verifier setupVerifier = newVerifier(setupDir.getAbsolutePath());
        setupVerifier.setAutoclean(false);
        setupVerifier.addCliArgument("-X");
        setupVerifier.deleteDirectory("target");
        setupVerifier.deleteArtifacts("org.apache.maven.its.mng5214");
        setupVerifier.setLogFileName("log-setup.txt");
        setupVerifier.addCliArgument("-PcreateWsdl");
        setupVerifier.addCliArgument("generate-resources");
        setupVerifier.execute();

        File testDir = extractResources("/mng-5214");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("consumer/target");
        verifier.deleteDirectory("dependency/target");
        verifier.setLogFileName("log-test.txt");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        List<String> lines = verifier.loadLogLines();
        // RESOLVE-ONE-DEPENDENCY org.apache.maven.its.mng5214:dependency:wsdl:1.0-SNAPSHOT $ /tmp/it
        // .repo/org/apache/maven/its/mng5214/dependency/1.0-SNAPSHOT/dependency-1.0-SNAPSHOT.wsdl
        for (String line : lines) {
            if (line.contains("RESOLVE-ONE-DEPENDENCY org.apache.maven.its.mng5214:dependency:wsdl:1.0-SNAPSHOT")) {
                assertFalse(line.contains("classes-main"));
                assertTrue(line.endsWith(".wsdl"));
            }
        }
    }
}
