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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2123">MNG-2123</a>.
 */
public class MavenITmng2123VersionRangeDependencyTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2123VersionRangeDependencyTest() {
        super("(2.0.8,)");
    }

    @Test
    public void testitMNG2123() throws Exception {
        File testDir = extractResources("/mng-2123");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng2123", "maven-core-it");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("target/artifacts.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng2123:fixed:jar:0.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng2123:common:jar:3.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng2123:range:jar:0.1"), artifacts.toString());
    }
}
