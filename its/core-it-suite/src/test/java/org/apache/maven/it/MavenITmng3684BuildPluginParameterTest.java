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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3684">MNG-3684</a>:
 * Verify that the Build instance injected as a plugin parameter contains
 * interpolated values for things like the various build paths, etc.
 *
 * @author jdcasey
 */
public class MavenITmng3684BuildPluginParameterTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3684BuildPluginParameterTest() {
        super("(2.0.9,)");
    }

    @Test
    public void testitMNG3684() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3684");
        File pluginDir = new File(testDir, "maven-mng3684-plugin");
        File projectDir = new File(testDir, "project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath(), "remote");
        verifier.setLogFileName("log-validate.txt");
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier.setLogFileName("log-site.txt");
        verifier.addCliArgument("site");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
