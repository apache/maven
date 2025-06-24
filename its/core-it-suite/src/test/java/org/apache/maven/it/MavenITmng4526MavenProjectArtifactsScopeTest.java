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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4526">MNG-4526</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4526MavenProjectArtifactsScopeTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4526MavenProjectArtifactsScopeTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-alpha-7,)");
    }

    /**
     * Test that MavenProject.getArtifacts() only holds artifacts matching the scope requested by a mojo. This
     * must also be the case when previously already artifacts from a wider scope were resolved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4526");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4526");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("generate-sources");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts;

        artifacts = verifier.loadLines("target/compile.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng4526:a:jar:0.1"), artifacts.toString());
        assertFalse(artifacts.contains("org.apache.maven.its.mng4526:b:jar:0.1"), artifacts.toString());
        assertFalse(artifacts.contains("org.apache.maven.its.mng4526:c:jar:0.1"), artifacts.toString());

        artifacts = verifier.loadLines("target/runtime.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng4526:a:jar:0.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng4526:b:jar:0.1"), artifacts.toString());
        assertFalse(artifacts.contains("org.apache.maven.its.mng4526:c:jar:0.1"), artifacts.toString());

        artifacts = verifier.loadLines("target/test.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng4526:a:jar:0.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng4526:b:jar:0.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng4526:c:jar:0.1"), artifacts.toString());
    }
}
