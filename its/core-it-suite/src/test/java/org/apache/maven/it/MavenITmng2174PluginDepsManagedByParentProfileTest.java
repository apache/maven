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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2174">MNG-2174</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2174PluginDepsManagedByParentProfileTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2174PluginDepsManagedByParentProfileTest() {
        super("[2.0.9,3.0-alpha-1),[3.0-alpha-3,)");
    }

    /**
     * Verify that plugin dependencies defined by plugin management of a parent profile are not lost when the
     * parent's main plugin management section is also present.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2174() throws Exception {
        File testDir = extractResources("/mng-2174");

        Verifier verifier = newVerifier(new File(testDir, "sub").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng2174");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pcl.properties");
        assertEquals("1", props.getProperty("mng-2174.properties.count"));
        assertNotNull(props.getProperty("mng-2174.properties"));
    }
}
