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

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4921">MNG-4921</a>.
 *
 * Verifies that the {@code <qualityManagement>} element (POM model 4.2.0) is read from the POM,
 * inherited from the parent and can be overridden by a child.
 */
class MavenITmng4921QualityManagementTest extends AbstractMavenIntegrationTestCase {

    MavenITmng4921QualityManagementTest() {}

    /**
     * Verify that the quality management information is read from the effective model, inherited from the parent POM
     * unless overridden by the child.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitQualityManagement() throws Exception {
        Path testDir = extractResources("mng-4921");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("inherit/target");
        verifier.deleteDirectory("override/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("SonarQube", props.getProperty("project.qualityManagement.system"));
        assertEquals("https://sonar.parent.example.org", props.getProperty("project.qualityManagement.url"));

        // child without own qualityManagement inherits it from the parent
        props = verifier.loadProperties("inherit/target/pom.properties");
        assertEquals("SonarQube", props.getProperty("project.qualityManagement.system"));
        assertEquals("https://sonar.parent.example.org", props.getProperty("project.qualityManagement.url"));

        // child declaring its own qualityManagement overrides the parent's
        props = verifier.loadProperties("override/target/pom.properties");
        assertEquals("Coverity", props.getProperty("project.qualityManagement.system"));
        assertEquals("https://scan.child.example.org", props.getProperty("project.qualityManagement.url"));
    }
}
