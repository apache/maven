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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3498">MNG-3498</a>.
 *
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3498ForkToOtherMojoTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3498ForkToOtherMojoTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    @Test
    public void testitMNG3498() throws Exception {
        // The testdir is computed from the location of this
        // file.
        File testDir = extractResources("/mng-3498");

        File pluginDir = new File(testDir, "maven-mng3498-plugin");
        File projectDir = new File(testDir, "mng-3498-project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.deleteArtifact("org.apache.maven.its.mng3498", "mavenit-mng3498-plugin", "1", "pom");

        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
