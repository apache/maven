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

class MavenITmng5527BomRelocationTest extends AbstractMavenIntegrationTestCase {

    @Test
    void followsRelocationChainAndPreservesImportExclusions() throws Exception {
        Path testDir = extractResources("mng-5527-bom-relocation/project");
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.filterFile("../settings-template.xml", "settings.xml", verifier.newDefaultFilterMap());
        verifier.addCliArguments("-s", "settings.xml", "verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("has been relocated");

        Properties properties = verifier.loadProperties("target/project.properties");
        assertEquals("1", properties.getProperty("project.dependencyManagement.dependencies"));
        assertEquals(
                "org.apache.commons:commons-lang3:jar",
                properties.getProperty("project.dependencyManagement.dependencies.0.managementKey"));
        assertEquals("3.18.0", properties.getProperty("project.dependencyManagement.dependencies.0.version"));
        assertEquals("1", properties.getProperty("project.dependencyManagement.dependencies.0.exclusions"));
        assertEquals(
                "commons-io", properties.getProperty("project.dependencyManagement.dependencies.0.exclusions.0.groupId"));
        assertEquals(
                "commons-io", properties.getProperty("project.dependencyManagement.dependencies.0.exclusions.0.artifactId"));
        assertEquals("3.18.0", properties.getProperty("project.dependencies.0.version"));
    }
}
