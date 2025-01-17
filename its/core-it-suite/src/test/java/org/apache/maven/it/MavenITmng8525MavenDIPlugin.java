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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8525">MNG-8525</a>.
 *
 */
public class MavenITmng8525MavenDIPlugin extends AbstractMavenIntegrationTestCase {

    private File testDir;

    public MavenITmng8525MavenDIPlugin() {
        super("[4.0.0-rc-2,)");
    }

    @BeforeEach
    public void setUp() throws Exception {
        testDir = extractResources("/mng-8525-maven-di-plugin");
    }

    @Test
    public void testMavenDIPlugin() throws Exception {
        //
        // Build a plugin that uses a Maven DI plugin
        //
        Verifier v0 = newVerifier(testDir.getAbsolutePath());
        v0.setAutoclean(false);
        v0.deleteDirectory("target");
        v0.deleteArtifacts("org.apache.maven.plugins");
        v0.addCliArgument("install");
        v0.execute();
        v0.verifyErrorFreeLog();

        //
        // Execute the Maven DI plugin
        //
        Verifier v1 = newVerifier(testDir.getAbsolutePath());
        v1.setAutoclean(false);
        v1.addCliArgument("org.apache.maven.plugins:mavendi-maven-plugin:0.0.1-SNAPSHOT:hello");
        v1.execute();
        v1.verifyErrorFreeLog();
        v1.verifyTextInLog("Hello! I am a component that is being used via field injection!");
    }
}
