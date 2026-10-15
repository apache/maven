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
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/13413">gh-13413</a>:
 * a value with white space which is quoted in {@code MAVEN_ARGS}, like a path with a space for
 * {@code -Dmaven.repo.local}, must reach Maven as a single argument.
 * <p>
 * The launcher script is run directly, and not through the executor of the test harness, because that executor
 * splits {@code MAVEN_ARGS} itself and does not pass it on to the script. On Windows this runs {@code mvn.cmd}.
 */
class MavenITgh13413QuotedValuesInMavenArgsTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testQuotedValueWithSpace() throws Exception {
        Path basedir = extractResources("gh-13413-maven-args-quoting");
        // a path with a space, as the Jenkins Pipeline Maven plugin builds for a workspace like that
        Path localRepo = basedir.resolve("local repo");

        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        Path bin = Path.of(System.getProperty("maven.home")).resolve("bin");
        List<String> command = windows
                ? List.of("cmd.exe", "/c", bin.resolve("mvn.cmd").toString(), "-B", "validate")
                : List.of(bin.resolve("mvn").toString(), "-B", "validate");

        ProcessBuilder builder = new ProcessBuilder(command).directory(basedir.toFile()).redirectErrorStream(true);
        builder.environment().put("MAVEN_ARGS", "\"-Dmaven.repo.local=" + localRepo + "\" -Dprop.plain=plain");
        builder.environment().put("MAVEN_SKIP_RC", "true");
        builder.environment().remove("MAVEN_OPTS");
        builder.environment().remove("MAVEN_CONFIG");
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(2, TimeUnit.MINUTES), "Maven did not end in time:\n" + output);

        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("BUILD SUCCESS"), output);
    }
}
