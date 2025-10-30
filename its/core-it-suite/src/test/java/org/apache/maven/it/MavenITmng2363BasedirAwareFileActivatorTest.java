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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2363">MNG-2363</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2363BasedirAwareFileActivatorTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that the file-based profile activator resolves relative paths against the current project's base directory
     * and also interpolates ${basedir} if explicitly given, just like usual for other parts of the POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Path testDir = extractResources("/mng-2363");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("sub-a/target");
        verifier.deleteDirectory("sub-b/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/parent1.txt");
        verifier.verifyFilePresent("target/parent2.txt");
        verifier.verifyFileNotPresent("target/file1.txt");
        verifier.verifyFileNotPresent("target/file2.txt");

        verifier.verifyFileNotPresent("sub-a/target/parent1.txt");
        verifier.verifyFileNotPresent("sub-a/target/parent2.txt");
        verifier.verifyFilePresent("sub-a/target/file1.txt");
        verifier.verifyFileNotPresent("sub-a/target/file2.txt");

        verifier.verifyFileNotPresent("sub-b/target/parent1.txt");
        verifier.verifyFileNotPresent("sub-b/target/parent2.txt");
        verifier.verifyFileNotPresent("sub-b/target/file1.txt");
        verifier.verifyFilePresent("sub-b/target/file2.txt");
    }
}
