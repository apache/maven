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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4788">MNG-4788</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4788InstallationToCustomLocalRepoTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that plugins can install artifacts to a custom local repo (i.e. custom base dir and custom layout).
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Path testDir = extractResources("/mng-4788");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/local-repo/test-0.1-SNAPSHOT.jar");
        verifier.verifyFileNotPresent(
                "target/local-repo/org/apache/maven/its/mng4788/test/0.1-SNAPSHOT/test-0.1-SNAPSHOT.jar");
    }
}
