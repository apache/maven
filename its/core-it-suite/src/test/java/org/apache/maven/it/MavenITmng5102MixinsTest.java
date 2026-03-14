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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5135">MNG-5135</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng5102MixinsTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that mixins can be loaded from the file system.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testWithPath() throws Exception {
        File testDir = extractResources("/mng-5102-mixins/path");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng5102");
        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/model.properties");
        Properties props = verifier.loadProperties("target/model.properties");
        assertEquals("true", props.getProperty("project.properties.mixin1"));
        assertNull(props.getProperty("project.properties.mixin3"));

        verifier.verifyFilePresent("child/target/model.properties");
        props = verifier.loadProperties("child/target/model.properties");
        assertEquals("true", props.getProperty("project.properties.mixin1"));
        assertEquals("true", props.getProperty("project.properties.mixin3"));

        verifier.verifyFilePresent(
                "target/project-local-repo/org.apache.maven.its.mng5102/child/0.1/child-0.1-consumer.pom");
        List<String> lines = verifier.loadLines(
                "target/project-local-repo/org.apache.maven.its.mng5102/child/0.1/child-0.1-consumer.pom");
        assertTrue(lines.stream().noneMatch(l -> l.contains("<mixin>")));
    }

    /**
     * Verify that mixins can be loaded from the repositories.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testWithGav() throws Exception {
        File testDir = extractResources("/mng-5102-mixins/gav");

        Verifier verifier = newVerifier(new File(testDir, "mixin-2").getAbsolutePath());

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng5102");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "project").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/model.properties");
        Properties props = verifier.loadProperties("target/model.properties");
        assertEquals("true", props.getProperty("project.properties.mixin2"));

        verifier.verifyFilePresent(
                "target/project-local-repo/org.apache.maven.its.mng5102/gav/0.1/gav-0.1-consumer.pom");
        List<String> lines = verifier.loadLines(
                "target/project-local-repo/org.apache.maven.its.mng5102/gav/0.1/gav-0.1-consumer.pom");
        assertTrue(lines.stream().anyMatch(l -> l.contains("<mixin>")));
    }

    /**
     * Verify that mixins can be loaded from the repositories with a classifier.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testWithClassifier() throws Exception {
        File testDir = extractResources("/mng-5102-mixins/classifier");

        Verifier verifier = newVerifier(new File(testDir, "mixin-4").getAbsolutePath());

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng5102");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "project").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/model.properties");
        Properties props = verifier.loadProperties("target/model.properties");
        assertEquals("true", props.getProperty("project.properties.mixin4"));

        verifier.verifyFilePresent(
                "target/project-local-repo/org.apache.maven.its.mng5102/classifier/0.1/classifier-0.1-consumer.pom");
        List<String> lines = verifier.loadLines(
                "target/project-local-repo/org.apache.maven.its.mng5102/classifier/0.1/classifier-0.1-consumer.pom");
        assertTrue(lines.stream().anyMatch(l -> l.contains("<mixin>")));
    }
}
