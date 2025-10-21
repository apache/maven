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

public class MavenITmng5805PkgTypeMojoConfiguration2 extends AbstractMavenIntegrationTestCase {

    @Test
    public void testPkgTypeMojoConfiguration() throws Exception {
        File testDir = extractResources("/mng-5805-pkg-type-mojo-configuration2");

        // First, build the test plugin dependency
        Verifier verifier = newVerifier(new File(testDir, "mng5805-plugin-dep").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Then, build the test extension2
        verifier = newVerifier(new File(testDir, "mng5805-extension2").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Then, build the test plugin
        verifier = newVerifier(new File(testDir, "mng5805-plugin").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Finally, run the test project
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("CLASS_NAME=org.apache.maven.its.mng5805.TestClass1");
    }
}
