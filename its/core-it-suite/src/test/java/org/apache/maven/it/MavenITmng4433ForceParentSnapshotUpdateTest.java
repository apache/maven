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
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4433">MNG-4433</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4433ForceParentSnapshotUpdateTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4433ForceParentSnapshotUpdateTest() {
        super("[2.0,3.0-alpha-1),[3.0-alpha-4,)");
    }

    /**
     * Verify that snapshot updates of parent POMs can be forced from the command line via "-U".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4433");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4433");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");

        Map<String, String> filterProps = verifier.newDefaultFilterMap();

        filterProps.put("@repo@", "repo-1");
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
        verifier.setLogFileName("log-force-1.txt");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/old.txt");
        verifier.verifyFileNotPresent("target/new.txt");

        filterProps.put("@repo@", "repo-2");
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
        verifier.setLogFileName("log-force-2.txt");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-U");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent("target/old.txt");
        verifier.verifyFilePresent("target/new.txt");
    }
}
