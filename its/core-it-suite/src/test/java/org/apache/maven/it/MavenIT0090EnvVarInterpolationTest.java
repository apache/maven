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

public class MavenIT0090EnvVarInterpolationTest extends AbstractMavenIntegrationTestCase {
    public MavenIT0090EnvVarInterpolationTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that ensures that envars are interpolated correctly into plugin
     * configurations.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0090() throws Exception {
        File testDir = extractResources("/it0090");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setEnvironmentVariable("MAVEN_TEST_ENVAR", "MAVEN_TEST_ENVAR_VALUE");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/env.properties");
        assertEquals("MAVEN_TEST_ENVAR_VALUE", props.getProperty("stringParam"));
    }
}
