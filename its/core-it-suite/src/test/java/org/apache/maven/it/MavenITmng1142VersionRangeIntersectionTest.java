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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1142">MNG-1142</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1142VersionRangeIntersectionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng1142VersionRangeIntersectionTest() {
        super("[2.0.7,3.0-alpha-1),[3.0,)");
    }

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
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-1142");

        Verifier verifier = newVerifier(new File(testDir, project).getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng1142");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt", "UTF-8");

        assertFalse(classpath.toString(), classpath.contains("a-1.1.2.jar"));
        assertTrue(classpath.toString(), classpath.contains("a-1.1.1.jar"));
        assertTrue(classpath.toString(), classpath.contains("b-0.1.jar"));
    }
}
