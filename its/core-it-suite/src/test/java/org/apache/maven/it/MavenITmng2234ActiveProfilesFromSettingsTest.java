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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2234">MNG-2234</a>.
 */
public class MavenITmng2234ActiveProfilesFromSettingsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2234ActiveProfilesFromSettingsTest() {
        super("(2.0.8,)");
    }

    /**
     * Verify that the activeProfile section from the settings.xml can also activate profiles specified in the POM,
     * i.e. outside of the settings.xml.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2234() throws Exception {
        File testDir = extractResources("/mng-2234");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");

        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/touch.txt");
    }
}
