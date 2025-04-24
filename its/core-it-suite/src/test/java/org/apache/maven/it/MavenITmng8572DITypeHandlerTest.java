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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8572">MNG-8572</a>.
 *
 * It verifies that Maven plugins with extensions=true can provide custom artifact type handlers using the Maven API DI system.
 */
public class MavenITmng8572DITypeHandlerTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng8572DITypeHandlerTest() {
        super("[4.0.0-rc-4,)");
    }

    @Test
    public void testCustomTypeHandler() throws Exception {
        // Build the extension first
        File testDir = extractResources("/mng-8572-di-type-handler");
        Verifier verifier = newVerifier(new File(testDir, "extension").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng8572");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Now use the extension in a test project
        verifier = newVerifier(new File(testDir, "test").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that our custom type handler was used
        verifier.verifyTextInLog("[INFO] Using custom type handler for type: custom-type");
    }
}
