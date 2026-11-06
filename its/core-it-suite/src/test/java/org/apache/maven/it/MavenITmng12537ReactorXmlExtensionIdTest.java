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
 * Verifies that an extension declared in {@code .mvn/reactor.xml} using the
 * {@code id="groupId:artifactId:version"} shorthand is correctly parsed
 * through the full StAX reader → {@code toCoreExtension()} pipeline and
 * loaded at build time.
 *
 * @see <a href="https://github.com/apache/maven/pull/13249">#13249</a>
 */
class MavenITmng12537ReactorXmlExtensionIdTest extends AbstractMavenIntegrationTestCase {

    /**
     * Install the extension into the IT local repository, then run the client
     * project whose {@code .mvn/reactor.xml} declares the same extension using
     * the {@code id="g:a:v"} shorthand.  If the StAX reader correctly parses
     * the attribute and routes it through {@code toCoreExtension()}, the
     * extension is loaded and prints its marker log message.
     */
    @Test
    void extensionLoadedViaIdShorthandInReactorXml() throws Exception {
        Path testDir = extractResources("/mng-12537-reactor-xml-extension-id");

        // Install the extension artifact into the shared IT local repository.
        Verifier verifier = newVerifier(testDir.resolve("extension"));
        verifier.setLogFileName("extension-install.txt");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Run the client project: reactor.xml declares the extension via id=g:a:v.
        verifier = newVerifier(testDir.resolve("client"));
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[MNG-12537] Extension loaded via id shorthand!");
    }
}
