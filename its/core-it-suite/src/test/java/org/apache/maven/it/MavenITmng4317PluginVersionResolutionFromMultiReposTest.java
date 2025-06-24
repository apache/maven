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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4317">MNG-4317</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4317PluginVersionResolutionFromMultiReposTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4317PluginVersionResolutionFromMultiReposTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that the g:a level metadata files from different repositories are properly merged when trying to resolve
     * a version for some plugin that was invoked with g:a:goal.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4317");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4317");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("org.apache.maven.its.mng4317:maven-mng4317-plugin:touch");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/touch.txt");
    }
}
