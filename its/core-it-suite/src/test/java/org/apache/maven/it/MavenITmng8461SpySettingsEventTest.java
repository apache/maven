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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8461">MNG-8461</a>.
 */
class MavenITmng8461SpySettingsEventTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8461SpySettingsEventTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify that settings building event is emitted.
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/mng-8461").getAbsoluteFile().toPath();
        Verifier verifier;

        Path extension = basedir.resolve("extension");
        verifier = newVerifier(extension.toString());
        verifier.setAutoclean(false);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path project = basedir.resolve("project");
        verifier = newVerifier(project.toString());
        verifier.setAutoclean(false);
        verifier.setForkJvm(true);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("Initializing Simple Event Spy");
        verifier.verifyTextInLog("SettingsBuilderResult event is present");
    }
}
