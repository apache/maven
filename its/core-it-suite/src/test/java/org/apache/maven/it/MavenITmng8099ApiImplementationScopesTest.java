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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for api and implementation dependency scopes (MNG-8099).
 *
 * Verifies that:
 * - api-scoped deps are transitively visible to consumers at compile time
 * - implementation-scoped deps are NOT transitively visible at compile time
 *
 * The test installs a library that uses both api and implementation scopes,
 * then builds two standalone consumers:
 * - consumer: directly uses ApiDep (transitive via api) — should compile
 * - consumer-impl-fail: directly uses ImplDep (transitive via implementation) — should fail
 */
public class MavenITmng8099ApiImplementationScopesTest extends AbstractMavenIntegrationTestCase {

    @BeforeEach
    void installDependencies() throws Exception {
        Path testDir = extractResources("mng-8099-api-implementation-scopes");

        Verifier verifier = newVerifier(testDir, false);
        verifier.addCliArgument("install");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    /**
     * Consumer directly references ApiDep (transitive via api scope from library).
     * Should compile successfully because api deps are transitive at compile time.
     */
    @Test
    public void testApiScopeIsTransitiveAtCompileTime() throws Exception {
        Path testDir = extractResources("mng-8099-api-implementation-scopes");
        Path consumerDir = testDir.resolve("consumer");

        Verifier verifier = newVerifier(consumerDir.toString(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("compile");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    /**
     * Consumer directly references ImplDep (transitive via implementation scope from library).
     * Should FAIL to compile because implementation deps are NOT transitive at compile time.
     */
    @Test
    public void testImplementationScopeIsNotTransitiveAtCompileTime() throws Exception {
        Path testDir = extractResources("mng-8099-api-implementation-scopes");
        Path consumerDir = testDir.resolve("consumer-impl-fail");

        Verifier verifier = newVerifier(consumerDir.toString(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("compile");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");

        assertThrows(
                VerificationException.class,
                verifier::execute,
                "Compilation should fail because ImplDep is not transitively visible at compile time");
    }
}
