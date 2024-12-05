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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3938">MNG-3938</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3938MergePluginExecutionsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3938MergePluginExecutionsTest() {
        super("(2.0.4,)");
    }

    /**
     * Test that plugin executions with the same id are merged during inheritance, especially executions using the
     * default id, regardless whether the id is given explicitly by the user or implicitly assumed from defaults, when
     * no {@code <pluginManagement>} is involved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithoutPluginMngt() throws Exception {
        testitMNG3938("test-1");
    }

    /**
     * Test that plugin executions with the same id are merged during inheritance, especially executions using the
     * default id, regardless whether the id is given explicitly by the user or implicitly assumed from defaults, when
     * {@code <pluginManagement>} is involved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithPluginMngt() throws Exception {
        testitMNG3938("test-2");
    }

    private void testitMNG3938(String project) throws Exception {
        File testDir = extractResources("/mng-3938/" + project);

        Verifier verifier = newVerifier(new File(testDir, "sub").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines("target/default.log");
        assertEquals(Arrays.asList(new String[] {"child"}), lines);

        lines = verifier.loadLines("target/non-default.log");
        assertEquals(Arrays.asList(new String[] {"child"}), lines);

        verifier.verifyFileNotPresent("target/parent.log");
    }
}
