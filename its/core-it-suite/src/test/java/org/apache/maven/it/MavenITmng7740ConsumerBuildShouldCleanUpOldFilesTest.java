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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenITmng7740ConsumerBuildShouldCleanUpOldFilesTest extends AbstractMavenIntegrationTestCase {

    protected MavenITmng7740ConsumerBuildShouldCleanUpOldFilesTest() {
        super("[4.0.0-alpha-6,)");
    }

    @Test
    void testConsumerBuildShouldCleanUpOldConsumerFiles() throws Exception {
        File testDir = extractResources("/mng-7740-consumer-files");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");

        verifier.execute();

        verifier.verifyErrorFreeLog();

        try (Stream<Path> stream = Files.walk(testDir.toPath())) {
            final List<Path> consumerFiles = stream.filter(
                            path -> path.getFileName().toString().contains("consumer")
                                    && path.getFileName().toString().contains("pom"))
                    .collect(Collectors.toList());
            assertTrue(consumerFiles.size() == 0, "Expected no consumer pom file.");
        }
    }
}
