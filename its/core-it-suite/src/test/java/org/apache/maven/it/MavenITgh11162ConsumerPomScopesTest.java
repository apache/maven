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

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that consumer POM keeps only "compile" and "runtime" scoped dependencies
 * and drops other scopes including the new scopes introduced by Maven 4.
 * @since 4.0.0-rc-3
 *
 */
class MavenITgh11162ConsumerPomScopesTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testConsumerPomFiltersScopes() throws Exception {
        Path basedir = extractResources("/gh-11162-consumer-pom-scopes").toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("install");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPom = basedir.resolve(Paths.get(
                "target",
                "project-local-repo",
                "org.apache.maven.its.gh11162",
                "consumer-pom-scopes-app",
                "1.0",
                "consumer-pom-scopes-app-1.0-consumer.pom"));
        assertTrue(Files.exists(consumerPom), "consumer pom not found at " + consumerPom);

        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }

        long numDeps = consumerPomModel.getDependencies() != null
                ? consumerPomModel.getDependencies().size()
                : 0;
        assertEquals(2, numDeps, "Consumer POM should keep only compile and runtime dependencies");

        boolean hasCompile = consumerPomModel.getDependencies().stream()
                .anyMatch(d -> "compile".equals(d.getScope()) && "compile-dep".equals(d.getArtifactId()));
        boolean hasRuntime = consumerPomModel.getDependencies().stream()
                .anyMatch(d -> "runtime".equals(d.getScope()) && "runtime-dep".equals(d.getArtifactId()));
        assertTrue(hasCompile, "compile dependency should be present");
        assertTrue(hasRuntime, "runtime dependency should be present");

        long dropped = consumerPomModel.getDependencies().stream()
                .map(d -> d.getScope())
                .filter(s -> !"compile".equals(s) && !"runtime".equals(s))
                .count();
        assertEquals(0, dropped, "All non compile/runtime scopes should be dropped in consumer POM");
    }
}
