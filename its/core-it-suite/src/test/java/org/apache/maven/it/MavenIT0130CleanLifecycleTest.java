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
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0130CleanLifecycleTest extends AbstractMavenIntegrationTestCase {

    public MavenIT0130CleanLifecycleTest() {
        super("[2.0.0,)");
    }

    /**
     * Test default binding of goals for "clean" lifecycle.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0130() throws Exception {
        File testDir = extractResources("/it0130");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.setAutoclean(false);
        verifier.addCliArgument("clean");
        verifier.execute();
        verifier.verifyFilePresent("target/clean-clean.txt");
        verifier.verifyErrorFreeLog();
    }
}
