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

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/12507">gh-12507</a>:
 * {@code ${...}} expressions inside CLI-supplied plugin parameter values must be resolved
 * against the session/project context (Maven 3 semantics), not silently replaced with the
 * empty string. The real-world trigger is
 * {@code -DaltDeploymentRepository=id::file://${project.build.directory}/deploy}, which under
 * the regression deploys to the filesystem root ({@code file:///deploy}).
 */
class MavenITgh12507CliParamNestedInterpolationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a system-property expression inside a CLI-supplied parameter value is resolved.
     */
    @Test
    void testNestedSystemPropertyExpression() throws Exception {
        Path basedir = extractResources("gh-12507-cli-param-nested-interpolation");

        Verifier verifier = newVerifier(basedir);
        verifier.addCliArgument("-Dconfig.stringParam=PRE-${user.dir}-POST");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        assertEquals("PRE-" + basedir + "-POST", props.getProperty("stringParam"));
    }

    /**
     * Verify that a project expression inside a CLI-supplied parameter value is resolved.
     */
    @Test
    void testNestedProjectExpression() throws Exception {
        Path basedir = extractResources("gh-12507-cli-param-nested-interpolation");

        Verifier verifier = newVerifier(basedir);
        verifier.addCliArgument("-Dconfig.stringParam=PRE-${project.build.directory}-POST");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        assertEquals(
                "PRE-" + basedir.resolve("target") + "-POST",
                props.getProperty("stringParam"));
    }
}
