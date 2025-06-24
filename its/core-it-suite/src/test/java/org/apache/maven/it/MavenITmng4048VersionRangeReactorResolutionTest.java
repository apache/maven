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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4048">MNG-4048</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4048VersionRangeReactorResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4048VersionRangeReactorResolutionTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Verify that dependencies using version ranges can be resolved from the reactor.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4048");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("sub-2/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4048");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("sub-2/target/compile.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng4048:sub-1:jar:1.1-SNAPSHOT"), artifacts.toString());
    }
}
