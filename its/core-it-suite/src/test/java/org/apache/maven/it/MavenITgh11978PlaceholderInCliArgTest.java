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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11978">gh-11978</a>.
 *
 * <p>Verifies that the launcher script does not expand {@code ${...}} patterns
 * in CLI arguments. The placeholder name intentionally contains dots (invalid as a
 * shell variable name) so without the fix, {@code eval exec} aborts with
 * {@code bad substitution} before Maven even starts.
 */
class MavenITgh11978PlaceholderInCliArgTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/gh-11978-placeholder-in-cli-arg")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.setForkJvm(true); // NOTE: We want to go through the launcher script
        // The placeholder name contains dots, which is invalid as a shell variable name.
        // Without the fix, the shell's `eval exec` aborts with "bad substitution".
        // With the fix, the literal ${...} arrives at Maven.
        verifier.addCliArgument("-Dtest.placeholder=value_${some.maven.placeholder}_end");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog(); // primary check: shell crash produces non-zero exit

        // Secondary: verify the literal placeholder flowed through Maven.
        // Maven resolves the unknown ${some.maven.placeholder} to empty.
        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("-value__end-", props.getProperty("project.properties.pom.placeholder"));
    }
}
