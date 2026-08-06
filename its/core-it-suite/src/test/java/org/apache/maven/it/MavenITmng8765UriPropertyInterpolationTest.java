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
 * This is a test set for
 * <a href="https://issues.apache.org/jira/browse/MNG-8765">MNG-8765</a>.
 *
 * <p>Verifies that property interpolation runs before type conversion for
 * URI-typed plugin parameters. When a URI parameter contains a property
 * reference like {@code https://example.com/${my.version}/path}, the property
 * must be resolved before the string is converted to {@link java.net.URI}.
 * Otherwise, the curly braces cause a {@link java.net.URISyntaxException}.</p>
 *
 * <p>The regression was found in CloudStack (gnodet/maven4-testing#34733) where
 * a URI parameter with {@code ${cs.version}} defined by a Groovy script at
 * runtime was not interpolated before URI conversion.</p>
 */
public class MavenITmng8765UriPropertyInterpolationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that property interpolation resolves ${...} in URI-typed plugin
     * parameters before type conversion, including when properties are
     * inherited from a parent POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPomProperty() throws Exception {
        Path testDir = extractResources("mng-8765-uri-property-interpolation");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("child/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Check parent module: property defined in same POM
        Properties parentProps = verifier.loadProperties("target/plugin-config.properties");
        assertEquals("https://example.com/1.2.3/path", parentProps.getProperty("uriParam"));
        assertEquals("https://example.com/1.2.3/path", parentProps.getProperty("urlParam"));
        assertEquals("1.2.3", parentProps.getProperty("stringParam"));

        // Check child module: property inherited from parent POM
        Properties childProps = verifier.loadProperties("child/target/plugin-config.properties");
        assertEquals("https://example.com/1.2.3/path", childProps.getProperty("uriParam"));
        assertEquals("https://example.com/1.2.3/path", childProps.getProperty("urlParam"));
        assertEquals("1.2.3", childProps.getProperty("stringParam"));
    }

    /**
     * Verify that a property passed via -D on the command line is resolved in
     * URI-typed plugin parameters. This simulates the CloudStack scenario where
     * a Groovy script sets a property at runtime via
     * {@code project.properties.setProperty(...)}.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCliProperty() throws Exception {
        Path testDir = extractResources("mng-8765-uri-property-interpolation/cli-property");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dcli.version=2.0.0");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/plugin-config.properties");
        assertEquals("https://example.com/2.0.0/path", props.getProperty("uriParam"));
        assertEquals("https://example.com/2.0.0/path", props.getProperty("urlParam"));
        assertEquals("2.0.0", props.getProperty("stringParam"));
    }
}
