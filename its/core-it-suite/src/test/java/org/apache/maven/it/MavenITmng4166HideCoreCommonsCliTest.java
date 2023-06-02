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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4166">MNG-4166</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4166HideCoreCommonsCliTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4166HideCoreCommonsCliTest() {
        super("[2.2.0,)");
    }

    /**
     * Verify that plugins can use their own version of commons-cli and are not bound to the version bundled in the
     * core.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4166() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4166");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifact("commons-cli", "commons-cli", "0.1.4166", "jar");
        verifier.deleteArtifact("commons-cli", "commons-cli", "0.1.4166", "pom");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/class.properties");
        assertNotNull(props.getProperty("org.apache.maven.its.mng4166.CoreIt"));
    }
}
