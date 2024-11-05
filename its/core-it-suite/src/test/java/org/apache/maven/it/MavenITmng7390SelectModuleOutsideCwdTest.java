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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test suite tests whether other modules in the same multi-module project can be selected when invoking Maven from a submodule.
 * Related JIRA issue: <a href="https://issues.apache.org/jira/browse/MNG-7390">MNG-7390</a>.
 *
 * @author Martin Kanters
 */
public class MavenITmng7390SelectModuleOutsideCwdTest extends AbstractMavenIntegrationTestCase {

    private File moduleADir;

    public MavenITmng7390SelectModuleOutsideCwdTest() {
        super("[4.0.0-alpha-1,)");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        moduleADir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7390-pl-outside-cwd/module-a");

        // Clean up target files from earlier runs (verifier.setAutoClean does not work, as we are reducing the reactor)
        final Verifier verifier = newVerifier(moduleADir.getAbsolutePath());
        verifier.addCliArgument("-f");
        verifier.addCliArgument("..");
        verifier.addCliArgument("clean");
        verifier.execute();
    }

    @Test
    public void testSelectModuleByCoordinate() throws Exception {
        final Verifier verifier = newVerifier(moduleADir.getAbsolutePath());

        verifier.addCliArgument("-pl");
        verifier.addCliArgument(":module-b");
        verifier.setLogFileName("log-module-by-coordinate.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyFileNotPresent("target/touch.txt");
        verifier.verifyFileNotPresent("../target/touch.txt");
        verifier.verifyFilePresent("../module-b/target/touch.txt");
    }

    @Test
    public void testSelectMultipleModulesByCoordinate() throws Exception {
        final Verifier verifier = newVerifier(moduleADir.getAbsolutePath());

        verifier.addCliArgument("-pl");
        verifier.addCliArgument(":module-b,:module-a");
        verifier.setLogFileName("log-modules-by-coordinate.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyFilePresent("target/touch.txt");
        verifier.verifyFileNotPresent("../target/touch.txt");
        verifier.verifyFilePresent("../module-b/target/touch.txt");
    }

    @Test
    public void testSelectModuleByRelativePath() throws Exception {
        final Verifier verifier = newVerifier(moduleADir.getAbsolutePath());

        verifier.addCliArgument("-pl");
        verifier.addCliArgument("../module-b");
        verifier.setLogFileName("log-module-by-relative-path.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyFileNotPresent("target/touch.txt");
        verifier.verifyFileNotPresent("../target/touch.txt");
        verifier.verifyFilePresent("../module-b/target/touch.txt");
    }

    @Test
    public void testSelectModulesByRelativePath() throws Exception {
        final Verifier verifier = newVerifier(moduleADir.getAbsolutePath());

        verifier.addCliArgument("-pl");
        verifier.addCliArgument("../module-b,.");
        verifier.setLogFileName("log-modules-by-relative-path.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyFilePresent("target/touch.txt");
        verifier.verifyFileNotPresent("../target/touch.txt");
        verifier.verifyFilePresent("../module-b/target/touch.txt");
    }

    /**
     * Maven determines whether the target module is in a multi-module project based on the presence of a .mvn directory in root.
     * This test verifies that when that directory is missing, the module cannot be found.
     */
    @Test
    public void testSelectModulesOutsideCwdDoesNotWorkWhenDotMvnIsNotPresent() throws Exception {
        final String noDotMvnPath = "/mng-7390-pl-outside-cwd-no-dotmvn/module-a";
        final File noDotMvnDir = ResourceExtractor.simpleExtractResources(getClass(), noDotMvnPath);
        final Verifier verifier = newVerifier(noDotMvnDir.getAbsolutePath());

        verifier.addCliArgument("-pl");
        verifier.addCliArgument("../module-b");
        verifier.setLogFileName("log-modules-by-relative-path-no-dotmvn.txt");
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            fail("Expected goal to fail");
        } catch (VerificationException e) {
            verifier.verifyFileNotPresent("target/touch.txt");
            verifier.verifyFileNotPresent("../target/touch.txt");
            verifier.verifyFileNotPresent("../module-b/target/touch.txt");
        }
    }
}
