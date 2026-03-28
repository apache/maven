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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11715">GH-11715</a>.
 *
 * Verifies that help:effective-pom preserves the 4.1.0 root namespace/schema for a 4.1.0 POM.
 *
 * @since 4.0.0-rc-5
 */
class MavenITgh11715EffectivePomNamespaceTest extends AbstractMavenIntegrationTestCase {

    MavenITgh11715EffectivePomNamespaceTest() {
        super("(4.0.0-rc-5,)");
    }

    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/gh-11715").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("help:effective-pom");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("<modelVersion>4.1.0</modelVersion>");
        verifier.verifyTextInLog("<project xmlns=\"http://maven.apache.org/POM/4.1.0\"");
        verifier.verifyTextInLog(
                "xsi:schemaLocation=\"http://maven.apache.org/POM/4.1.0 https://maven.apache.org/xsd/maven-4.1.0.xsd\"");
    }
}
