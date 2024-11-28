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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0132PomLifecycleTest extends AbstractMavenIntegrationTestCase {

    public MavenIT0132PomLifecycleTest() {
        super("[2.0.0,)");
    }

    /**
     * Test default binding of goals for "pom" lifecycle.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0132() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/it0132");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.setAutoClean(false);
        verifier.addCliArgument("deploy");
        verifier.execute();
        if (matchesVersionRange("(2.0.1,3.0-alpha-1)")) {
            verifier.verifyFilePresent("target/site-attach-descriptor.txt");
        }
        verifier.verifyFilePresent("target/install-install.txt");
        verifier.verifyFilePresent("target/deploy-deploy.txt");
        verifier.verifyErrorFreeLog();
    }
}
