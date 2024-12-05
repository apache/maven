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
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MavenITmng7160ExtensionClassloader extends AbstractMavenIntegrationTestCase {
    public MavenITmng7160ExtensionClassloader() {
        super("[3.9.0,)");
    }

    @Test
    public void testVerify() throws IOException, VerificationException {
        final File projectDir = extractResources("/mng-7160-extensionclassloader");

        final Verifier extensionVerifier = newVerifier(new File(projectDir, "extension").getAbsolutePath());
        extensionVerifier.addCliArgument("install");
        extensionVerifier.execute();
        extensionVerifier.verifyErrorFreeLog();

        final Verifier verifier1 = newVerifier(new File(projectDir, "project-build").getAbsolutePath());
        verifier1.addCliArgument("install");
        verifier1.execute();
        verifier1.verifyErrorFreeLog();
        verifier1.verifyTextInLog("xpp3 -> mvn");
        verifier1.verifyTextInLog("base64 -> ext");

        final Verifier verifier2 = newVerifier(new File(projectDir, "project-core-parent-first").getAbsolutePath());
        verifier2.addCliArgument("install");
        verifier2.execute();
        verifier2.verifyErrorFreeLog();
        verifier2.verifyTextInLog("xpp3 -> mvn");
        verifier2.verifyTextInLog("base64 -> mvn");

        final Verifier verifier3 = newVerifier(new File(projectDir, "project-core-plugin").getAbsolutePath());
        verifier3.addCliArgument("verify");
        verifier3.execute();
        verifier3.verifyErrorFreeLog();
        verifier3.verifyTextInLog("xpp3 -> mvn");
        verifier3.verifyTextInLog("base64 -> ext");

        final Verifier verifier4 = newVerifier(new File(projectDir, "project-core-self-first").getAbsolutePath());
        verifier4.addCliArgument("verify");
        verifier4.execute();
        verifier4.verifyErrorFreeLog();
        verifier4.verifyTextInLog("xpp3 -> ext");
        verifier4.verifyTextInLog("base64 -> ext");
    }
}
