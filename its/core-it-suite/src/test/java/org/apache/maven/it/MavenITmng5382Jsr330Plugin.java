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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5382">MNG-5382</a>.
 *
 * @author Jason van Zyl
 */
public class MavenITmng5382Jsr330Plugin extends AbstractMavenIntegrationTestCase {

    private File testDir;

    public MavenITmng5382Jsr330Plugin() {
        super("[3.1-alpha,)");
    }

    @BeforeEach
    public void setUp() throws Exception {
        testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5382");
    }

    @Test
    public void testJsr330PluginExecution() throws Exception {
        //
        // Build a plugin that uses a JSR330 plugin
        //
        Verifier v0 = newVerifier(testDir.getAbsolutePath(), "remote");
        v0.setAutoclean(false);
        v0.deleteDirectory("target");
        v0.deleteArtifacts("org.apache.maven.its.mng5382");
        v0.addCliArgument("install");
        v0.execute();
        v0.verifyErrorFreeLog();

        //
        // Execute the JSR330 plugin
        //
        Verifier v1 = newVerifier(testDir.getAbsolutePath(), "remote");
        v1.setAutoclean(false);
        v1.addCliArgument("org.apache.maven.its.mng5382:jsr330-maven-plugin:0.0.1-SNAPSHOT:hello");
        v1.execute();
        v1.verifyErrorFreeLog();
        v1.verifyTextInLog(
                "Hello! I am a component that is being used via constructor injection! That's right, I'm a JSR330 badass.");
    }
}
