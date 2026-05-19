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
 * Verifies that the launcher script does not expand <code>${...}</code> patterns
 * in CLI arguments. Regression test for the {@code eval exec} shell expansion
 * that broke any argument containing Maven property placeholders.
 *
 * <h2>Real-world scenario</h2>
 *
 * <p>This bug was originally surfaced by maven-surefire-plugin integration tests.
 * Surefire allows users to declare system properties for the forked test JVM with
 * placeholders that surefire substitutes <em>at fork time</em>, e.g.
 * {@code -DtestProperty=testValue_${surefire.threadNumber}_${surefire.forkNumber}}.
 * The intended flow is:
 * <ol>
 *   <li>The user invokes {@code mvn} with the literal placeholder on the CLI.</li>
 *   <li>The launcher script must pass the literal {@code ${...}} verbatim to
 *       the JVM as a system property value.</li>
 *   <li>The surefire plugin reads the system property, performs <em>its own</em>
 *       interpolation when forking each test JVM, replacing
 *       {@code ${surefire.threadNumber}} and {@code ${surefire.forkNumber}}
 *       with the actual fork/thread numbers.</li>
 *   <li>Each forked test JVM thus sees a unique substituted value.</li>
 * </ol>
 *
 * <p>The bug broke step 2: the launcher script's {@code eval exec} re-parsed
 * the command string and invoked shell variable expansion on {@code ${...}}.
 * Names containing dots (such as {@code surefire.threadNumber}) are invalid
 * shell variable names, so the shell aborted with {@code bad substitution}
 * before Maven even started, breaking all surefire forked-test ITs that relied
 * on this pattern.
 *
 * <h2>Test design</h2>
 *
 * <p>The placeholder name contains dots, mirroring surefire's real usage and
 * deliberately producing an invalid shell variable name. Without the fix:
 * <ul>
 *   <li>The shell's {@code eval exec} aborts with {@code bad substitution}.</li>
 *   <li>The build fails immediately and {@link Verifier#verifyErrorFreeLog()}
 *       catches it.</li>
 * </ul>
 * With the fix:
 * <ul>
 *   <li>The literal {@code ${...}} arrives at Maven as a system property value.</li>
 *   <li>Maven's recursive property interpolation resolves the unknown placeholder
 *       to an empty string when reading {@code ${test.placeholder}} from the POM.</li>
 *   <li>The resulting value is {@code -value__end-}.</li>
 * </ul>
 *
 * <p>Note: this test cannot easily replicate surefire's late-binding interpolation
 * (step 3 above) because Maven's standard property interpolation does not
 * recursively resolve POM properties for placeholders embedded inside a system
 * property value. Surefire performs that substitution in its own plugin code at
 * fork time. The signal that proves the fix works is the absence of a
 * {@code bad substitution} shell error, not the resolved value itself.
 */
class MavenITgh11978PlaceholderInCliArgTest extends AbstractMavenIntegrationTestCase {

    MavenITgh11978PlaceholderInCliArgTest() {
        super("[4.0.0-rc-1,)");
    }

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
        verifier.verifyErrorFreeLog(); // key check: fails immediately on shell crash

        // Sanity check: the value flowed through Maven, which resolves the unknown
        // ${some.maven.placeholder} to empty during recursive interpolation.
        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("-value__end-", props.getProperty("project.properties.pom.placeholder"));
    }
}
