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

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for <a href="https://issues.apache.org/jira/browse/MNG-1378">MNG-1378</a>.
 */
public class MavenITmng1378TestJarTransitiveDependenciesTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testTestJarCarriesProducerTestDependencies() throws Exception {
        Path testDir = extractResources("mng-1378");

        Verifier producer = newVerifier(testDir);
        producer.setAutoclean(false);
        producer.deleteArtifacts("org.apache.maven.its.mng1378");
        producer.addCliArgument("-pl");
        producer.addCliArgument("support,test-jar");
        producer.addCliArgument("validate");
        producer.execute();
        producer.verifyErrorFreeLog();

        Verifier consumer = newVerifier(testDir.resolve("consumer"));
        consumer.setAutoclean(false);
        consumer.addCliArgument("validate");
        consumer.execute();
        consumer.verifyErrorFreeLog();

        List<String> testClasspath = consumer.loadLines("target/test.txt");
        assertTrue(testClasspath.contains("test-jar-1.0-tests.jar"), testClasspath.toString());
        assertTrue(testClasspath.contains("support-1.0.jar"), testClasspath.toString());

        Verifier regularConsumer = newVerifier(testDir.resolve("regular-consumer"));
        regularConsumer.setAutoclean(false);
        regularConsumer.addCliArgument("validate");
        regularConsumer.execute();
        regularConsumer.verifyErrorFreeLog();

        List<String> regularTestClasspath = regularConsumer.loadLines("target/test.txt");
        assertTrue(regularTestClasspath.contains("test-jar-1.0.jar"), regularTestClasspath.toString());
        assertFalse(regularTestClasspath.contains("support-1.0.jar"), regularTestClasspath.toString());
    }
}
