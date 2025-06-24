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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3900">MNG-3900</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3900ProfilePropertiesInterpolationTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3900ProfilePropertiesInterpolationTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that build properties defined via active profiles are used for interpolation.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3900() throws Exception {
        File testDir = extractResources("/mng-3900");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Pinterpolation-profile");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("PASSED", props.getProperty("project.properties.test"));
        assertEquals("PASSED", props.getProperty("project.properties.property"));
        assertEquals("http://maven.apache.org/PASSED", props.getProperty("project.url"));
    }
}
