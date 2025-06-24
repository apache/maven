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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4779">MNG-4779</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4779MultipleDepsWithVersionRangeFromLocalRepoTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4779MultipleDepsWithVersionRangeFromLocalRepoTest() {
        super("[2.0.3,)");
    }

    /**
     * Test that dependency resolution doesn't error out when a dependency with a range satisfied from the local repo
     * is seen more than once during the collection.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4779");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("test/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4779");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("test/target/classpath.txt");

        assertEquals(4, classpath.size(), classpath.toString());
    }
}
