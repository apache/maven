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
import java.util.Collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2921">MNG-2921</a>. It naturally includes the
 * test for the related issue <a href="https://issues.apache.org/jira/browse/MNG-2877">MNG-2877</a> whose original test was
 * too weak to prevent this issue.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2921ActiveAttachedArtifactsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2921ActiveAttachedArtifactsTest() {
        super("(2.0.6,)");
    }

    /**
     * Verify that attached project artifacts can be resolved from the reactor as active project artifacts for
     * consumption on other module's class paths. Note the subtle difference of this test compared to the closely
     * related issue MNG-2871: This test is about *attached* artifacts, i.e. dependencies that have already been
     * packaged. MNG-2871 on the other hand is about dependencies that haven't been packaged yet but merely exist
     * as loose class files in a module's output directory. In other words, this test is concerned with the situation
     * during the lifecycle phase "package" while MNG-2871 is concerned with earlier phases like "test".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2921() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-2921");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("consumer/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Collection<String> compileArtifacts = verifier.loadLines("consumer/target/compile.txt");
        assertTrue(
                compileArtifacts.contains("org.apache.maven.its.mng2921:ejbs:ejb-client:client:1.0-SNAPSHOT"),
                compileArtifacts.toString());
        assertTrue(
                compileArtifacts.contains("org.apache.maven.its.mng2921:producer:ejb-client:client:1.0-SNAPSHOT"),
                compileArtifacts.toString());
        assertFalse(
                compileArtifacts.contains("org.apache.maven.its.mng2921:tests:test-jar:tests:1.0-SNAPSHOT"),
                compileArtifacts.toString());
        assertFalse(
                compileArtifacts.contains("org.apache.maven.its.mng2921:producer:test-jar:tests:1.0-SNAPSHOT"),
                compileArtifacts.toString());

        Collection<String> testArtifacts = verifier.loadLines("consumer/target/test.txt");
        assertTrue(
                testArtifacts.contains("org.apache.maven.its.mng2921:ejbs:ejb-client:client:1.0-SNAPSHOT"),
                testArtifacts.toString());
        assertTrue(
                testArtifacts.contains("org.apache.maven.its.mng2921:producer:ejb-client:client:1.0-SNAPSHOT"),
                testArtifacts.toString());
        assertTrue(
                testArtifacts.contains("org.apache.maven.its.mng2921:tests:test-jar:tests:1.0-SNAPSHOT"),
                testArtifacts.toString());
        assertTrue(
                testArtifacts.contains("org.apache.maven.its.mng2921:producer:test-jar:tests:1.0-SNAPSHOT"),
                testArtifacts.toString());

        Collection<String> testClassPath = verifier.loadLines("consumer/target/test-classpath.txt");
        assertTrue(testClassPath.contains("ejbs/attached.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("tests/attached.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("producer/client.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("producer/tests.jar"), testClassPath.toString());
    }
}
