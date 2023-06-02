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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5006">MNG-5006</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng5006VersionRangeDependencyParentResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5006VersionRangeDependencyParentResolutionTest() {
        super("[2.0.3,3.0-alpha-1),[3.0.3,)");
    }

    /**
     * Verify that resolution of parent POMs of dependencies that use a version range is not restricted to the
     * repository from which the specific dependency version was picked. Or put differently, the fact that a:0.1
     * was found in repo-1 does not mean parents/dependencies of a:0.1 are also located in that same repo, they
     * could be in any of the originally declared repos.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5006");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng5006");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines("target/compile.txt", "UTF-8");

        assertTrue(compile.toString(), compile.contains("a-0.1.jar"));
        assertTrue(compile.toString(), compile.contains("b-0.1.jar"));
    }
}
