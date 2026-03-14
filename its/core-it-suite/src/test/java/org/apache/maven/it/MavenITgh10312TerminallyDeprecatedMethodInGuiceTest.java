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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/10312">GH-10312</a>.
 */
class MavenITgh10312TerminallyDeprecatedMethodInGuiceTest extends AbstractMavenIntegrationTestCase {

    MavenITgh10312TerminallyDeprecatedMethodInGuiceTest() {}

    @Test
    @EnabledForJreRange(min = JRE.JAVA_24)
    void worryingShouldNotBePrinted() throws Exception {
        File testDir = extractResources("/gh-10312-terminally-deprecated-method-in-guice");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);
        verifier.addCliArgument("validate");
        verifier.execute();

        assertTrue(verifier.getStdout().isEmpty(), "Expected no output on stdout, but got: " + verifier.getStdout());

        assertFalse(
                verifier.getStderr()
                        .contains(
                                "WARNING: sun.misc.Unsafe::staticFieldBase has been called by com.google.inject.internal.aop.HiddenClassDefiner"),
                "Expected no warning about sun.misc.Unsafe::staticFieldBase, but got: " + verifier.getStderr());
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_24, max = JRE.JAVA_25)
    void allowOverwriteByUser() throws Exception {
        File testDir = extractResources("/gh-10312-terminally-deprecated-method-in-guice");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);
        verifier.addCliArgument("validate");
        verifier.addCliArgument("-Dguice_custom_class_loading=BRIDGE");
        verifier.execute();

        assertTrue(verifier.getStdout().isEmpty(), "Expected no output on stdout, but got: " + verifier.getStdout());

        assertTrue(
                verifier.getStderr()
                        .contains(
                                "WARNING: sun.misc.Unsafe::staticFieldBase has been called by com.google.inject.internal.aop.HiddenClassDefiner"),
                "Expected warning about sun.misc.Unsafe::staticFieldBase, but got: " + verifier.getStderr());
    }
}
