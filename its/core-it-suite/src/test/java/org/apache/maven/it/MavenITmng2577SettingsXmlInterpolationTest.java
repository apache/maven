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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2577">MNG-2577</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2577SettingsXmlInterpolationTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2577SettingsXmlInterpolationTest() {
        super("[2.0.3,)");
    }

    /**
     * Verify that the settings.xml can be interpolated using environment variables.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitEnvVars() throws Exception {
        File testDir = extractResources("/mng-2577");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings-env.xml");
        verifier.setEnvironmentVariable("MNGIT", "env-var-test");
        verifier.setLogFileName("log-env.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/settings.properties");
        assertEquals("env-var-test", props.getProperty("settings.servers.0.username"));
    }

    /**
     * Verify that the settings.xml can be interpolated using user properties.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitSystemProps() throws Exception {
        requiresMavenVersion("[3.0-alpha-1,)");

        File testDir = extractResources("/mng-2577");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings-sys.xml");
        verifier.addCliArgument("-Dusr.MNGIT=usr-prop-test");
        verifier.setLogFileName("log-sys.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/settings.properties");
        assertEquals("usr-prop-test", props.getProperty("settings.servers.0.username"));
        assertEquals(File.separator, props.getProperty("settings.servers.0.password"));
    }
}
