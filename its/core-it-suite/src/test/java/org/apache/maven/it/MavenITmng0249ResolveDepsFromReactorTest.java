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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-249">MNG-249</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0249ResolveDepsFromReactorTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng0249ResolveDepsFromReactorTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that the reactor can establish the artifact location of known projects for dependencies.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG0249() throws Exception {
        File testDir = extractResources("/mng-0249");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> ccp = verifier.loadLines("test-component-c/target/compile.txt");
        assertTrue(ccp.contains("test-component-c/classes"), ccp.toString());
        assertTrue(ccp.contains("test-component-b/classes"), ccp.toString());
        assertTrue(ccp.contains("test-component-a/classes"), ccp.toString());

        List<String> rcp = verifier.loadLines("test-component-c/target/runtime.txt");
        assertTrue(rcp.contains("test-component-c/classes"), rcp.toString());
        assertTrue(rcp.contains("test-component-b/classes"), rcp.toString());
        assertTrue(rcp.contains("test-component-a/classes"), rcp.toString());

        List<String> tcp = verifier.loadLines("test-component-c/target/test.txt");
        assertTrue(tcp.contains("test-component-c/classes"), tcp.toString());
        assertTrue(tcp.contains("test-component-b/classes"), tcp.toString());
        assertTrue(tcp.contains("test-component-a/classes"), tcp.toString());
    }
}
