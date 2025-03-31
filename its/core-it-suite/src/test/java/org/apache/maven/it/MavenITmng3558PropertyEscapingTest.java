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
 * This is a test case for <a href="https://issues.apache.org/jira/browse/MNG-3558">MNG-3558</a>.
 *
 * Verifies that property references can be properly escaped in both model properties
 * and plugin configuration using backslash.
 */
class MavenITmng3558PropertyEscapingTest extends AbstractMavenIntegrationTestCase {
    MavenITmng3558PropertyEscapingTest() {
        super("[4.0.0-beta-5,)");
    }

    @Test
    public void testPropertyEscaping() throws Exception {
        File testDir = extractResources("/mng-3558-property-escaping");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify model properties
        Properties modelProps = verifier.loadProperties("target/property-values.properties");
        assertEquals("${test.value}", modelProps.getProperty("project.properties.escaped.property"));
        assertEquals("prefix-${test.value}-suffix", modelProps.getProperty("project.properties.escaped.with.context"));
        assertEquals("interpolated-value", modelProps.getProperty("project.properties.normal.property"));

        // Verify plugin configuration
        Properties configProps = verifier.loadProperties("target/config-values.properties");
        assertEquals("${test.value}", configProps.getProperty("param"));
        assertEquals("prefix-${test.value}-suffix", configProps.getProperty("paramWithContext"));
        assertEquals("interpolated-value", configProps.getProperty("normalParam"));
    }
}
