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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4872">MNG-4872</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4872ReactorResolutionAttachedWithExclusionsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4872ReactorResolutionAttachedWithExclusionsTest() {
        super("[3.0-beta-1,)");
    }

    /**
     * Test that resolution of (attached) artifacts from the reactor doesn't cause exclusions to be lost.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4872");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("consumer/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4872");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("consumer/target/artifacts.txt");

        assertTrue(artifacts.contains("org.apache.maven.its.mng4872:producer:jar:0.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.mng4872:producer:jar:shaded:0.1"), artifacts.toString());
        assertFalse(artifacts.contains("org.apache.maven.its.mng4872:excluded:jar:0.1"), artifacts.toString());
    }
}
