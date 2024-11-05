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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3122">MNG-3122</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3122ActiveProfilesNoDuplicatesTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3122ActiveProfilesNoDuplicatesTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Verify that MavenProject.getActiveProfiles() reports profiles from the settings.xml with activeByDefault=true
     * only once.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3122() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3122");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/profile.properties");
        int count = 0;
        int n = Integer.parseInt(props.getProperty("project.activeProfiles", "0"));
        for (int i = 0; i < n; i++) {
            if ("mng3122".equals(props.getProperty("project.activeProfiles." + i + ".id"))) {
                count++;
            }
        }
        assertEquals(1, count);
    }
}
