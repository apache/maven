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
package org.apache.maven.model.building;

import java.util.Properties;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Models built at {@link ModelBuildingRequest#VALIDATION_LEVEL_MINIMAL} come from POMs resolved
 * from a repository during dependency resolution (a dependency POM, one of its parents, or an
 * imported BOM), see for instance {@code DefaultArtifactDescriptorReader#loadPom}. Their file and
 * property activators are not evaluated. Platform-derived activation (JDK version, operating
 * system, activeByDefault) is unaffected at either level. Repositories declared in
 * legitimately-active profiles (JDK/OS/activeByDefault) are honored — stripping them would break
 * the established {@code project → dep1 → dep2} pattern where dep1 declares dep2's non-Central
 * repository inside a JDK- or activeByDefault-activated profile. A project build, at
 * {@link ModelBuildingRequest#VALIDATION_LEVEL_STRICT}, still evaluates every activator.
 */
class ExternalModelProfileActivationTest {

    private static final String POM = "<project>\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>thegroup</groupId>\n"
            + "  <artifactId>withprofiles</artifactId>\n"
            + "  <version>1</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <profiles>\n"
            + "    <profile>\n"
            + "      <id>file-condition</id>\n"
            + "      <activation>\n"
            + "        <file>\n"
            + "          <exists>${some.dir}</exists>\n"
            + "        </file>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.file>activated</profile.file>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            + "    <profile>\n"
            + "      <id>property-condition</id>\n"
            + "      <activation>\n"
            + "        <property>\n"
            + "          <name>some.gating.property</name>\n"
            + "        </property>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.property>activated</profile.property>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            + "    <profile>\n"
            + "      <id>jdk-condition</id>\n"
            + "      <activation>\n"
            + "        <jdk>[1,)</jdk>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.jdk>activated</profile.jdk>\n"
            + "      </properties>\n"
            + "      <repositories>\n"
            + "        <repository>\n"
            + "          <id>profile-repo</id>\n"
            + "          <url>https://repo.example.test/profile</url>\n"
            + "        </repository>\n"
            + "      </repositories>\n"
            + "    </profile>\n"
            + "  </profiles>\n"
            + "</project>\n";

    private Model build(int validationLevel) throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        Properties systemProperties = new Properties();
        systemProperties.putAll(System.getProperties());
        systemProperties.setProperty("some.dir", System.getProperty("java.io.tmpdir"));
        systemProperties.setProperty("some.gating.property", "true");

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(POM));
        request.setValidationLevel(validationLevel);
        request.setSystemProperties(systemProperties);

        return builder.build(request).getEffectiveModel();
    }

    @Test
    void testProjectBuildEvaluatesAllActivators() throws Exception {
        Model model = build(ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertEquals("activated", model.getProperties().get("profile.file"));
        assertEquals("activated", model.getProperties().get("profile.property"));
        assertEquals("activated", model.getProperties().get("profile.jdk"));
        assertTrue(model.getRepositories().stream().anyMatch(r -> "profile-repo".equals(r.getId())));
    }

    @Test
    void testDependencyPomHonorsJdkActivatedProfileRepositories() throws Exception {
        Model model = build(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        assertNull(model.getProperties().get("profile.file"));
        assertNull(model.getProperties().get("profile.property"));
        assertEquals("activated", model.getProperties().get("profile.jdk"));
        // Repositories from legitimately-active profiles (JDK-activated) must be honored:
        // stripping them would break the project → dep1 → dep2 pattern. See #13100, #13141.
        assertTrue(model.getRepositories().stream().anyMatch(r -> "profile-repo".equals(r.getId())));
    }

    /**
     * An {@code activeByDefault=true} profile in an external dependency model must contribute
     * its repositories — the same guarantee as for JDK/OS-activated profiles (see #13100).
     * Uses an isolated POM with only an activeByDefault profile so the standard Maven rule
     * ("activeByDefault is suppressed when any other profile activates") does not interfere.
     */
    @Test
    void testActiveByDefaultProfileRepositoryHonored() throws Exception {
        // Isolated POM: only one profile (activeByDefault=true), no other profiles that could
        // suppress it.  Without isolation the JDK-activated sibling in POM would cause Maven
        // to skip the activeByDefault profile entirely, making the test vacuous.
        String activeByDefaultPom = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>thegroup</groupId>\n"
                + "  <artifactId>active-by-default-profile</artifactId>\n"
                + "  <version>1</version>\n"
                + "  <packaging>pom</packaging>\n"
                + "  <profiles>\n"
                + "    <profile>\n"
                + "      <id>always-active</id>\n"
                + "      <activation>\n"
                + "        <activeByDefault>true</activeByDefault>\n"
                + "      </activation>\n"
                + "      <repositories>\n"
                + "        <repository>\n"
                + "          <id>always-active-repo</id>\n"
                + "          <url>https://repo.example.test/always-active</url>\n"
                + "        </repository>\n"
                + "      </repositories>\n"
                + "    </profile>\n"
                + "  </profiles>\n"
                + "</project>\n";

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        Properties systemProperties = new Properties();
        systemProperties.putAll(System.getProperties());

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(activeByDefaultPom));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setSystemProperties(systemProperties);

        Model model = builder.build(request).getEffectiveModel();

        // The always-active profile activates by default: its repository must survive external
        // model resolution unchanged. See #13100, #13141.
        assertTrue(
                model.getRepositories().stream().anyMatch(r -> "always-active-repo".equals(r.getId())),
                "Repository from activeByDefault profile must be retained in external dependency model");
    }
}
