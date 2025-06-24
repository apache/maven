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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2130">MNG-2130</a>.
 */
public class MavenITmng2130ParentLookupFromReactorCacheTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng2130ParentLookupFromReactorCacheTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that parent-POMs cached during a build are available as parents
     * to other POMs in the multi-module build.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2130() throws Exception {
        File testDir = extractResources("/mng-2130");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.mng2130");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
