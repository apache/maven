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
 * Verifies that an alias defined in {@code .mvn/reactor.xml} is expanded by the CLI parser.
 *
 * <p>The test project defines {@code ci} \u2192 {@code verify -Dmaven.test.skip=true}.
 * Running {@code mvn ci} must therefore execute the {@code verify} lifecycle
 * and skip test compilation/execution.
 *
 * @see <a href="https://github.com/apache/maven/issues/12537">MNG-12537</a>
 */
class MavenITmng12537ReactorXmlAliasTest extends AbstractMavenIntegrationTestCase {

    @Test
    void aliasIsExpandedAndVerifyRuns() throws Exception {
        Path testDir = extractResources("/mng-12537-reactor-xml-alias");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("ci"); // alias \u2192 verify -Dmaven.test.skip=true
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // verify phase ran: classes must have been compiled
        verifier.verifyFilePresent("target/classes");
        // tests were skipped (-Dmaven.test.skip=true)
        verifier.verifyTextNotInLog("Tests run:");
    }
}
