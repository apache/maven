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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2222">MNG-2222</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2222OutputDirectoryReactorResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2222OutputDirectoryReactorResolutionTest() {
        super("[3.0-beta-1,)");
    }

    /**
     * Test that dependencies on reactor projects can be satisfied by their output directories even if those do not
     * exist (e.g. due to non-existing sources). This ensures consistent build results for "mvn compile" and
     * "mvn package".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-2222");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("mod-a/target");
        verifier.deleteDirectory("mod-b/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng2222");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("mod-b/target/compile.txt");
        assertTrue(classpath.contains("mod-a/target/classes"), classpath.toString());
    }
}
