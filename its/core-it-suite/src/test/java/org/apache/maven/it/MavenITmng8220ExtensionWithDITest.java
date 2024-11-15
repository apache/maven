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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8220">MNG-8220</a>.
 */
public class MavenITmng8220ExtensionWithDITest extends AbstractMavenIntegrationTestCase {
    public MavenITmng8220ExtensionWithDITest() {
        super("[4.0.0-beta-4,)");
    }

    /**
     *  Verify that the central url can be overridden by a user property.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitModel() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8220-extension-with-di");

        Verifier verifier = newVerifier(new File(testDir, "extensions").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "test").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[MNG-8220] DumbModelParser1 Called from extension");
        verifier.verifyTextInLog("[MNG-8220] DumbModelParser2 Called from extension");
        verifier.verifyTextInLog("[MNG-8220] DumbModelParser3 Called from extension");
        verifier.verifyTextInLog("[MNG-8220] DumbModelParser4 Called from extension");
    }
}
