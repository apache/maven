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
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2690">MNG-2690</a>.
 *
 * It checks, at the most basic level possible, that the plugin manager is intercepting things like
 * {@link NoClassDefFoundError} and ComponentLookupException, then throwing user-friendly errors when loading and
 * configuring a mojo.
 *
 * @author jdcasey
 */
public class MavenITmng2690MojoLoadingErrorsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2690MojoLoadingErrorsTest() {
        super("(2.1.0-M1,)");
    }

    @Test
    public void testNoClassDefFromMojoLoad() throws IOException, VerificationException {
        File testDir = extractResources("/mng-2690/noclassdef-mojo");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);

        verifier.addCliArgument("validate");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "should throw an error during execution.");

        List<String> lines = verifier.loadFile(new File(testDir, "log.txt"), false);

        int msg = indexOf(lines, "(?i).*required class is missing.*");
        assertTrue(msg >= 0, "User-friendly message was not found in output.");

        int cls = lines.get(msg).toString().replace('/', '.').indexOf("junit.framework.TestCase");
        assertTrue(cls >= 0, "Missing class name was not found in output.");
    }

    @Test
    public void testNoClassDefFromMojoConfiguration() throws IOException, VerificationException {
        File testDir = extractResources("/mng-2690/noclassdef-param");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);

        verifier.addCliArgument("validate");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "should throw an error during execution.");

        List<String> lines = verifier.loadFile(new File(testDir, "log.txt"), false);

        int msg = indexOf(lines, "(?i).*required class (i|wa)s missing( during (mojo )?configuration)?.*");
        assertTrue(msg >= 0, "User-friendly message was not found in output.");

        int cls = lines.get(msg).toString().replace('/', '.').indexOf("junit.framework.TestCase");
        assertTrue(cls >= 0, "Missing class name was not found in output.");
    }

    @Test
    public void testMojoComponentLookupException() throws IOException, VerificationException {
        File testDir = extractResources("/mng-2690/mojo-complookup");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);

        verifier.addCliArgument("validate");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "should throw an error during execution.");

        List<String> lines = verifier.loadFile(new File(testDir, "log.txt"), false);

        String compLookupMsg = "(?i).*unable to .* mojo 'mojo-component-lookup-exception' .* plugin "
                + "'org\\.apache\\.maven\\.its\\.plugins:maven-it-plugin-error.*";

        assertTrue(indexOf(lines, compLookupMsg) > 0, "User-friendly message was not found in output.");
    }

    @Test
    public void testMojoRequirementComponentLookupException() throws IOException, VerificationException {
        File testDir = extractResources("/mng-2690/requirement-complookup");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);

        verifier.addCliArgument("validate");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "should throw an error during execution.");

        List<String> lines = verifier.loadFile(new File(testDir, "log.txt"), false);

        String compLookupMsg = "(?i).*unable to .* mojo 'requirement-component-lookup-exception' .* plugin "
                + "'org\\.apache\\.maven\\.its\\.plugins:maven-it-plugin-error.*";

        assertTrue(indexOf(lines, compLookupMsg) > 0, "User-friendly message was not found in output.");
    }

    private int indexOf(List<String> logLines, String regex) {
        Pattern pattern = Pattern.compile(regex);

        for (int i = 0; i < logLines.size(); i++) {
            String logLine = logLines.get(i);

            if (pattern.matcher(logLine).matches()) {
                return i;
            }
        }

        return -1;
    }
}
