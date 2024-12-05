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
 * <a href="https://issues.apache.org/jira/browse/MNG-7468">MNG-7468</a>
 *
 * @author Slawomir Jaranowski
 */
public class MavenITmng7468UnsupportedPluginsParametersTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng7468UnsupportedPluginsParametersTest() {
        super("[3.9.0,)");
    }

    /**
     * Test that ensures that warning is not printed for empty configuration
     */
    @Test
    public void testNoConfiguration() throws Exception {
        List<String> warnLines = performTest("no-config");
        assertTrue(warnLines.isEmpty(), "Unwanted warnings: " + warnLines);
    }

    /**
     * Test that ensures that warning is not printed for valid parameters
     */
    @Test
    public void testValidParameter() throws Exception {
        List<String> warnLines = performTest("valid-parameter");
        assertTrue(warnLines.isEmpty(), "Unwanted warnings: " + warnLines);
    }

    /**
     * Test that ensures that warning is not printed for valid parameters
     */
    @Test
    public void testValidParameterAlias() throws Exception {
        List<String> warnLines = performTest("valid-parameter-alias");
        assertTrue(warnLines.isEmpty(), "Unwanted warnings: " + warnLines);
    }

    /**
     * Test that ensures that warning is not printed for valid parameters
     */
    @Test
    public void testValidParameterForOtherGoal() throws Exception {
        List<String> warnLines = performTest("valid-parameter-other-goal");
        assertTrue(warnLines.isEmpty(), "Unwanted warnings: " + warnLines);
    }

    /**
     * Test that ensures that warning is printed for configuration
     */
    @Test
    public void testInBuildPlugin() throws Exception {
        List<String> warnLines = performTest("config-build-plugin");
        assertWarningContains(warnLines);
    }

    /**
     * Test that ensures that warning is printed for configuration
     */
    @Test
    public void testInBuildExecution() throws Exception {
        List<String> warnLines = performTest("config-build-execution");
        assertWarningContains(warnLines);
    }

    /**
     * Test that ensures that warning is printed for configuration
     */
    @Test
    public void testInBuildMixed() throws Exception {
        List<String> warnLines = performTest("config-build-mixed");
        assertWarningContains(warnLines);
    }

    /**
     * Test that ensures that warning is printed for configuration
     */
    @Test
    public void testInPluginManagement() throws Exception {
        List<String> warnLines = performTest("config-plugin-management");
        assertWarningContains(warnLines);
    }

    /**
     * Test that ensures that warning is printed for configuration
     */
    @Test
    public void testInPluginManagementParent() throws Exception {
        List<String> warnLines = performTest("config-plugin-management-parent");
        assertWarningContains(warnLines);
    }

    /**
     * Test that ensures that warning is printed for configuration
     */
    @Test
    public void testWithForkedGoalExecution() throws Exception {
        List<String> warnLines = performTest("config-with-fork-goal");

        assertTrue(
                warnLines.remove(
                        "[WARNING] Parameter 'invalidXml' is unknown for plugin 'maven-it-plugin-fork:2.1-SNAPSHOT:fork-goal (fork)'"));

        assertTrue(
                warnLines.remove(
                        "[WARNING] Parameter 'invalidParam' is unknown for plugin 'maven-it-plugin-fork:2.1-SNAPSHOT:fork-goal (fork)'"));

        assertTrue(
                warnLines.remove(
                        "[WARNING] Parameter 'invalidXml' is unknown for plugin 'maven-it-plugin-fork:2.1-SNAPSHOT:touch (touch)'"));

        assertTrue(
                warnLines.remove(
                        "[WARNING] Parameter 'invalidParam' is unknown for plugin 'maven-it-plugin-fork:2.1-SNAPSHOT:touch (touch)'"));

        assertTrue(warnLines.isEmpty(), "Not verified line: " + warnLines);
    }

    private List<String> performTest(String project) throws Exception {
        File testDir = extractResources("/mng-7468-unsupported-params");

        Verifier verifier = newVerifier(new File(testDir, project).getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLogLines();
        return findUnknownWarning(logLines);
    }

    private void assertWarningContains(List<String> warnLines) {
        assertTrue(
                warnLines.remove(
                        "[WARNING] Parameter 'invalidParam' is unknown for plugin 'maven-it-plugin-configuration:2.1-SNAPSHOT:touch (default)'"));

        assertTrue(
                warnLines.remove(
                        "[WARNING] Parameter 'invalidXml' is unknown for plugin 'maven-it-plugin-configuration:2.1-SNAPSHOT:touch (default)'"));

        assertTrue(warnLines.isEmpty(), "Not verified line: " + warnLines);
    }

    private List<String> findUnknownWarning(List<String> logLines) {
        Pattern pattern = Pattern.compile("\\[WARNING] Parameter .* is unknown.*");
        List<String> result = new ArrayList<>();
        for (String line : logLines) {
            if (pattern.matcher(line).matches()) {
                result.add(line);
            }
        }

        return result;
    }
}
