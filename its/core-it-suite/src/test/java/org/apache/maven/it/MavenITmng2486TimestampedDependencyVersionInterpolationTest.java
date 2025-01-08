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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2486">MNG-2486</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2486TimestampedDependencyVersionInterpolationTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2486TimestampedDependencyVersionInterpolationTest() {
        super("[2.0.5,)");
    }

    /**
     * Verify that the expression ${project.version} gets resolved to X-SNAPSHOT and not the actual timestamp
     * during transitive dependency resolution. In part, this depends on the deployed SNAPSHOT POMs to retain their
     * X-SNAPSHOT project version and not having it replaced with the timestamp version.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-2486");

        Verifier verifier;

        verifier = newVerifier(new File(testDir, "dep-a").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng2486", null);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "parent").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "dep-b").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "test").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        // enforce remote resolution
        verifier.deleteArtifacts("org.apache.maven.its.mng2486", null);
        verifier.deleteArtifacts("org.apache.maven.its.mng2486", "maven-core-it");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> files = verifier.loadLines("target/classpath.txt");
        assertTrue(files.contains("dep-a-0.1-SNAPSHOT.jar"), files.toString());
    }
}
