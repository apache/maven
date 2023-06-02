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
import java.util.Properties;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3877">MNG-3877</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3877BasedirAlignedModelTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3877BasedirAlignedModelTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that project directories are basedir aligned when inspected by plugins via the MavenProject instance.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3877() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3877");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties modelProps = verifier.loadProperties("target/model.properties");

        assertPathEquals(testDir, "target", modelProps.getProperty("project.build.directory"));

        assertPathEquals(testDir, "target/classes", modelProps.getProperty("project.build.outputDirectory"));

        assertPathEquals(testDir, "target/test-classes", modelProps.getProperty("project.build.testOutputDirectory"));

        assertPathEquals(testDir, "src/main/java", modelProps.getProperty("project.build.sourceDirectory"));
        assertPathEquals(testDir, "src/main/java", modelProps.getProperty("project.compileSourceRoots.0"));

        assertPathEquals(testDir, "src/test/java", modelProps.getProperty("project.build.testSourceDirectory"));
        assertPathEquals(testDir, "src/test/java", modelProps.getProperty("project.testCompileSourceRoots.0"));

        assertPathEquals(testDir, "src/main/resources", modelProps.getProperty("project.build.resources.0.directory"));

        assertPathEquals(
                testDir, "src/test/resources", modelProps.getProperty("project.build.testResources.0.directory"));

        assertPathEquals(testDir, "src/main/filters/it.properties", modelProps.getProperty("project.build.filters.0"));

        /*
         * NOTE: The script source directory is deliberately excluded from the checks due to MNG-3741.
         */

        // MNG-3877
        if (matchesVersionRange("[3.0-alpha-3,)")) {
            assertPathEquals(testDir, "target/site", modelProps.getProperty("project.reporting.outputDirectory"));
        }
    }

    private void assertPathEquals(File basedir, String expected, String actual) throws IOException {
        File actualFile = new File(actual);
        assertTrue("path not absolute: " + actualFile, actualFile.isAbsolute());
        assertCanonicalFileEquals(new File(basedir, expected), actualFile);
    }
}
