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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2605">MNG-2605</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2605BogusProfileActivationTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2605BogusProfileActivationTest() {
        super("(2.0.10,2.1.0-M1),(2.1.0-M1,3.0-alpha-1),(3.0-alpha-1,)");
    }

    /**
     * Test that profiles are not accidentally activated when they have no activation element at all and
     * the user did not request their activation via id.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2605() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-2605");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/profile.properties");
        assertNull(props.getProperty("project.properties.pomProperty"));
        assertNull(props.getProperty("project.properties.settingsProperty"));
        assertNull(props.getProperty("project.properties.profilesProperty"));
    }
}
