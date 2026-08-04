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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Integration test for the incremental build context API
 * (<a href="https://github.com/apache/maven/pull/12576">#12576</a>).
 * <p>
 * Verifies end-to-end incremental build behavior using a test plugin that
 * injects {@code IncrementalContext} and uses it to copy files incrementally:
 * <ol>
 *   <li>Initial build: all source files are NEW and get processed</li>
 *   <li>No-change rebuild: all files are UNMODIFIED and get skipped</li>
 *   <li>After modification: only the changed file is re-processed</li>
 * </ol>
 */
class MavenITgh12576IncrementalContextTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testIncrementalContext() throws Exception {
        Path testDir = extractResources("/gh-12576-incremental-context");

        // Step 1: install the incremental-copy test plugin
        Verifier pluginVerifier = newVerifier(testDir.resolve("plugin"));
        pluginVerifier.addCliArgument("install");
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        // Step 2: initial build — all files should be processed as NEW
        // Use clean to ensure no stale incremental state from prior runs
        Verifier v1 = newVerifier(testDir.resolve("consumer"));
        v1.setLogFileName("log-build1.txt");
        v1.addCliArgument("clean");
        v1.addCliArgument("process-sources");
        v1.execute();
        v1.verifyErrorFreeLog();
        v1.verifyTextInLog("[incremental] Processed file1.txt (NEW)");
        v1.verifyTextInLog("[incremental] Processed file2.txt (NEW)");
        v1.verifyTextInLog("[incremental] Summary: 2 processed, 0 skipped");
        v1.verifyFilePresent("target/data/file1.txt");
        v1.verifyFilePresent("target/data/file2.txt");

        // Step 3: no-change rebuild — all files should be skipped
        Verifier v2 = newVerifier(testDir.resolve("consumer"));
        v2.setAutoclean(false);
        v2.setLogFileName("log-build2.txt");
        v2.addCliArgument("process-sources");
        v2.execute();
        v2.verifyErrorFreeLog();
        v2.verifyTextInLog("[incremental] Summary: 0 processed, 2 skipped");

        // Step 4: modify one file, then rebuild — only the modified file should be re-processed
        Path file1 = testDir.resolve("consumer/src/main/data/file1.txt");
        // Ensure the timestamp actually changes (some filesystems have 1-second granularity)
        Thread.sleep(1100);
        Files.write(file1, "modified content for incremental rebuild test\n".getBytes(StandardCharsets.UTF_8));

        Verifier v3 = newVerifier(testDir.resolve("consumer"));
        v3.setAutoclean(false);
        v3.setLogFileName("log-build3.txt");
        v3.addCliArgument("process-sources");
        v3.execute();
        v3.verifyErrorFreeLog();
        v3.verifyTextInLog("[incremental] Processed file1.txt (MODIFIED)");
        v3.verifyTextInLog("[incremental] Summary: 1 processed, 1 skipped");
    }
}
