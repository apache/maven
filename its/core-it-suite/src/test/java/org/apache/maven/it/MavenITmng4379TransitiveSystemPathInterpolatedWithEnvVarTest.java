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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4379">MNG-4379</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4379TransitiveSystemPathInterpolatedWithEnvVarTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4379TransitiveSystemPathInterpolatedWithEnvVarTest() {
        super("[2.0.3,2.1.0),[3.0-alpha-6,)");
    }

    /**
     * Test that the path of a system-scope dependency gets interpolated using environment variables during
     * transitive dependency resolution.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4379");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4379", "maven-core-it");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.setEnvironmentVariable("MNG_4379_HOME", testDir.getAbsolutePath());
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArguments("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt");
        assertTrue(classpath.contains("pom.xml"), classpath.toString());
    }
}
