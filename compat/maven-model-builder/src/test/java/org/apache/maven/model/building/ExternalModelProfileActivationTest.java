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
 * property activators are not evaluated, and their profiles contribute no repositories. A project
 * build, at {@link ModelBuildingRequest#VALIDATION_LEVEL_STRICT}, still evaluates every activator.
 * Platform-derived activation (JDK version, operating system, activeByDefault) is unaffected at
 * either level.
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
    void testDependencyPomActivatesOnlyEnvironmentIndependentProfiles() throws Exception {
        Model model = build(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        assertNull(model.getProperties().get("profile.file"));
        assertNull(model.getProperties().get("profile.property"));
        assertEquals("activated", model.getProperties().get("profile.jdk"));
        assertTrue(model.getRepositories().stream().noneMatch(r -> "profile-repo".equals(r.getId())));
    }
}
