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

public class MavenITmng6562WarnDefaultBindings extends AbstractMavenIntegrationTestCase {

    public MavenITmng6562WarnDefaultBindings() {
        super("[4.0.0-alpha-1,)");
    }

    @Test
    public void testItShouldNotWarn() throws Exception {
        File testDir = extractResources("/mng-6562-default-bindings");

        String phase = "validate";
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);
        verifier.setLogFileName(phase + ".txt");
        verifier.addCliArgument("-fos");
        verifier.addCliArgument("WARN"); // ALSO NO WARNINGS
        verifier.addCliArgument(phase);
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testItShouldNotWarn2() throws Exception {
        File testDir = extractResources("/mng-6562-default-bindings");

        String phase = "process-resources";
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);
        verifier.setLogFileName(phase + ".txt");
        verifier.addCliArgument("-fos");
        verifier.addCliArgument("WARN"); // ALSO NO WARNINGS
        verifier.addCliArgument(phase);
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testItShouldWarnForCompilerPlugin() throws Exception {
        File testDir = extractResources("/mng-6562-default-bindings");

        String phase = "compile";
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);
        verifier.setLogFileName(phase + ".txt");
        verifier.addCliArgument(phase);
        verifier.execute();

        verifier.verifyTextInLog("Version not locked for default bindings plugins [maven-compiler-plugin]"
                + ", you should define versions in pluginManagement section of your pom.xml or parent");
    }

    @Test
    public void testItShouldWarnForCompilerPlugin2() throws Exception {
        File testDir = extractResources("/mng-6562-default-bindings");

        String phase = "process-test-resources";
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);
        verifier.setLogFileName(phase + ".txt");
        verifier.addCliArgument(phase);
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("Version not locked for default bindings plugins [maven-compiler-plugin]"
                + ", you should define versions in pluginManagement section of your pom.xml or parent");
    }

    @Test
    public void testItShouldWarnForCompilerPlugin3() throws Exception {
        File testDir = extractResources("/mng-6562-default-bindings");

        String phase = "test-compile";
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);
        verifier.setLogFileName(phase + ".txt");
        verifier.addCliArgument(phase);
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("Version not locked for default bindings plugins [maven-compiler-plugin]"
                + ", you should define versions in pluginManagement section of your pom.xml or parent");
    }

    @Test
    public void testItShouldWarnForCompilerPluginAndSurefirePlugin() throws Exception {
        File testDir = extractResources("/mng-6562-default-bindings");

        String phase = "test";
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);
        verifier.setLogFileName(phase + ".txt");
        verifier.addCliArgument(phase);
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog(
                "Version not locked for default bindings plugins [maven-compiler-plugin, maven-surefire-plugin]"
                        + ", you should define versions in pluginManagement section of your pom.xml or parent");
    }
}
