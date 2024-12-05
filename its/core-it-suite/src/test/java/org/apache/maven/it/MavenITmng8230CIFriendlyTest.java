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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8230">MNG-8230</a>.
 */
class MavenITmng8230CIFriendlyTest extends AbstractMavenIntegrationTestCase {

    private static final String PROPERTIES = "target/expression.properties";

    MavenITmng8230CIFriendlyTest() {
        super("[4.0.0-beta-5,)");
    }

    /**
     *  Verify that CI friendly version work when using project properties
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitCiFriendlyWithProjectProperties() throws Exception {
        File testDir = extractResources("/mng-8230-ci-friendly-and-gav");

        File basedir = new File(testDir, "cif-with-project-props");
        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.addCliArgument("-Dexpression.outputFile=" + new File(basedir, PROPERTIES).getPath());
        verifier.addCliArgument("-Dexpression.expressions=project/version");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent(PROPERTIES);
        Properties props = verifier.loadProperties(PROPERTIES);
        assertEquals(props.getProperty("project.version"), "1.0-SNAPSHOT");
    }

    /**
     *  Verify that CI friendly version work when using project properties
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitCiFriendlyWithProjectPropertiesOverride() throws Exception {
        File testDir = extractResources("/mng-8230-ci-friendly-and-gav");

        File basedir = new File(testDir, "cif-with-project-props");
        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.addCliArgument("-Dexpression.outputFile=" + new File(basedir, PROPERTIES).getPath());
        verifier.addCliArgument("-Dexpression.expressions=project/version");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        verifier.addCliArgument("-Dci-version=1.1-SNAPSHOT");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent(PROPERTIES);
        Properties props = verifier.loadProperties(PROPERTIES);
        assertEquals(props.getProperty("project.version"), "1.1-SNAPSHOT");
    }

    /**
     *  Verify that CI friendly version work when using user properties
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitCiFriendlyWithUserProperties() throws Exception {
        File testDir = extractResources("/mng-8230-ci-friendly-and-gav");

        File basedir = new File(testDir, "cif-with-user-props");
        Verifier verifier = newVerifier(basedir.getAbsolutePath());

        verifier.addCliArgument("-Dexpression.outputFile=" + new File(basedir, PROPERTIES).getPath());
        verifier.addCliArgument("-Dexpression.expressions=project/version");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        verifier.addCliArgument("-Dci-version=1.1-SNAPSHOT");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent(PROPERTIES);
        Properties props = verifier.loadProperties(PROPERTIES);
        assertEquals(props.getProperty("project.version"), "1.1-SNAPSHOT");
    }

    /**
     *  Verify that CI friendly version fails if the properties are not given
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitCiFriendlyWithUserPropertiesNotGiven() throws Exception {
        File testDir = extractResources("/mng-8230-ci-friendly-and-gav");

        File basedir = new File(testDir, "cif-with-user-props");
        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.addCliArgument("-Dexpression.outputFile=" + new File(basedir, PROPERTIES).getPath());
        verifier.addCliArgument("-Dexpression.expressions=project/version");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        try {
            verifier.execute();
            fail("Expected failure");
        } catch (VerificationException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "'version' contains an expression but should be a constant. @ myGroup:parent:${ci-version}"),
                    e.getMessage());
        }
    }

    @Test
    void testitExpressionInGroupId() throws Exception {
        File testDir = extractResources("/mng-8230-ci-friendly-and-gav");

        File basedir = new File(testDir, "exp-in-groupid");
        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.addCliArgument("-Dexpression.outputFile=" + new File(basedir, PROPERTIES).getPath());
        verifier.addCliArgument("-Dexpression.expressions=project/version");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        try {
            verifier.execute();
            fail("Expected failure");
        } catch (VerificationException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "'groupId' contains an expression but should be a constant. @ ${foo}:myArtifact:1.0-SNAPSHOT"),
                    e.getMessage());
        }
    }

    @Test
    void testitExpressionInArtifactId() throws Exception {
        File testDir = extractResources("/mng-8230-ci-friendly-and-gav");

        File basedir = new File(testDir, "exp-in-artifactid");
        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.addCliArgument("-Dexpression.outputFile=" + new File(basedir, PROPERTIES).getPath());
        verifier.addCliArgument("-Dexpression.expressions=project/version");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        try {
            verifier.execute();
            fail("Expected failure");
        } catch (VerificationException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "'artifactId' contains an expression but should be a constant. @ myGroup:${foo}:1.0-SNAPSHOT"),
                    e.getMessage());
        }
    }
}
