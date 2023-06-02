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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

public class MavenITmng7038RootdirTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng7038RootdirTest() {
        super("[4.0.0-alpha-6,)");
    }

    @Test
    public void testRootdir() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7038-rootdir");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props;

        verifier.verifyFilePresent("target/pom.properties");
        props = verifier.loadProperties("target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                testDir.getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals("project.rootDirectory", testDir.getAbsolutePath(), props.getProperty("project.rootDirectory"));
        assertEquals("session.topDirectory", testDir.getAbsolutePath(), props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.TRUE.toString(),
                props.getProperty("project.properties.activated"));

        verifier.verifyFilePresent("module-a/target/pom.properties");
        props = verifier.loadProperties("module-a/target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals(
                "project.rootDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.rootDirectory"));
        assertEquals("session.topDirectory", testDir.getAbsolutePath(), props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.FALSE.toString(),
                props.getProperty("project.properties.activated"));

        verifier.verifyFilePresent("module-a/module-a-1/target/pom.properties");
        props = verifier.loadProperties("module-a/module-a-1/target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals(
                "project.rootDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.rootDirectory"));
        assertEquals("session.topDirectory", testDir.getAbsolutePath(), props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.FALSE.toString(),
                props.getProperty("project.properties.activated"));

        verifier.verifyFilePresent("module-b/target/pom.properties");
        props = verifier.loadProperties("module-b/target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                testDir.getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals("project.rootDirectory", testDir.getAbsolutePath(), props.getProperty("project.rootDirectory"));
        assertEquals("session.topDirectory", testDir.getAbsolutePath(), props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.TRUE.toString(),
                props.getProperty("project.properties.activated"));

        verifier.verifyFilePresent("module-b/module-b-1/target/pom.properties");
        props = verifier.loadProperties("module-b/module-b-1/target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                testDir.getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals("project.rootDirectory", testDir.getAbsolutePath(), props.getProperty("project.rootDirectory"));
        assertEquals("session.topDirectory", testDir.getAbsolutePath(), props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.TRUE.toString(),
                props.getProperty("project.properties.activated"));
    }

    @Test
    public void testRootdirWithTopdirAndRoot() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7038-rootdir");
        Verifier verifier = newVerifier(new File(testDir, "module-a").getAbsolutePath());

        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props;

        verifier.verifyFilePresent("target/pom.properties");
        props = verifier.loadProperties("target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals(
                "project.rootDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.rootDirectory"));
        assertEquals(
                "session.topDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("session.topDirectory"));
        assertEquals(
                "session.rootDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.FALSE.toString(),
                props.getProperty("project.properties.activated"));

        verifier.verifyFilePresent("module-a-1/target/pom.properties");
        props = verifier.loadProperties("module-a-1/target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals(
                "project.rootDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("project.rootDirectory"));
        assertEquals(
                "session.topDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("session.topDirectory"));
        assertEquals(
                "session.rootDirectory",
                new File(testDir, "module-a").getAbsolutePath(),
                props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.FALSE.toString(),
                props.getProperty("project.properties.activated"));
    }

    @Test
    public void testRootdirWithTopdirAndNoRoot() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7038-rootdir");
        Verifier verifier = newVerifier(new File(testDir, "module-b").getAbsolutePath());

        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props;

        verifier.verifyFilePresent("target/pom.properties");
        props = verifier.loadProperties("target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                testDir.getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals("project.rootDirectory", testDir.getAbsolutePath(), props.getProperty("project.rootDirectory"));
        assertEquals(
                "session.topDirectory",
                new File(testDir, "module-b").getAbsolutePath(),
                props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.TRUE.toString(),
                props.getProperty("project.properties.activated"));

        verifier.verifyFilePresent("module-b-1/target/pom.properties");
        props = verifier.loadProperties("module-b-1/target/pom.properties");
        assertEquals(
                "project.properties.rootdir",
                testDir.getAbsolutePath(),
                props.getProperty("project.properties.rootdir"));
        assertEquals("project.rootDirectory", testDir.getAbsolutePath(), props.getProperty("project.rootDirectory"));
        assertEquals(
                "session.topDirectory",
                new File(testDir, "module-b").getAbsolutePath(),
                props.getProperty("session.topDirectory"));
        assertEquals("session.rootDirectory", testDir.getAbsolutePath(), props.getProperty("session.rootDirectory"));
        assertEquals(
                "project.properties.activated",
                Boolean.TRUE.toString(),
                props.getProperty("project.properties.activated"));
    }
}
