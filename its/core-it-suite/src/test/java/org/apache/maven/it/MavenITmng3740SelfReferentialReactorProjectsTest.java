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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3740">MNG-3740</a>.
 *
 * Check that when a plugin project build uses an earlier version of itself, it
 * doesn't result in a StackOverflowError as a result of trying to calculate
 * a concrete state for its project references, which includes itself because of
 * this plugin configuration in the POM.
 *
 * @author jdcasey
 *
 * @since 2.0.8
 *
 */
public class MavenITmng3740SelfReferentialReactorProjectsTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testitMNG3740() throws Exception {
        Path testDir = extractResources("mng-3740");
        Path v1 = testDir.resolve("projects.v1");
        Path v2 = testDir.resolve("projects.v2");

        Verifier verifier;

        verifier = newVerifier(v1);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(v2);
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
