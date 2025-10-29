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
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1142">MNG-1142</a>.
 *
 * @author Benjamin Bentmann
 * @since 2.0.7
 */
public class MavenITmng1142VersionRangeIntersectionTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that user properties from the CLI do not override POM properties of transitive dependencies.
     * This variant checks dependency order a, b.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitAB() throws Exception {
        testit("test-ab");
    }

    /**
     * Verify that combination of two version ranges doesn't erroneously select an exclusive upper bound.
     * This variant checks dependency order b, a.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitBA() throws Exception {
        testit("test-ba");
    }

    private void testit(String project) throws Exception {
        Path testDir = extractResourcesAsPath("/mng-1142");

        Verifier verifier = newVerifier(testDir.resolve(project).getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng1142");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt");

        assertFalse(classpath.contains("a-1.1.2.jar"), classpath.toString());
        assertTrue(classpath.contains("a-1.1.1.jar"), classpath.toString());
        assertTrue(classpath.contains("b-0.1.jar"), classpath.toString());
    }
}
