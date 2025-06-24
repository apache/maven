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
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for <a href="https://issues.apache.org/jira/browse/MNG-4463">MNG-4463</a>.
 *
 * @author Christian Schulte
 */
public class MavenITmng4463DependencyManagementImportVersionRanges extends AbstractMavenIntegrationTestCase {

    public MavenITmng4463DependencyManagementImportVersionRanges() {
        super("[4.0.0-alpha-1,)");
    }

    @Test
    public void testInclusiveUpperBoundResolvesToHighestVersion() throws Exception {
        final File testDir = extractResources("/mng-4463/inclusive-upper-bound");
        final Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        final List<String> artifacts = verifier.loadLines("target/compile.txt");
        assertTrue(artifacts.contains("org.apache.maven:maven-plugin-api:jar:3.0"), artifacts.toString());
    }

    @Test
    public void testExclusiveUpperBoundResolvesToHighestVersion() throws Exception {
        final File testDir = extractResources("/mng-4463/exclusive-upper-bound");
        final Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("target/compile.txt");
        assertTrue(artifacts.contains("org.apache.maven:maven-plugin-api:jar:3.0"), artifacts.toString());
    }

    @Test
    public void testFailureWithoutUpperBound() throws Exception {
        final File testDir = extractResources("/mng-4463/no-upper-bound");
        final Verifier verifier = newVerifier(testDir.getAbsolutePath());

        try {
            verifier.setAutoclean(false);
            verifier.deleteDirectory("target");
            verifier.addCliArgument("validate");
            verifier.execute();
            fail("Expected 'VerificationException' not thrown.");
        } catch (final VerificationException e) {
            final List<String> lines = verifier.loadFile(new File(testDir, "log.txt"), false);
            assertTrue(
                    indexOf(lines, ".*dependency version range.*does not specify an upper bound.*") >= 0,
                    "Expected error message not found.");
        }
    }

    private static int indexOf(final List<String> logLines, final String regex) {
        final Pattern pattern = Pattern.compile(regex);

        for (int i = 0, l0 = logLines.size(); i < l0; i++) {
            if (pattern.matcher(logLines.get(i)).matches()) {
                return i;
            }
        }

        return -1;
    }
}
