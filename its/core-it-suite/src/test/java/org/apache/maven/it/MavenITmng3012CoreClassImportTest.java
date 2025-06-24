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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3012">MNG-3012</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3012CoreClassImportTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3012CoreClassImportTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that classes shared with the Maven core realm are imported into the plugin realm such that instances of
     * these classes created by the core can be cast to classes loaded by the plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3012() throws Exception {
        File testDir = extractResources("/mng-3012");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifact("org.codehaus.plexus", "plexus-utils", "0.1-mng3012", "jar");
        verifier.deleteArtifact("org.codehaus.plexus", "plexus-utils", "0.1-mng3012", "pom");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties xpp3domProps = verifier.loadProperties("target/xpp3dom.properties");
        assertEquals("true", xpp3domProps.getProperty("project.reporting.plugins.0.configuration"));
    }
}
