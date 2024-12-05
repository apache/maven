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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3052">MNG-3052</a>.
 *
 * When a project dependency declares its own repositories, they should be used to
 * resolve that dependency's dependencies. This includes both steps: determining
 * the dependency artifact information (version, etc.) AND resolving the actual
 * artifact itself.
 *
 * NOTE: The SNAPSHOT versions are CRITICAL in this test, since they force the
 * correct resolution of artifact metadata, which depends on having the correct
 * set of repositories present.
 *
 * @author jdcasey
 */
public class MavenITmng3052DepRepoAggregationTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3052DepRepoAggregationTest() {
        super("(2.0.9,)"); // only test in 2.0.10+
    }

    @Test
    public void testitMNG3052() throws Exception {
        File testDir = extractResources("/mng-3052").getCanonicalFile();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3052");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.filterFile(
                "repo-d/org/apache/maven/its/mng3052/direct/0.1-SNAPSHOT/template.pom",
                "repo-d/org/apache/maven/its/mng3052/direct/0.1-SNAPSHOT/direct-0.1-20090517.133956-1.pom");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng3052", "direct", "0.1-SNAPSHOT", "jar");
        verifier.verifyArtifactPresent("org.apache.maven.its.mng3052", "trans", "0.1-SNAPSHOT", "jar");
    }
}
