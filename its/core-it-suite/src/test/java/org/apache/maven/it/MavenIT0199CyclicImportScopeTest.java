package org.apache.maven.it;

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

import java.io.File;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

public class MavenIT0199CyclicImportScopeTest extends AbstractMavenIntegrationTestCase {

    public MavenIT0199CyclicImportScopeTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    public void testit0199() throws Exception {
        // v1: parent not using BOM; explicit dep from componentB → componentA
        // v2: BOM introduced; componentB → componentA picks up implicit version 1 from main@v1
        // v3: components now inheriting indirectly from an older version of the BOM that includes them; componentB → componentA version overridden
        for (int i = 1; i <= 3; i++) {
            build("v" + i + "/parent", null);
            build("v" + i + "/componentA", "target/componentA-" + i + ".jar");
            build("v" + i + "/componentB", "target/componentB-" + i + ".jar");
            build("v" + i + "/main", "bundle/target/bundle-" + i + ".jar");
        }
    }

    private void build(String module, String expectedArtifact) throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/cyclic-import-scope");
        Verifier verifier = newVerifier(new File(testDir.getAbsolutePath(), module).getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.executeGoal("install");
        if (expectedArtifact != null) {
            verifier.verifyFilePresent(expectedArtifact);
        }
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
