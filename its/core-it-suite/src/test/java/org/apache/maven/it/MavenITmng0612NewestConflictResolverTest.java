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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-612">MNG-612</a>.
 *
 * @author Mark Hobson
 *
 */
@Disabled
public class MavenITmng0612NewestConflictResolverTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng0612NewestConflictResolverTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that ensures the newest-wins conflict resolver is used.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG612() throws Exception {
        File testDir = extractResources("/mng-0612/dependency");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        testDir = extractResources("/mng-0612/plugin");
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        testDir = extractResources("/mng-0612/project");
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
