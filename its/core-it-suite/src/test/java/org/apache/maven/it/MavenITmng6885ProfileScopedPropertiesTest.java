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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6885">MNG-6885</a>.
 *
 * Verifies profile-scoped property resolution (${profile.&lt;id&gt;.&lt;property&gt;}) during model building and interpolation.
 */
class MavenITmng6885ProfileScopedPropertiesTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testitProfileScopedProperties() throws Exception {
        Path testDir = extractResources("mng-6885");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Pdev,prod");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");

        // Verify unscoped property reflects dominant (last active profile) value
        assertEquals("443", props.getProperty("project.properties.server.port"));
        assertEquals("443", props.getProperty("project.properties.unscopedPort"));

        // Verify profile-scoped properties for both active profiles are accessible without collision
        assertEquals("8080", props.getProperty("project.properties.profile.dev.server.port"));
        assertEquals("443", props.getProperty("project.properties.profile.prod.server.port"));
        assertEquals("jdbc:h2:mem", props.getProperty("project.properties.profile.dev.db.url"));
        assertEquals("jdbc:mysql://prod", props.getProperty("project.properties.profile.prod.db.url"));

        // Verify interpolated properties in the POM referencing profile-scoped properties
        assertEquals("8080", props.getProperty("project.properties.devPort"));
        assertEquals("443", props.getProperty("project.properties.prodPort"));

        // Verify inactive profile properties were not injected
        assertNull(props.getProperty("project.properties.profile.inactive.server.port"));
    }
}
