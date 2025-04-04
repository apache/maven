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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8477">MNG-8477</a>.
 */
class MavenITmng8477MultithreadedFileActivationTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8477MultithreadedFileActivationTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify project is buildable.
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/mng-8477").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArguments("help:active-profiles", "-Dmaven.modelBuilder.parallelism=1");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("- xxx (source: test:m2:jar:1)");
    }
}
