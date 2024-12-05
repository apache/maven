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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-377">MNG-377</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0377PluginLookupFromPrefixTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng0377PluginLookupFromPrefixTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test usage of plugins.xml mapping file on the repository to resolve plugin artifactId from its prefix using the
     * pluginGroups in the provided settings.xml.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG377() throws Exception {
        File testDir = extractResources("/mng-0377");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng0377");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("-Dtouch.outputFile=target/file.txt");
        verifier.addCliArgument("itprefix:touch");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/file.txt");
    }
}
