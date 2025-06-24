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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4459">MNG-4459</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4459InMemorySettingsKeptEncryptedTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4459InMemorySettingsKeptEncryptedTest() {
        super("[2.1.0,3.0-alpha-1),[3.0-alpha-5,4.0.0-beta-6)");
    }

    /**
     * Verify that encrypted passwords in the settings stay encrypted in the settings model visible to
     * plugins. In other words, the passwords should only be decrypted at the transport layer.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4459");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.getSystemProperties()
                .setProperty("settings.security", new File(testDir, "settings-security.xml").getAbsolutePath());
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/settings.properties");
        assertEquals(
                "{BteqUEnqHecHM7MZfnj9FwLcYbdInWxou1C929Txa0A=}", props.getProperty("settings.servers.0.password", ""));
    }
}
