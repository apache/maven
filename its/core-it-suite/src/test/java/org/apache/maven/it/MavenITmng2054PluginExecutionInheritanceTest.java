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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2054">MNG-2054</a>.
 *
 *
 */
public class MavenITmng2054PluginExecutionInheritanceTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2054PluginExecutionInheritanceTest() {
        super("(2.0.3,)");
    }

    /**
     * Test that plugin executions from &gt;1 step of inheritance don't run multiple times.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2054() throws Exception {
        File testDir = extractResources("/mng-2054");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("project/project-level2/project-level3/project-jar/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> executions =
                verifier.loadLines("project/project-level2/project-level3/project-jar/target/exec.log");
        List<String> expected = Arrays.asList(new String[] {"once"});
        assertEquals(expected, executions);
    }
}
