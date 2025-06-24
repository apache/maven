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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8598">MNG-8598</a>:
 * Verify that ${MAVEN_PROJECTBASEDIR} and $MAVEN_PROJECTBASEDIR in .mvn/jvm.config are properly
 * substituted with the actual project base directory.
 */
public class MavenITmng8598JvmConfigSubstitutionTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng8598JvmConfigSubstitutionTest() {
        super("[4.0.0-rc-4,)");
    }

    @Test
    public void testProjectBasedirSubstitution() throws Exception {
        File testDir = extractResources("/mng-8598");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument(
                "-Dexpression.outputFile=" + new File(testDir, "target/pom.properties").getAbsolutePath());
        verifier.setForkJvm(true); // custom .mvn/jvm.config
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        String expectedPath = testDir.getAbsolutePath().replace('\\', '/');
        assertEquals(
                expectedPath + "/curated",
                props.getProperty("project.properties.curatedPathProp").replace('\\', '/'));
        assertEquals(
                expectedPath + "/simple",
                props.getProperty("project.properties.simplePathProp").replace('\\', '/'));
    }
}
