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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3927">MNG-3927</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3927PluginDefaultExecutionConfigTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3927PluginDefaultExecutionConfigTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that the configuration for a plugin execution without an identifier does not pollute the configuration
     * of default plugin executions introduced by the packaging.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3927() throws Exception {
        File testDir = extractResources("/mng-3927");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("process-resources");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent("target/failed.txt");
        verifier.verifyFilePresent("target/resources-resources.txt");
    }
}
