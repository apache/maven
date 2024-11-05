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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for
 * <a href="https://issues.apache.org/jira/browse/MNG-7939">MNG-7939</a>.
 * Allow to exclude plugins from validation
 */
class MavenITmng7939PluginsValidationExcludesTest extends AbstractMavenIntegrationTestCase {

    protected MavenITmng7939PluginsValidationExcludesTest() {
        super("[4.0.0-alpha-9,)");
    }

    @Test
    void warningForPluginValidationIsPresentInProject() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7939-plugins-validation-excludes");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setLogFileName("with-warning-log.txt");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dmaven.plugin.validation=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logs = verifier.loadLines(verifier.getLogFileName(), null);

        verifyTextInLog(logs, "[INFO] [MAVEN-CORE-IT-LOG] localRepository");
        verifyTextInLog(logs, "[WARNING]  * org.apache.maven.its.plugins:maven-it-plugin-configuration:2.1-SNAPSHOT");
        verifyTextInLog(
                logs, "[WARNING] Plugin [INTERNAL, EXTERNAL] validation issues were detected in following plugin(s)");
        verifyTextInLog(
                logs, "[WARNING]    * Mojo itconfiguration:localRepo (org.apache.maven.plugin.coreit.LocalRepoMojo)");
        verifyTextInLog(
                logs,
                "[WARNING]      - Parameter 'localRepository' uses deprecated parameter expression '${localRepository}'");
    }

    @Test
    void excludePluginFromValidation() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7939-plugins-validation-excludes");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setLogFileName("without-warning-log.txt");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dmaven.plugin.validation=verbose");
        verifier.addCliArgument(
                "-Dmaven.plugin.validation.excludes=org.apache.maven.its.plugins:maven-it-plugin-configuration:2.1-SNAPSHOT");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logs = verifier.loadLines(verifier.getLogFileName(), null);

        verifyTextInLog(logs, "[INFO] [MAVEN-CORE-IT-LOG] localRepository");
        verifyTextNotInLog(
                logs, "[WARNING]  * org.apache.maven.its.plugins:maven-it-plugin-configuration:2.1-SNAPSHOT");
        verifyTextNotInLog(
                logs, "[WARNING] Plugin [INTERNAL, EXTERNAL] validation issues were detected in following plugin(s)");
        verifyTextNotInLog(
                logs, "[WARNING]    * Mojo itconfiguration:localRepo (org.apache.maven.plugin.coreit.LocalRepoMojo)");
        verifyTextNotInLog(
                logs,
                "[WARNING]      - Parameter 'localRepository' uses deprecated parameter expression '${localRepository}'");
    }

    private void verifyTextInLog(List<String> logs, String text) {
        assertTrue("Log file not contains: " + text, logs.stream().anyMatch(l -> l.contains(text)));
    }

    private void verifyTextNotInLog(List<String> logs, String text) {
        assertFalse("Log file contains: " + text, logs.stream().anyMatch(l -> l.contains(text)));
    }
}
