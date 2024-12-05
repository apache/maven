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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3746">MNG-3746</a>.
 *
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 */
public class MavenITmng3746POMPropertyOverrideTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3746POMPropertyOverrideTest() {
        super("(2.0.8,)"); // only test in 2.0.9+
    }

    @Test
    public void testitMNG3746_UsingDefaultSystemProperty() throws Exception {
        // The testdir is computed from the location of this
        // file.
        File testDir = extractResources("/mng-3746");
        File pluginDir = new File(testDir, "maven-mng3746-plugin");
        File projectDir = new File(testDir, "project");

        Verifier verifier;

        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.setLogFileName("log-sys.txt");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.setLogFileName("log-sys.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testitMNG3746_UsingCLIProperty() throws Exception {
        // The testdir is computed from the location of this
        // file.
        File testDir = extractResources("/mng-3746");
        File pluginDir = new File(testDir, "maven-mng3746-plugin");
        File projectDir = new File(testDir, "project");

        Verifier verifier;

        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.setLogFileName("log-cli.txt");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.setLogFileName("log-cli.txt");

        verifier.addCliArgument("-Dtest.verification=cli");
        verifier.addCliArgument("-Dtest.usingCliValue=true");
        verifier.addCliArgument("-Djava.version=cli");

        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
