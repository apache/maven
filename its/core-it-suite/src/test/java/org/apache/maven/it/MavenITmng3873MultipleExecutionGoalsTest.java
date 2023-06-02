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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3873">MNG-3873</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3873MultipleExecutionGoalsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3873MultipleExecutionGoalsTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that all goals from a plugin execution are actually executed and not only one when no {@code <pluginManagement>}
     * is involved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithoutPluginMngt() throws Exception {
        testitMNG3873("test-1");
    }

    /**
     * Test that all goals from a plugin execution are actually executed and not only one when {@code <pluginManagement>}
     * is involved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithPluginMngt() throws Exception {
        testitMNG3873("test-2");
    }

    private void testitMNG3873(String project) throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3873");

        Verifier verifier = newVerifier(new File(testDir, project).getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        /*
         * NOTE: We deliberately don't check the exact order here, that's another issue.
         */
        List<String> lines = verifier.loadLines("target/it.log", "UTF-8");
        assertTrue(lines.toString(), lines.contains("test"));
        assertTrue(lines.toString(), lines.contains("----"));
    }
}
