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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-505">MNG-505</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0505VersionRangeTest extends AbstractMavenIntegrationTestCase {
    /**
     * Oleg 2009.04.30: the same functionality but simpler - no multiple ranges - syntax
     * is tested in MNG-4150
     */
    public MavenITmng0505VersionRangeTest() {
        super("(,3.0-alpha-1),[3.0-alpha-3,)");
    }

    /**
     * Test version range support.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG505() throws Exception {
        File testDir = extractResources("/mng-0505");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng0505", "maven-core-it");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Collection<String> artifacts = verifier.loadLines("target/artifacts.txt");
        assertEquals(4, artifacts.size());
        assertTrue(artifacts.contains("org.apache.maven.its.mng0505:a:jar:1.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng0505:b:jar:1.0"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng0505:c:jar:3.8"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng0505:d:jar:2.1.1"), artifacts.toString());
    }
}
