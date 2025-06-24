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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for
 * <a href="https://issues.apache.org/jira/browse/MNG-7464">MNG-7464</a>
 *
 * @author Slawomir Jaranowski
 */
public class MavenITmng7464ReadOnlyMojoParametersWarningTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng7464ReadOnlyMojoParametersWarningTest() {
        super("[3.9.0,)");
    }

    /**
     * Test that ensures that warning is not printed for empty and default value
     */
    @Test
    public void testEmptyConfiguration() throws Exception {
        File testDir = extractResources("/mng-7464-mojo-read-only-params");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-empty-configuration.txt");
        verifier.addCliArgument("-Dmaven.plugin.validation=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLogLines();
        List<String> warnLines = findReadOnlyWarning(logLines);

        assertTrue(warnLines.isEmpty(), "Unwanted warnings: " + warnLines);
    }

    /**
     * Test that ensures that warning is printed for read-only parameter set by property
     */
    @Test
    public void testReadOnlyProperty() throws Exception {
        File testDir = extractResources("/mng-7464-mojo-read-only-params");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-Duser.property=value");
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-read-only-property.txt");
        verifier.addCliArgument("-Dmaven.plugin.validation=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLogLines();
        List<String> warnLines = findReadOnlyWarning(logLines);

        assertTrue(
                warnLines.stream()
                        .anyMatch(
                                s -> s.contains(
                                        "Parameter 'readOnlyWithUserProperty' (user property 'user.property') is read-only, must not be used in configuration")));
    }

    /**
     * Test that ensures that warning is printed for read-only parameter set by plugin configuration.
     */
    @Test
    public void testReadOnlyConfig() throws Exception {
        File testDir = extractResources("/mng-7464-mojo-read-only-params");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-read-only-configuration.txt");
        verifier.addCliArgument("-Pconfig-values");
        verifier.addCliArgument("-Dmaven.plugin.validation=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLogLines();
        List<String> warnLines = findReadOnlyWarning(logLines);

        assertTrue(warnLines.stream()
                .anyMatch(s ->
                        s.contains("Parameter 'readOnlyWithDefault' is read-only, must not be used in configuration")));

        assertTrue(warnLines.stream()
                .anyMatch(s -> s.contains(
                        "Parameter 'readOnlyWithOutDefaults' is read-only, must not be used in configuration")));

        assertTrue(
                warnLines.stream()
                        .anyMatch(
                                s -> s.contains(
                                        "Parameter 'readOnlyWithProperty' (user property 'project.version') is read-only, must not be used in configuration")));

        assertTrue(
                warnLines.stream()
                        .anyMatch(
                                s -> s.contains(
                                        "Parameter 'readOnlyWithUserProperty' (user property 'user.property') is read-only, must not be used in configuration")));
    }

    private List<String> findReadOnlyWarning(List<String> logLines) {
        Pattern pattern = Pattern.compile(".* Parameter .* is read-only.*");
        List<String> result = new ArrayList<>();
        for (String line : logLines) {
            if (pattern.matcher(line).matches()) {
                result.add(line);
            }
        }
        return result;
    }
}
