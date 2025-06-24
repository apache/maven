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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5639">MNG-5639</a>:
 * Check that import POM defined in DependencyManagement can be resolved from a parameterised repository
 */
public class MavenITmng5639ImportScopePomResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5639ImportScopePomResolutionTest() {
        super("[3.2.2,)");
    }

    @Test
    public void testitMNG5639() throws Exception {
        File testDir = extractResources("/mng-5639-import-scope-pom-resolution");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.mng5639");

        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");

        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng5639", "b", "0.1", "jar");
    }
}
