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
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Regression test for <a href="https://github.com/apache/maven-dependency-plugin/issues/1024">MDEP-590</a>.
 */
class MavenITmdep0590ClassifiedPomArtifactFromReactorTest extends AbstractMavenIntegrationTestCase {

    @Test
    void classifiedPomShouldResolveToAttachedArtifact() throws Exception {
        Path testDir = extractResources("mdep-590");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("consumer/target");
        verifier.deleteArtifacts("org.apache.maven.its.mdep590");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties properties = verifier.loadProperties("consumer/target/artifact.properties");
        ItUtils.assertCanonicalFileEquals(
                testDir.resolve("producer/custom.pom"),
                Path.of(properties.getProperty(
                        "org.apache.maven.its.mdep590:producer:pom:custom:1.0-SNAPSHOT")));
    }
}
