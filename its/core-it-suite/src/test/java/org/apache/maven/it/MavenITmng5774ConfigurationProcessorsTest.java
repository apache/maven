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
import java.util.Properties;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

public class MavenITmng5774ConfigurationProcessorsTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng5774ConfigurationProcessorsTest() {
        super("(3.2.5,)");
    }

    @Test
    public void testBehaviourWhereThereIsOneUserSuppliedConfigurationProcessor() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5774-configuration-processors");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");

        verifier = newVerifier(new File(testDir, "build-with-one-processor-valid").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-configuration-processors");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(new File(testDir, "settings.xml").getAbsolutePath());
        verifier.addCliArgument("process-resources");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        // Making sure our configuration processor executes
        verifier.verifyTextInLog("[INFO] ConfigurationProcessorOne.process()");
        // We have a property value injected by our configuration processor. Make sure it's correct
        verifier.verifyFilePresent("target/classes/result.properties");
        Properties result = verifier.loadProperties("target/classes/result.properties");
        assertEquals("yes", result.getProperty("configurationProcessorContributedValue"));
    }

    @Test
    public void testBehaviourWhereThereAreTwoUserSuppliedConfigurationProcessor() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5774-configuration-processors");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");

        verifier = newVerifier(new File(testDir, "build-with-two-processors-invalid").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-configuration-processors");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(new File(testDir, "settings.xml").getAbsolutePath());
        try {
            verifier.addCliArgument("process-resources");
            verifier.execute();
            fail(
                    "We expected this invocation to fail because of too many user supplied configuration processors being present");
        } catch (VerificationException e) {
            verifier.verifyTextInLog("There can only be one user supplied ConfigurationProcessor");
        }
    }
}
