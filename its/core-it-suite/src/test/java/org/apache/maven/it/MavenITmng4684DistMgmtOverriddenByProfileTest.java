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
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4684">MNG-4684</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4684DistMgmtOverriddenByProfileTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that active profiles can override distribution management settings.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-4684");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Pmng4684");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");

        assertEquals("pr", props.getProperty("project.distributionManagement.repository.id"));
        assertEquals("http://localhost/r", props.getProperty("project.distributionManagement.repository.url"));
        assertEquals("", props.getProperty("project.distributionManagement.repository.name", ""));
        assertEquals("default", props.getProperty("project.distributionManagement.repository.layout"));
        assertEquals("true", props.getProperty("project.distributionManagement.repository.uniqueVersion"));

        assertEquals("psr", props.getProperty("project.distributionManagement.snapshotRepository.id"));
        assertEquals("http://localhost/sr", props.getProperty("project.distributionManagement.snapshotRepository.url"));
        assertEquals("", props.getProperty("project.distributionManagement.snapshotRepository.name", ""));
        assertEquals("default", props.getProperty("project.distributionManagement.snapshotRepository.layout"));
        assertEquals("true", props.getProperty("project.distributionManagement.snapshotRepository.uniqueVersion"));

        assertEquals("ps", props.getProperty("project.distributionManagement.site.id"));
        assertEquals("http://localhost/s", props.getProperty("project.distributionManagement.site.url"));
        assertEquals("passed", props.getProperty("project.distributionManagement.site.name"));
    }
}
