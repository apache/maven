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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4129">MNG-4129</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4129PluginExecutionInheritanceTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4129PluginExecutionInheritanceTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Verify that plugin executions defined in the parent with inherited=false are not executed in child modules.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4129");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("child-1/target");
        verifier.deleteDirectory("child-2/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> executions = verifier.loadLines("target/executions.txt");
        Collections.sort(executions);
        assertEquals(Arrays.asList(new String[] {"inherited-execution", "non-inherited-execution"}), executions);

        List<String> executions1 = verifier.loadLines("child-1/target/executions.txt");
        assertEquals(Collections.singletonList("inherited-execution"), executions1);

        List<String> executions2 = verifier.loadLines("child-2/target/executions.txt");
        assertEquals(Collections.singletonList("inherited-execution"), executions2);
    }
}
