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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7804">MNG-7804</a>.
 * Verifies that plugin execution can be ordered across different plugins.
 *
 */
class MavenITmng7804PluginExecutionOrderTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng7804PluginExecutionOrderTest() {
        super("[4.0.0-alpha-6,)");
    }

    /**
     * Verify that plugin executions are executed in order
     *
     * @throws Exception in case of failure
     */
    @Test
    void testOrder() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7804-plugin-execution-order");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> executions = verifier.loadLogLines().stream()
                .filter(l -> l.contains(" This should be "))
                .collect(Collectors.toList());
        assertEquals(4, executions.size());
        assertTrue(executions.get(0).contains("100"));
        assertTrue(executions.get(1).contains("200"));
        assertTrue(executions.get(2).contains("300"));
        assertTrue(executions.get(3).contains("400"));
    }
}
