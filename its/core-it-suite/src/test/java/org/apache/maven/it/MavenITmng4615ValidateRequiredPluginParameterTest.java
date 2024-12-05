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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4615">MNG-4615</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4615ValidateRequiredPluginParameterTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4615ValidateRequiredPluginParameterTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-2,)");
    }

    /**
     * Verify that Maven validates required mojo parameters (and doesn't just have the plugins die with NPEs).
     * This scenario checks the case of all required parameters being set via plugin configuration.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitAllSet() throws Exception {
        File testDir = extractResources("/mng-4615/test-0");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        assertEquals("one", props.get("requiredParam"));
        assertEquals("two", props.get("requiredParamWithDefault"));
    }

    /**
     * Verify that Maven validates required mojo parameters (and doesn't just have the plugins die with NPEs).
     * This scenario checks the case of a parameter missing its backing system property.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitExprMissing() throws Exception {
        File testDir = extractResources("/mng-4615/test-1");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-a.txt");
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail("Build did not fail despite required plugin parameter missing");
        } catch (VerificationException e) {
            // expected
        }
    }

    /**
     * Verify that Maven validates required mojo parameters (and doesn't just have the plugins die with NPEs).
     * This scenario checks the case of a parameter having its backing user property set.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitExprSet() throws Exception {
        File testDir = extractResources("/mng-4615/test-1");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dconfig.requiredParam=CLI");
        verifier.setLogFileName("log-b.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        assertEquals("CLI", props.get("requiredParam"));
        assertEquals("two", props.get("requiredParamWithDefault"));
    }

    /**
     * Verify that Maven validates required mojo parameters (and doesn't just have the plugins die with NPEs).
     * This scenario checks the case of a parameter missing its backing POM value.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPomValMissing() throws Exception {
        // cf. MNG-4764
        requiresMavenVersion("[3.0-beta-2,)");

        File testDir = extractResources("/mng-4615/test-2a");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail("Build did not fail despite required plugin parameter missing");
        } catch (VerificationException e) {
            // expected
        }
    }

    /**
     * Verify that Maven validates required mojo parameters (and doesn't just have the plugins die with NPEs).
     * This scenario checks the case of a parameter having its backing POM value set.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPomValSet() throws Exception {
        File testDir = extractResources("/mng-4615/test-2b");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        assertEquals("one", props.get("requiredParam"));
        assertEquals("http://foo.bar/", props.get("requiredParamWithDefault"));
    }
}
