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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-848">MNG-848</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0848UserPropertyOverridesDefaultValueTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng0848UserPropertyOverridesDefaultValueTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that execution/system properties take precedence over default value of plugin parameters.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG848() throws Exception {
        File testDir = extractResources("/mng-0848");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dconfig.aliasDefaultExpressionParam=PASSED");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties configProps = verifier.loadProperties("target/config.properties");
        assertEquals("maven-core-it", configProps.getProperty("defaultParam"));
        assertEquals("PASSED", configProps.getProperty("aliasDefaultExpressionParam"));
    }
}
