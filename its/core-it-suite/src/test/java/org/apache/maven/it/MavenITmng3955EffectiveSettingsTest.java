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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3955">MNG-3955</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3955EffectiveSettingsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3955EffectiveSettingsTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that plugin parameter expressions referring to the settings reflect the actual core state, especially
     * if settings have been overridden by CLI parameters.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3955() throws Exception {
        File testDir = extractResources("/mng-3955");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        String localRepo = verifier.getLocalRepository();
        verifier.setAutoclean(false);
        verifier.addCliArgument("-Dmaven.repo.local.tail=" + localRepo);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("--offline");
        verifier.addCliArgument("--batch-mode");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/settings.properties");
        assertEquals("true", props.getProperty("settings.offline"));
        assertEquals("false", props.getProperty("settings.interactiveMode"));
        assertEquals(
                new File(verifier.getLocalRepositoryWithSettings("settings.xml")).getAbsoluteFile(),
                new File(props.getProperty("settings.localRepository")).getAbsoluteFile());
    }
}
