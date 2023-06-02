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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0138PluginLifecycleTest extends AbstractMavenIntegrationTestCase {

    public MavenIT0138PluginLifecycleTest() {
        super("[2.0.0,)");
    }

    /**
     * Test default binding of goals for "maven-plugin" lifecycle.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0138() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/it0138");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.setAutoclean(false);
        verifier.addCliArgument("deploy");
        verifier.execute();
        verifier.verifyFilePresent("target/plugin-descriptor.txt");
        verifier.verifyFilePresent("target/resources-resources.txt");
        verifier.verifyFilePresent("target/compiler-compile.txt");
        verifier.verifyFilePresent("target/resources-test-resources.txt");
        verifier.verifyFilePresent("target/compiler-test-compile.txt");
        verifier.verifyFilePresent("target/surefire-test.txt");
        verifier.verifyFilePresent("target/jar-jar.txt");
        verifier.verifyFilePresent("target/plugin-add-plugin-artifact-metadata.txt");
        verifier.verifyFilePresent("target/install-install.txt");
        if (matchesVersionRange("(,2.2.0)")) {
            verifier.verifyFilePresent("target/plugin-update-registry.txt");
        }
        verifier.verifyFilePresent("target/deploy-deploy.txt");
        verifier.verifyErrorFreeLog();
    }
}
