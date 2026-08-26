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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenITmng8709ProfileDependencyVersionTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8709ProfileDependencyVersionTest() {
        super("[4.0.0-rc-7,)");
    }

    @Test
    void dependencyVersionFromActiveProfileIsValidForConsumerPom() throws Exception {
        Path basedir = extractResources("/mng-8709-profile-dependency-version")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPom = Path.of(verifier.getArtifactPath(
                "org.apache.maven.its.mng8709", "profile-version", "1.0", "pom"));
        String content = Files.readString(consumerPom);
        assertTrue(content.contains("<activeByDefault>true</activeByDefault>"));
        assertTrue(content.contains("<version>${junit.version}</version>"));
    }
}
