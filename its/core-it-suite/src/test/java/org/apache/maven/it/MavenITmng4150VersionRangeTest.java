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
import java.util.Collection;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-505">MNG-505</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng4150VersionRangeTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng4150VersionRangeTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test version range support.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4150() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4150");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4150");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Collection<String> artifacts = verifier.loadLines("target/artifacts.txt", "UTF-8");
        assertEquals(4, artifacts.size());
        assertTrue(artifacts.toString(), artifacts.contains("org.apache.maven.its.mng4150:a:jar:1.1"));
        assertTrue(artifacts.toString(), artifacts.contains("org.apache.maven.its.mng4150:b:jar:1.0"));
        assertTrue(artifacts.toString(), artifacts.contains("org.apache.maven.its.mng4150:c:jar:3.8"));
        assertTrue(artifacts.toString(), artifacts.contains("org.apache.maven.its.mng4150:d:jar:2.1.1"));
    }
}
