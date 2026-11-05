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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/4637">MNG-3309</a>.
 * Verifies that cascading profile activation works: a profile's properties can trigger
 * the activation of subsequent profiles within the same POM.
 */
class MavenITmng3309CascadingProfileActivationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a three-level cascade (trigger → step1 → step2) activates all
     * profiles in the chain and that an unrelated profile stays inactive.
     */
    @Test
    void testCascadingActivation() throws Exception {
        Path testDir = extractResources("mng-3309");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dstart.cascade=true");
        verifier.addCliArgument("-Dexpression.outputFile=" + testDir.resolve("target/expression.properties"));
        verifier.addCliArgument("-Dexpression.expressions=project/properties");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/expression.properties");
        assertEquals("activated", props.getProperty("project.properties.cascade.step1"));
        assertEquals("activated", props.getProperty("project.properties.cascade.step2"));
        assertEquals("reached", props.getProperty("project.properties.cascade.final"));
        assertNull(props.getProperty("project.properties.cascade.unrelated.result"));
    }

    /**
     * Verify that without the trigger property, no cascading profiles activate.
     */
    @Test
    void testNoCascadeWithoutTrigger() throws Exception {
        Path testDir = extractResources("mng-3309");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dexpression.outputFile=" + testDir.resolve("target/expression.properties"));
        verifier.addCliArgument("-Dexpression.expressions=project/properties");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/expression.properties");
        assertNull(props.getProperty("project.properties.cascade.step1"));
        assertNull(props.getProperty("project.properties.cascade.step2"));
        assertNull(props.getProperty("project.properties.cascade.final"));
        assertNull(props.getProperty("project.properties.cascade.unrelated.result"));
    }
}
